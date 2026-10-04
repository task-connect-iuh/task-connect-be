package vn.taskconnect.chat.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.taskconnect.admin.api.AdminFacade;
import vn.taskconnect.booking.api.BookingFacade;
import vn.taskconnect.booking.api.dto.BookingSummary;
import vn.taskconnect.chat.api.ChannelStatus;
import vn.taskconnect.chat.api.ChatMessageType;
import vn.taskconnect.chat.api.ChatSystemMessages;
import vn.taskconnect.chat.api.InboxTab;
import vn.taskconnect.chat.api.ProposalStatus;
import vn.taskconnect.chat.dto.request.ChatAttachmentUploadUrlRequest;
import vn.taskconnect.chat.dto.request.SendAttachmentMessageRequest;
import vn.taskconnect.chat.dto.response.ChatApplicationStatusResponse;
import vn.taskconnect.chat.dto.response.ChatAttachmentResponse;
import vn.taskconnect.chat.dto.response.ChatAttachmentUploadUrlResponse;
import vn.taskconnect.chat.dto.response.ChatInboxItemResponse;
import vn.taskconnect.chat.dto.response.ChatMessageResponse;
import vn.taskconnect.chat.dto.response.ChatReactionResponse;
import vn.taskconnect.chat.dto.response.InboxPingEvent;
import vn.taskconnect.chat.dto.response.TaskEditDiffResponse;
import vn.taskconnect.chat.entity.ChatChannel;
import vn.taskconnect.chat.entity.ChatMessage;
import vn.taskconnect.chat.entity.ChatMessageAttachment;
import vn.taskconnect.chat.entity.ChatMessageReaction;
import vn.taskconnect.chat.entity.ChatPinnedMessage;
import vn.taskconnect.chat.repository.ChatChannelRepository;
import vn.taskconnect.chat.repository.ChatMessageAttachmentRepository;
import vn.taskconnect.chat.repository.ChatMessageReactionRepository;
import vn.taskconnect.chat.repository.ChatMessageRepository;
import vn.taskconnect.chat.repository.ChatPinnedMessageRepository;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.common.storage.S3PresignedUploadService;
import vn.taskconnect.task.api.TaskApplicationStatus;
import vn.taskconnect.task.api.TaskFacade;
import vn.taskconnect.task.api.TaskStatus;
import vn.taskconnect.task.api.dto.ExtraCostBatchSummary;
import vn.taskconnect.task.api.dto.PriceProposalCreated;
import vn.taskconnect.task.api.dto.TaskApplicationParties;
import vn.taskconnect.task.api.dto.TaskEditSummary;

/**
 * Nghiep vu chinh cua module Chat (UC16): lazy-create kenh, gui/doc tin nhan, thuong luong
 * gia, Inbox, dong kenh. Dung truc tiep boi ChatController (cung module) va uy quyen tu
 * ChatFacadeImpl (cac method module khac can, xem ChatFacade). TaskFacade la phu thuoc duy
 * nhat ra ngoai module (Chat -> Task, xem 10-module-boundary.md) - dung de kiem tra quyen,
 * lay ten hien thi/tieu de viec, va thuc hien phan Task cua thuong luong gia
 * (task_price_history thuoc so huu module Task).
 */
@Service
public class ChatService {

    /** presigned GET cho anh/video/file dinh kem - ngan han, ky lai moi lan tra ve (xem toAttachmentResponse). */
    private static final Duration ATTACHMENT_GET_URL_TTL = Duration.ofMinutes(15);

    /** Cac loai tin nhan co the thu hoi - KHONG bao gom SYSTEM (khong co nguoi gui) va PRICE_PROPOSAL/
     * RESCHEDULE_PROPOSAL (da co co che Thu hoi rieng gan voi proposalStatus, xem withdraw*()). */
    private static final Set<ChatMessageType> RECALLABLE_MESSAGE_TYPES = EnumSet.of(
            ChatMessageType.TEXT, ChatMessageType.IMAGE, ChatMessageType.FILE, ChatMessageType.VIDEO,
            ChatMessageType.VOICE);

    private final ChatChannelRepository channelRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMessageAttachmentRepository attachmentRepository;
    private final ChatMessageReactionRepository reactionRepository;
    private final ChatPinnedMessageRepository pinnedMessageRepository;
    private final ChatAttachmentUploadService attachmentUploadService;
    private final S3PresignedUploadService s3Service;
    private final AdminFacade adminFacade;
    private final TaskFacade taskFacade;
    private final BookingFacade bookingFacade;
    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    public ChatService(ChatChannelRepository channelRepository, ChatMessageRepository messageRepository,
            ChatMessageAttachmentRepository attachmentRepository, ChatMessageReactionRepository reactionRepository,
            ChatPinnedMessageRepository pinnedMessageRepository, ChatAttachmentUploadService attachmentUploadService,
            S3PresignedUploadService s3Service, AdminFacade adminFacade, TaskFacade taskFacade,
            BookingFacade bookingFacade, SimpMessagingTemplate messagingTemplate, Clock clock) {
        this.channelRepository = channelRepository;
        this.messageRepository = messageRepository;
        this.attachmentRepository = attachmentRepository;
        this.reactionRepository = reactionRepository;
        this.pinnedMessageRepository = pinnedMessageRepository;
        this.attachmentUploadService = attachmentUploadService;
        this.s3Service = s3Service;
        this.adminFacade = adminFacade;
        this.taskFacade = taskFacade;
        this.bookingFacade = bookingFacade;
        this.messagingTemplate = messagingTemplate;
        this.clock = clock;
    }

    /**
     * Gui 1 tin nhan TEXT. Lazy-create kenh + SYSTEM message mo dau (backdate ve
     * application.createdAt) neu day la lan gui dau tien cho application nay (dac ta muc 2).
     * 403 neu senderAccountId khong phai Poster/Tasker cua application, 409 neu kenh da CLOSED.
     * replyToMessageId tuy chon (them 2026-09-26) - tra loi (quote) 1 tin nhan khac cung kenh,
     * xem validateReplyTarget().
     */
    @Transactional
    public ChatMessageResponse sendTextMessage(UUID applicationId, UUID senderAccountId, String text,
            UUID replyToMessageId) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, senderAccountId);
        UUID validReplyTo = validateReplyTarget(applicationId, replyToMessageId);
        taskFacade.clearInviteExpiryIfInvited(applicationId, senderAccountId);
        ChatChannel channel = requireOpenChannelLazyCreate(parties);
        ChatMessage message = messageRepository.save(ChatMessage.text(UUID.randomUUID(), channel.getId(),
                senderAccountId, text, validReplyTo, clock.instant()));
        ChatMessageResponse response = toResponse(message, parties);
        publish(parties, response);
        return response;
    }

    /**
     * Xin 1 presigned PUT URL de tu tai 1 anh/video/file len S3 truoc khi gui tin nhan dinh kem
     * (them 2026-09-26) - xem ChatAttachmentUploadService. Khong doi hoi kenh da ton tai (giong
     * cac thao tac gui khac, kenh chi lazy-create luc THAT SU gui tin).
     */
    @Transactional(readOnly = true)
    public ChatAttachmentUploadUrlResponse createAttachmentUploadUrl(UUID applicationId, UUID requesterAccountId,
            ChatAttachmentUploadUrlRequest request) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, requesterAccountId);
        return attachmentUploadService.createUploadUrl(applicationId, request);
    }

    /**
     * Tao 1 tin nhan IMAGE/FILE/VIDEO SAU KHI client da PUT xong tung file len S3 (them
     * 2026-09-26). Kiem tra so luong/dung luong khai bao so voi nguong doc tu AdminFacade
     * (khong xac minh lai noi dung that voi S3, cung han che voi ADR-003/004), lazy-create kenh
     * neu can, luu 1 dong chat_messages + N dong chat_message_attachments (dung thu tu client gui).
     */
    @Transactional
    public ChatMessageResponse sendAttachmentMessage(UUID applicationId, UUID senderAccountId,
            SendAttachmentMessageRequest request) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, senderAccountId);
        ChatMessageType type = request.messageType();
        if (type != ChatMessageType.IMAGE && type != ChatMessageType.FILE && type != ChatMessageType.VIDEO
                && type != ChatMessageType.VOICE) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_ATTACHMENT_TYPE);
        }
        validateAttachmentLimits(type, request.attachments());
        UUID validReplyTo = validateReplyTarget(applicationId, request.replyToMessageId());
        taskFacade.clearInviteExpiryIfInvited(applicationId, senderAccountId);
        ChatChannel channel = requireOpenChannelLazyCreate(parties);
        Instant now = clock.instant();
        UUID messageId = UUID.randomUUID();
        ChatMessage message = messageRepository.save(ChatMessage.attachmentMessage(messageId, channel.getId(),
                senderAccountId, type, request.caption(), validReplyTo, now));
        int order = 0;
        for (SendAttachmentMessageRequest.AttachmentInput input : request.attachments()) {
            attachmentRepository.save(ChatMessageAttachment.of(UUID.randomUUID(), messageId, input.objectKey(),
                    input.fileName(), input.mimeType(), input.fileSizeBytes(), order++, now));
        }
        ChatMessageResponse response = toResponse(message, parties);
        publish(parties, response);
        return response;
    }

    /**
     * Chinh nguoi gui Thu hoi 1 tin nhan TEXT/IMAGE/FILE/VIDEO cua chinh minh, trong han thoi
     * gian doc tu AdminFacade (mac dinh 15 phut) - them 2026-09-26. Thu hoi la MEM (khong xoa
     * du lieu that, xem Javadoc ChatMessage.markRecalled) va AP DUNG VOI CA HAI BEN (ca nguoi
     * gui lan nguoi nhan deu thay dong thay the sau khi thu hoi - rule da chot voi nguoi dung).
     * Neu tin dang duoc ghim, tu dong bo ghim (khong con noi dung de ghim).
     */
    @Transactional
    public ChatMessageResponse recallMessage(UUID applicationId, UUID messageId, UUID actingAccountId) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, actingAccountId);
        ChatMessage message = requireMessageInChannel(applicationId, messageId);
        if (!RECALLABLE_MESSAGE_TYPES.contains(message.getMessageType())) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_RECALLABLE);
        }
        if (message.isRecalled()) {
            throw new BusinessException(ErrorCode.MESSAGE_RECALLED);
        }
        if (!actingAccountId.equals(message.getSenderAccountId())) {
            throw new BusinessException(ErrorCode.NOT_MESSAGE_SENDER);
        }
        Duration recallWindow = Duration.ofMinutes(adminFacade.getChatMessageRecallWindowMinutes());
        if (Duration.between(message.getCreatedAt(), clock.instant()).compareTo(recallWindow) > 0) {
            throw new BusinessException(ErrorCode.RECALL_WINDOW_EXPIRED);
        }
        message.markRecalled(clock.instant());
        pinnedMessageRepository.deleteByMessageId(messageId);
        ChatMessageResponse response = toResponse(message, parties);
        publish(parties, response);
        return response;
    }

    /**
     * Tha/doi/bo mot emoji tren 1 tin nhan (them 2026-09-26) - toggle: chua tha thi tha moi, da
     * tha emoji KHAC thi doi, da tha CUNG emoji thi bo tha. Toi da 1 reaction/tai khoan/tin nhan
     * (UNIQUE trong V41).
     */
    @Transactional
    public ChatMessageResponse reactToMessage(UUID applicationId, UUID messageId, UUID actingAccountId, String emoji) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, actingAccountId);
        ChatMessage message = requireMessageInChannel(applicationId, messageId);
        if (message.isRecalled()) {
            throw new BusinessException(ErrorCode.MESSAGE_RECALLED);
        }
        Instant now = clock.instant();
        reactionRepository.findByMessageIdAndAccountId(messageId, actingAccountId).ifPresentOrElse(existing -> {
            if (existing.getEmoji().equals(emoji)) {
                reactionRepository.delete(existing);
            } else {
                existing.changeEmoji(emoji, now);
            }
        }, () -> reactionRepository.save(ChatMessageReaction.of(UUID.randomUUID(), messageId, actingAccountId, emoji, now)));
        ChatMessageResponse response = toResponse(message, parties);
        publish(parties, response);
        return response;
    }

    /**
     * Ghim 1 tin nhan vao kenh cua application (them 2026-09-26) - CA HAI ben (Poster/Tasker)
     * deu ghim duoc, toi da N tin dong thoi/kenh (N doc tu AdminFacade, mac dinh 3). Idempotent
     * neu tin da duoc ghim tu truoc (khong bao loi). Khoa PESSIMISTIC_WRITE tren ChatChannel de
     * tranh 2 ben cung ghim vuot qua gioi han khi dang o dung nguong (xem
     * ChatChannelRepository.findByIdForUpdate).
     */
    @Transactional
    public ChatMessageResponse pinMessage(UUID applicationId, UUID messageId, UUID actingAccountId) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, actingAccountId);
        ChatMessage message = requireMessageInChannel(applicationId, messageId);
        if (message.isRecalled()) {
            throw new BusinessException(ErrorCode.MESSAGE_RECALLED);
        }
        channelRepository.findByIdForUpdate(message.getChannelId());
        boolean alreadyPinned = pinnedMessageRepository.findByChannelIdOrderByCreatedAtAsc(message.getChannelId())
                .stream().anyMatch(pinned -> pinned.getMessageId().equals(messageId));
        if (!alreadyPinned) {
            int limit = adminFacade.getChatMaxPinnedMessagesPerChannel();
            if (pinnedMessageRepository.countByChannelId(message.getChannelId()) >= limit) {
                throw new BusinessException(ErrorCode.PIN_LIMIT_REACHED);
            }
            pinnedMessageRepository.save(ChatPinnedMessage.of(UUID.randomUUID(), message.getChannelId(), messageId,
                    actingAccountId, clock.instant()));
        }
        ChatMessageResponse response = toResponse(message, parties);
        publish(parties, response);
        return response;
    }

    /** Bo ghim 1 tin nhan (them 2026-09-26) - CA HAI ben ghim duoc thi CA HAI ben cung bo ghim duoc. No-op neu chua tung ghim. */
    @Transactional
    public ChatMessageResponse unpinMessage(UUID applicationId, UUID messageId, UUID actingAccountId) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, actingAccountId);
        ChatMessage message = requireMessageInChannel(applicationId, messageId);
        pinnedMessageRepository.deleteByChannelIdAndMessageId(message.getChannelId(), messageId);
        ChatMessageResponse response = toResponse(message, parties);
        publish(parties, response);
        return response;
    }

    /** Danh sach tin nhan cua 1 kenh, rong neu chua ai gui gi (kenh chua ton tai). 403 neu requester khong phai Poster/Tasker. */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listMessages(UUID applicationId, UUID requesterAccountId) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, requesterAccountId);
        return channelRepository.findByApplicationId(applicationId)
                .map(channel -> listMessagesOfChannel(channel, parties))
                .orElse(List.of());
    }

    /**
     * Chi tiet "truoc -> sua" cua 1 lan Poster sua cong viec (UC07), tra cuu tu 1 SYSTEM message
     * co ref_task_edit_id - dung khi FE bam nut "Xem chi tiết thay đổi". Quyen xem kiem tra bang
     * requireParty() nhu moi endpoint chat khac; du lieu that doc qua TaskFacade (cam JOIN
     * xuyen module). 404 neu messageId khong thuoc kenh nay, hoac khong phai SYSTEM message sinh
     * tu UC07 (refTaskEditId null), hoac su kien sua khong con ton tai.
     */
    @Transactional(readOnly = true)
    public TaskEditDiffResponse getTaskEditOfMessage(UUID applicationId, UUID messageId, UUID actingAccountId) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, actingAccountId);
        ChatMessage message = requireMessageInChannel(applicationId, messageId);
        UUID refTaskEditId = message.getRefTaskEditId();
        if (refTaskEditId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        TaskEditSummary summary = taskFacade.findTaskEdit(refTaskEditId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        List<TaskEditDiffResponse.Change> changes = summary.changes().stream()
                .map(change -> new TaskEditDiffResponse.Change(change.field(), change.oldValue(), change.newValue()))
                .toList();
        return new TaskEditDiffResponse(summary.id(), summary.editedAt(), changes);
    }

    /**
     * Nap toan bo tin nhan cua 1 kenh kem attachments/reactions/pin - nap gop theo tap messageId
     * (3 truy van bulk) thay vi tung dong 1 truy van (tranh N+1 khi 1 kenh co nhieu tin, them
     * 2026-09-26).
     */
    private List<ChatMessageResponse> listMessagesOfChannel(ChatChannel channel, TaskApplicationParties parties) {
        List<ChatMessage> messages = messageRepository.findByChannelIdOrderByCreatedAtAsc(channel.getId());
        if (messages.isEmpty()) {
            return List.of();
        }
        List<UUID> messageIds = messages.stream().map(ChatMessage::getId).toList();
        Map<UUID, List<ChatMessageAttachment>> attachmentsByMessage = attachmentRepository
                .findByMessageIdInOrderBySortOrderAsc(messageIds).stream()
                .collect(Collectors.groupingBy(ChatMessageAttachment::getMessageId));
        Map<UUID, List<ChatMessageReaction>> reactionsByMessage = reactionRepository.findByMessageIdIn(messageIds)
                .stream().collect(Collectors.groupingBy(ChatMessageReaction::getMessageId));
        Map<UUID, ChatPinnedMessage> pinnedByMessage = pinnedMessageRepository
                .findByChannelIdOrderByCreatedAtAsc(channel.getId()).stream()
                .collect(Collectors.toMap(ChatPinnedMessage::getMessageId, pinned -> pinned));
        return messages.stream()
                .map(message -> assemble(message, parties, amountOf(message),
                        attachmentsByMessage.getOrDefault(message.getId(), List.of()),
                        reactionsByMessage.getOrDefault(message.getId(), List.of()),
                        pinnedByMessage.get(message.getId())))
                .toList();
    }

    /** Danh sach channel (Inbox) cua 1 tai khoan, loc theo tab, moi nhat truoc (dac ta muc 10). */
    @Transactional(readOnly = true)
    public List<ChatInboxItemResponse> listInbox(UUID accountId, InboxTab tab) {
        List<UUID> applicationIds = taskFacade.listApplicationIdsForAccount(accountId);
        if (applicationIds.isEmpty()) {
            return List.of();
        }
        List<ChatChannel> channels = channelRepository.findByApplicationIdIn(applicationIds);
        List<ChatInboxItemResponse> items = new ArrayList<>();
        for (ChatChannel channel : channels) {
            taskFacade.getApplicationParties(channel.getApplicationId()).ifPresent(parties -> {
                ChatMessage lastMessage = messageRepository
                        .findTopByChannelIdOrderByCreatedAtDesc(channel.getId()).orElse(null);
                boolean needsResponse = computeNeedsResponse(channel, lastMessage, accountId);
                if (matchesTab(tab, channel.getStatus(), needsResponse, channel.getBookingId() != null)) {
                    items.add(toInboxItem(channel, parties, lastMessage, accountId, needsResponse));
                }
            });
        }
        items.sort(Comparator.comparing(ChatInboxItemResponse::lastMessageAt).reversed());
        return items;
    }

    /**
     * Tao kenh + SYSTEM message mo dau + tin nhan TEXT dau tien (cau hoi cua Tasker) - dung
     * cho "Nhan tin hoi them" (goi tu Task, cung transaction voi tao application INQUIRING).
     */
    @Transactional
    public void openChannelWithFirstInquiryMessage(UUID applicationId, UUID taskerAccountId,
            String systemMessageBody, String inquiryMessageText, Instant createdAt) {
        if (channelRepository.findByApplicationId(applicationId).isPresent()) {
            return;
        }
        ChatChannel channel = channelRepository.save(ChatChannel.open(UUID.randomUUID(), applicationId, createdAt));
        messageRepository.save(ChatMessage.system(UUID.randomUUID(), channel.getId(), systemMessageBody, createdAt));
        ChatMessage inquiryMessage = messageRepository.save(ChatMessage.text(UUID.randomUUID(), channel.getId(),
                taskerAccountId, inquiryMessageText, createdAt.plusMillis(1)));
        taskFacade.getApplicationParties(applicationId)
                .ifPresent(parties -> publish(parties, toResponse(inquiryMessage, parties)));
    }

    /**
     * Dong kenh cua 1 application (neu co va dang OPEN) kem 1 SYSTEM message - dung cho
     * withdraw/decline/reject/reject-auto/invite-expired. No-op neu chua co kenh hoac da CLOSED.
     */
    @Transactional
    public void closeChannelIfExists(UUID applicationId, String systemMessageBody) {
        channelRepository.findByApplicationId(applicationId).ifPresent(channel -> {
            if (channel.getStatus() != ChannelStatus.OPEN) {
                return;
            }
            channel.close();
            ChatMessage closingMessage = messageRepository
                    .save(ChatMessage.system(UUID.randomUUID(), channel.getId(), systemMessageBody, clock.instant()));
            taskFacade.getApplicationParties(applicationId)
                    .ifPresent(parties -> publish(parties, toResponse(closingMessage, parties)));
        });
    }

    /**
     * Gui 1 SYSTEM message vao kenh cua application (neu co va dang OPEN), KHONG dong kenh -
     * dung khi Tasker nang cap thang 1 don INQUIRING len PENDING. No-op neu chua co kenh hoac
     * da CLOSED - khac closeChannelIfExists() o cho khong goi channel.close().
     */
    @Transactional
    public void postSystemMessageIfOpen(UUID applicationId, String systemMessageBody) {
        channelRepository.findByApplicationId(applicationId).ifPresent(channel -> {
            if (channel.getStatus() != ChannelStatus.OPEN) {
                return;
            }
            ChatMessage message = messageRepository
                    .save(ChatMessage.system(UUID.randomUUID(), channel.getId(), systemMessageBody, clock.instant()));
            taskFacade.getApplicationParties(applicationId)
                    .ifPresent(parties -> publish(parties, toResponse(message, parties)));
        });
    }

    /**
     * Nhu postSystemMessageIfOpen() nhung dinh kem id cua 1 su kien Poster sua cong viec (UC07)
     * vao chat_messages.ref_task_edit_id, de FE hien nut "Xem chi tiết thay đổi" tren dung tin
     * nay. No-op neu chua co kenh hoac kenh da CLOSED.
     */
    @Transactional
    public void postTaskEditSystemMessageIfOpen(UUID applicationId, String systemMessageBody, UUID taskEditEventId) {
        channelRepository.findByApplicationId(applicationId).ifPresent(channel -> {
            if (channel.getStatus() != ChannelStatus.OPEN) {
                return;
            }
            ChatMessage message = messageRepository.save(ChatMessage.taskEditSystem(UUID.randomUUID(),
                    channel.getId(), systemMessageBody, taskEditEventId, clock.instant()));
            taskFacade.getApplicationParties(applicationId)
                    .ifPresent(parties -> publish(parties, toResponse(message, parties)));
        });
    }

    /**
     * Gui 1 tin nhan EXTRA_COST_BATCH vao kenh cua application (neu co va dang OPEN) ngay sau khi
     * Tasker dang xong 1 batch "Chi phi phat sinh" (them 2026-10-03, thay the cho 1 dong SYSTEM
     * text truoc day - yeu cau nguoi dung: hien nhu 1 the rieng trong khung chat, giong
     * PRICE_PROPOSAL). No-op neu chua co kenh hoac kenh da CLOSED (khong nen xay ra - batch chi
     * dang duoc khi task dang ASSIGNED, kenh chac chan da mo tu luc UC11 xac nhan ung vien).
     */
    @Transactional
    public void postExtraCostBatchMessageIfOpen(UUID applicationId, UUID taskerAccountId, UUID extraCostBatchId) {
        channelRepository.findByApplicationId(applicationId).ifPresent(channel -> {
            if (channel.getStatus() != ChannelStatus.OPEN) {
                return;
            }
            ChatMessage message = messageRepository.save(ChatMessage.extraCostBatch(UUID.randomUUID(),
                    channel.getId(), taskerAccountId, extraCostBatchId, clock.instant()));
            taskFacade.getApplicationParties(applicationId)
                    .ifPresent(parties -> publish(parties, toResponse(message, parties)));
        });
    }

    /**
     * Co ton tai 1 message dung loai dang PROPOSED trong kenh cua application nay khong -
     * dung boi ChatFacade.hasPendingProposal() (Task goi vao de kiem tra truoc khi tao de xuat
     * moi/xac nhan UC11). Chua co kenh nghia la chac chan chua co de xuat nao.
     */
    @Transactional(readOnly = true)
    public boolean hasPendingProposal(UUID applicationId, ChatMessageType type) {
        return channelRepository.findByApplicationId(applicationId)
                .map(channel -> messageRepository.existsByChannelIdAndMessageTypeAndProposalStatus(channel.getId(),
                        type, ProposalStatus.PROPOSED))
                .orElse(false);
    }

    /**
     * Xem Javadoc ChatFacade.findProposalStatusesByPriceHistoryIds() - doc thang tu
     * chat_messages (khong can biet kenh nao), moi dong task_price_history luon di kem dung 1
     * tin nhan PRICE_PROPOSAL nen anh xa 1-1 an toan.
     */
    @Transactional(readOnly = true)
    public Map<UUID, ProposalStatus> findProposalStatusesByPriceHistoryIds(Set<UUID> priceHistoryIds) {
        if (priceHistoryIds.isEmpty()) {
            return Map.of();
        }
        return messageRepository.findByRefPriceHistoryIdIn(priceHistoryIds).stream()
                .collect(Collectors.toMap(ChatMessage::getRefPriceHistoryId, ChatMessage::getProposalStatus));
    }

    /**
     * Tao 1 de xuat gia moi trong kenh cua application - lazy-create kenh neu day la lan gui
     * dau tien (de xuat gia co the la noi dung dau tien, vd Poster de nghi gia ngay khi vao
     * khung chat cua 1 don PENDING chua ai nhan tin gi). Goi TaskFacade.proposePrice de tao
     * task_price_history + xac dinh change_type (409 neu dang co 1 de xuat khac chua xu ly -
     * kiem tra do chinh Task thuc hien qua hasPendingProposal()), roi tao chat_messages
     * PRICE_PROPOSAL trong CUNG kenh, tham chieu dung dong vua tao (dac ta muc 3).
     */
    @Transactional
    public ChatMessageResponse createPriceProposal(UUID applicationId, UUID proposerAccountId, long amount,
            String note) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, proposerAccountId);
        taskFacade.clearInviteExpiryIfInvited(applicationId, proposerAccountId);
        ChatChannel channel = requireOpenChannelLazyCreate(parties);
        if (hasPendingProposal(applicationId, ChatMessageType.PRICE_PROPOSAL)) {
            throw new BusinessException(ErrorCode.PROPOSAL_PENDING);
        }
        PriceProposalCreated created = taskFacade.proposePrice(applicationId, proposerAccountId, amount, note);
        ChatMessage message = messageRepository.save(ChatMessage.priceProposal(UUID.randomUUID(), channel.getId(),
                proposerAccountId, note, created.priceHistoryId(), clock.instant()));
        ChatMessageResponse response = toResponse(message, parties, amount);
        publish(parties, response);
        return response;
    }

    /**
     * Ben KHONG PHAI nguoi tao de xuat moi duoc Dong y. Goi TaskFacade de ghi nhan hieu ung
     * (accepted_at, nang INQUIRING len PENDING, cap nhat application.proposed_price), roi cap
     * nhat proposalStatus tren chinh message do va sinh 1 SYSTEM message.
     */
    @Transactional
    public ChatMessageResponse acceptPriceProposal(UUID applicationId, UUID messageId, UUID accepterAccountId) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, accepterAccountId);
        taskFacade.clearInviteExpiryIfInvited(applicationId, accepterAccountId);
        ChatMessage proposal = requirePendingProposal(applicationId, messageId, ChatMessageType.PRICE_PROPOSAL);
        if (accepterAccountId.equals(proposal.getSenderAccountId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Bạn không thể tự đồng ý đề xuất giá của chính mình.");
        }
        long amount = taskFacade.acceptPriceProposal(applicationId, proposal.getRefPriceHistoryId(),
                accepterAccountId);
        proposal.markProposalAccepted();
        String accepterName = displayNameOf(parties, accepterAccountId);
        return appendProposalOutcomeSystemMessage(parties, proposal,
                ChatSystemMessages.priceProposalAccepted(accepterName, amount));
    }

    /** Chi nguoi tao (proposer) moi duoc Thu hoi de xuat con PROPOSED - SYSTEM message rieng "đã thu hồi" (dac ta muc 3). */
    @Transactional
    public ChatMessageResponse withdrawPriceProposal(UUID applicationId, UUID messageId, UUID actingAccountId) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, actingAccountId);
        ChatMessage proposal = requirePendingProposal(applicationId, messageId, ChatMessageType.PRICE_PROPOSAL);
        if (!actingAccountId.equals(proposal.getSenderAccountId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ người tạo đề xuất mới được thu hồi.");
        }
        proposal.markProposalRejected();
        return appendProposalOutcomeSystemMessage(parties, proposal,
                ChatSystemMessages.priceProposalWithdrawn(displayNameOf(parties, actingAccountId)));
    }

    /**
     * Ben con lai (khong phai proposer) Tu choi de xuat con PROPOSED - SYSTEM message rieng
     * "đã bị từ chối", PHAN BIET voi withdrawPriceProposal() (dac ta muc 3 - suy luan, xem
     * docs/PROGRESS-CHAT-MODULE.md ve ly do dac ta khong liet ke rieng nut nay).
     */
    @Transactional
    public ChatMessageResponse rejectPriceProposal(UUID applicationId, UUID messageId, UUID actingAccountId) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, actingAccountId);
        ChatMessage proposal = requirePendingProposal(applicationId, messageId, ChatMessageType.PRICE_PROPOSAL);
        if (actingAccountId.equals(proposal.getSenderAccountId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Không thể tự từ chối đề xuất của chính mình, hãy dùng Thu hồi.");
        }
        proposal.markProposalRejected();
        return appendProposalOutcomeSystemMessage(parties, proposal,
                ChatSystemMessages.priceProposalRejected(displayNameOf(parties, actingAccountId)));
    }

    /**
     * Tao 1 de xuat doi lich moi (UC16 muc 9, Round B6) - khoa "1 de xuat treo" DOC LAP voi
     * price proposal (kiem tra rieng theo ChatMessageType.RESCHEDULE_PROPOSAL, khong dung
     * chung voi PRICE_PROPOSAL - gia dinh nay ghi trong docs/PROGRESS-CHAT-MODULE.md, de doi
     * neu can khoa chung). Gate task.status==ASSIGNED do BookingFacade.proposeReschedule() tu
     * kiem tra va nem loi neu sai (Booking so huu khai niem "cua so co the doi lich").
     */
    @Transactional
    public ChatMessageResponse createRescheduleProposal(UUID applicationId, UUID proposerAccountId,
            Instant proposedTime, String note) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, proposerAccountId);
        ChatChannel channel = requireOpenChannelLazyCreate(parties);
        if (hasPendingProposal(applicationId, ChatMessageType.RESCHEDULE_PROPOSAL)) {
            throw new BusinessException(ErrorCode.PROPOSAL_PENDING);
        }
        bookingFacade.proposeReschedule(applicationId);
        ChatMessage message = messageRepository.save(ChatMessage.rescheduleProposal(UUID.randomUUID(),
                channel.getId(), proposerAccountId, note, proposedTime, clock.instant()));
        ChatMessageResponse response = toResponse(message, parties);
        publish(parties, response);
        return response;
    }

    /**
     * Ben KHONG PHAI nguoi tao de xuat moi duoc Dong y. Goi BookingFacade de ghi that
     * scheduled_at, roi cap nhat proposalStatus tren chinh message do va sinh 1 SYSTEM message.
     */
    @Transactional
    public ChatMessageResponse acceptRescheduleProposal(UUID applicationId, UUID messageId, UUID accepterAccountId) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, accepterAccountId);
        ChatMessage proposal = requirePendingProposal(applicationId, messageId, ChatMessageType.RESCHEDULE_PROPOSAL);
        if (accepterAccountId.equals(proposal.getSenderAccountId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Bạn không thể tự đồng ý đề xuất đổi lịch của chính mình.");
        }
        BookingSummary booking = bookingFacade.acceptReschedule(applicationId, proposal.getProposedTime());
        proposal.markProposalAccepted();
        String accepterName = displayNameOf(parties, accepterAccountId);
        return appendProposalOutcomeSystemMessage(parties, proposal,
                ChatSystemMessages.rescheduleProposalAccepted(accepterName, booking.scheduledAt()));
    }

    /** Chi nguoi tao (proposer) moi duoc Thu hoi de xuat doi lich con PROPOSED - cung co che voi withdrawPriceProposal(). */
    @Transactional
    public ChatMessageResponse withdrawRescheduleProposal(UUID applicationId, UUID messageId, UUID actingAccountId) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, actingAccountId);
        ChatMessage proposal = requirePendingProposal(applicationId, messageId, ChatMessageType.RESCHEDULE_PROPOSAL);
        if (!actingAccountId.equals(proposal.getSenderAccountId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ người tạo đề xuất mới được thu hồi.");
        }
        proposal.markProposalRejected();
        return appendProposalOutcomeSystemMessage(parties, proposal,
                ChatSystemMessages.rescheduleProposalWithdrawn(displayNameOf(parties, actingAccountId)));
    }

    /** Ben con lai (khong phai proposer) Tu choi de xuat doi lich con PROPOSED - cung co che voi rejectPriceProposal(). */
    @Transactional
    public ChatMessageResponse rejectRescheduleProposal(UUID applicationId, UUID messageId, UUID actingAccountId) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, actingAccountId);
        ChatMessage proposal = requirePendingProposal(applicationId, messageId, ChatMessageType.RESCHEDULE_PROPOSAL);
        if (actingAccountId.equals(proposal.getSenderAccountId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Không thể tự từ chối đề xuất của chính mình, hãy dùng Thu hồi.");
        }
        proposal.markProposalRejected();
        return appendProposalOutcomeSystemMessage(parties, proposal,
                ChatSystemMessages.rescheduleProposalRejected(displayNameOf(parties, actingAccountId)));
    }

    /**
     * Cac trang thai application coi la DA KET THUC doi voi chat - khong con gui/lazy-create
     * duoc nua, BAT KE kenh vat ly da tung ton tai hay chua. Phat hien 2026-09-21: cac
     * closeChannelIfExists() o TaskApplicationService (withdraw/reject/reject-auto/decline-
     * invite/invite-expired) chi dong duoc kenh NEU no da ton tai - 1 don PENDING chua tung co
     * tin nhan nao (kenh chua lazy-create) roi bi tu choi/rut lai thi khong co kenh nao de dong,
     * nen requireOpenChannelLazyCreate() truoc day van vo tu tao kenh moi va cho gui binh
     * thuong. Danh sach nay dung DUNG cac trang thai ma cac diem goi closeChannelIfExists() o
     * tren dang xu ly, giu nhat quan voi dinh nghia do thay vi tu suy dien tap moi.
     */
    private static final Set<TaskApplicationStatus> CHAT_TERMINAL_APPLICATION_STATUSES = EnumSet.of(
            TaskApplicationStatus.WITHDRAWN, TaskApplicationStatus.REJECTED, TaskApplicationStatus.REJECTED_AUTO,
            TaskApplicationStatus.DECLINED, TaskApplicationStatus.INVITE_EXPIRED,
            // Them cho UC07: Poster huy ca cong viec (TaskService.cancelTask) cung dong kenh
            // qua closeChannelIfExists() giong 5 trang thai tren.
            TaskApplicationStatus.CANCELLED);

    /**
     * Lazy-create kenh neu chua co (dac ta muc 2), hoac tra ve kenh da co - 409 neu da CLOSED,
     * neu application da o 1 trong CHAT_TERMINAL_APPLICATION_STATUSES du kenh vat ly chua tung
     * duoc tao (xem Javadoc hang so tren), HOAC neu task cha da REJECTED (Admin tu choi hau
     * kiem - xem TaskService.rejectFlaggedTask()): applicationStatus cua CHINH don khong doi khi
     * Admin tu choi task (don van PENDING/INQUIRING/INVITED), nen phai kiem tra rieng qua
     * taskStatus, khong the gop chung vao CHAT_TERMINAL_APPLICATION_STATUSES.
     */
    private ChatChannel requireOpenChannelLazyCreate(TaskApplicationParties parties) {
        if (CHAT_TERMINAL_APPLICATION_STATUSES.contains(parties.status())
                || parties.taskStatus() == TaskStatus.REJECTED) {
            throw new BusinessException(ErrorCode.CHANNEL_CLOSED);
        }
        ChatChannel channel = channelRepository.findByApplicationId(parties.applicationId())
                .orElseGet(() -> createChannelWithOpenerSystemMessage(parties));
        if (channel.getStatus() == ChannelStatus.CLOSED) {
            throw new BusinessException(ErrorCode.CHANNEL_CLOSED);
        }
        return channel;
    }

    /** Lay dung 1 message PRICE_PROPOSAL/RESCHEDULE_PROPOSAL dang PROPOSED thuoc dung kenh cua application - 404/409 neu khong khop. */
    private ChatMessage requirePendingProposal(UUID applicationId, UUID messageId, ChatMessageType expectedType) {
        ChatChannel channel = channelRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!message.getChannelId().equals(channel.getId()) || message.getMessageType() != expectedType) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (message.getProposalStatus() != ProposalStatus.PROPOSED) {
            throw new BusinessException(ErrorCode.CANNOT_REVOKE_RESOLVED);
        }
        return message;
    }

    /** Sinh 1 SYSTEM message ket qua xu ly de xuat (Dong y/Tu choi/Thu hoi), publish, tra ve chinh SYSTEM message do. */
    private ChatMessageResponse appendProposalOutcomeSystemMessage(TaskApplicationParties parties,
            ChatMessage proposal, String systemBody) {
        ChatMessage system = messageRepository
                .save(ChatMessage.system(UUID.randomUUID(), proposal.getChannelId(), systemBody, clock.instant()));
        ChatMessageResponse response = toResponse(system, parties);
        publish(parties, response);
        return response;
    }

    /** Gan booking vao kenh cua application (neu co) ngay sau khi UC11 chon xong nguoi thang (dac ta muc 1). */
    @Transactional
    public void attachBooking(UUID applicationId, UUID bookingId) {
        channelRepository.findByApplicationId(applicationId).ifPresent(channel -> channel.attachBooking(bookingId));
    }

    /**
     * Xem Javadoc ChatFacade.notifyApplicationConfirmed() - lazy-create kenh neu ung vien thang
     * chua tung co tin nhan nao (khac attachBooking(), chi no-op neu chua co kenh), khong dong.
     */
    @Transactional
    public void notifyApplicationConfirmed(UUID applicationId, String systemMessageBody, Instant now) {
        ChatChannel channel = channelRepository.findByApplicationId(applicationId)
                .orElseGet(() -> channelRepository.save(ChatChannel.open(UUID.randomUUID(), applicationId, now)));
        ChatMessage message = messageRepository
                .save(ChatMessage.system(UUID.randomUUID(), channel.getId(), systemMessageBody, now));
        taskFacade.getApplicationParties(applicationId)
                .ifPresent(parties -> publish(parties, toResponse(message, parties)));
    }

    /**
     * Tao kenh ngay khi Poster moi truc tiep 1 Tasker (UC09, Round B5), kem 1 SYSTEM message,
     * va them 1 PRICE_PROPOSAL (qua TaskFacade.proposePrice, giong het luong thuong luong gia
     * binh thuong) neu Poster co dinh kem gia. Khong kiem tra "dang co de xuat PROPOSED khac"
     * nhu createPriceProposal(): day la lan dau tien co noi dung trong kenh nay, khong the co
     * de xuat nao truoc do. applicationId luon la 1 dong MOI HOAN TOAN (dac ta muc 8 ban cap
     * nhat 2026-09-17) nen kenh chac chan chua ton tai - nhanh findByApplicationId chi la phong
     * ho, khong con y nghia "mo lai" nhu ban truoc.
     */
    @Transactional
    public void openChannelForInvite(UUID applicationId, UUID posterAccountId, String systemMessageBody,
            Long proposedAmount, String proposalNote, Instant now) {
        openChannelWithOptionalPriceProposal(applicationId, posterAccountId, systemMessageBody, proposedAmount,
                proposalNote, now);
    }

    /**
     * Tao kenh ngay khi Tasker ung tuyen kem "De nghi mot muc khac" (UC10, yeu cau nguoi dung
     * 2026-09-30), kem 1 SYSTEM message va 1 PRICE_PROPOSAL luon di kem (khac
     * openChannelForInvite - proposedAmount o day khong bao giat null, method nay chi duoc goi
     * khi Tasker THAT SU dinh kem gia). Dung chung helper openChannelWithOptionalPriceProposal
     * voi openChannelForInvite - hai luong deu la "lan dau tien co noi dung trong kenh nay", chi
     * khac ai la nguoi de xuat (posterAccountId vs taskerAccountId).
     */
    @Transactional
    public void openChannelForApplyWithPriceProposal(UUID applicationId, UUID taskerAccountId,
            String systemMessageBody, long proposedAmount, String proposalNote, Instant now) {
        openChannelWithOptionalPriceProposal(applicationId, taskerAccountId, systemMessageBody, proposedAmount,
                proposalNote, now);
    }

    /**
     * Logic dung chung cho openChannelForInvite/openChannelForApplyWithPriceProposal: lazy-create
     * kenh (hoac tai su dung neu da co - phong ho, khong nen xay ra vi ca 2 luong deu goi tren 1
     * applicationId vua tao), gui SYSTEM message mo dau, roi them 1 PRICE_PROPOSAL (qua
     * TaskFacade.proposePrice, giong het luong thuong luong gia binh thuong) neu proposedAmount
     * khac null. Khong kiem tra "dang co de xuat PROPOSED khac" nhu createPriceProposal(): day la
     * lan dau tien co noi dung trong kenh nay nen khong the co de xuat nao truoc do.
     */
    private void openChannelWithOptionalPriceProposal(UUID applicationId, UUID proposerAccountId,
            String systemMessageBody, Long proposedAmount, String proposalNote, Instant now) {
        ChatChannel channel = channelRepository.findByApplicationId(applicationId)
                .orElseGet(() -> channelRepository.save(ChatChannel.open(UUID.randomUUID(), applicationId, now)));

        ChatMessage lastMessage = messageRepository
                .save(ChatMessage.system(UUID.randomUUID(), channel.getId(), systemMessageBody, now));
        Long amountForResponse = null;
        if (proposedAmount != null) {
            PriceProposalCreated created = taskFacade.proposePrice(applicationId, proposerAccountId, proposedAmount,
                    proposalNote);
            lastMessage = messageRepository.save(ChatMessage.priceProposal(UUID.randomUUID(), channel.getId(),
                    proposerAccountId, proposalNote, created.priceHistoryId(), now.plusMillis(1)));
            amountForResponse = proposedAmount;
        }
        ChatMessage toPublish = lastMessage;
        Long amount = amountForResponse;
        taskFacade.getApplicationParties(applicationId)
                .ifPresent(parties -> publish(parties, toResponse(toPublish, parties, amount)));
    }

    /** Ten hien thi cua 1 trong 2 ben theo accountId, lay tu TaskApplicationParties da co san. */
    private String displayNameOf(TaskApplicationParties parties, UUID accountId) {
        return accountId.equals(parties.posterId()) ? parties.posterName() : parties.taskerName();
    }

    /** Lazy-create kenh moi + SYSTEM message mo dau, backdate ve dung thoi diem application duoc tao (dac ta muc 2). */
    private ChatChannel createChannelWithOpenerSystemMessage(TaskApplicationParties parties) {
        ChatChannel channel = channelRepository.save(ChatChannel.open(UUID.randomUUID(), parties.applicationId(),
                clock.instant()));
        String systemBody = switch (parties.status()) {
            case INVITED -> ChatSystemMessages.posterInvited(parties.posterName());
            default -> ChatSystemMessages.taskerApplied(parties.taskerName());
        };
        messageRepository.save(ChatMessage.system(UUID.randomUUID(), channel.getId(), systemBody,
                parties.createdAt()));
        return channel;
    }

    /**
     * Publish tin nhan moi len "/topic/chat/{applicationId}" va bao hieu Inbox cho ca 2 ben qua
     * "/user/.../queue/inbox" - HOAN LAI sau khi transaction bao quanh COMMIT xong (neu dang co
     * transaction) thay vi gui ngay giua transaction nhu truoc. Ly do (phat hien 2026-09-22, xem
     * docs/PROGRESS-CHAT-MODULE.md): FE nhan push xong goi lai NGAY 1 REST GET moi de lam moi
     * toan bo danh sach tin nhan (xem InboxPage.tsx, moi khi nhan 1 tin SYSTEM); neu ban ghi vua
     * ghi trong DB CHUA COMMIT (con dang nam giua 1 method @Transactional dai, vd
     * TaskApplicationService.confirm() con lam tiep nhieu viec khac - cascade REJECTED_AUTO cho
     * cac ung vien con lai - sau khi da publish cho ung vien thang) thi GET do co the chay o 1
     * transaction/connection khac, doc DUOI muc isolation READ_COMMITTED se KHONG thay ban ghi
     * chua commit - ket qua la tin nhan hien ra 1 nhip roi bien mat khi FE ghi de bang du lieu
     * cu vua fetch lai (khong phai loi thieu tin nhan trong DB, tin van luu dung, chi la client
     * nhan push som hon luc du lieu thuc su san sang doc lai). Dung
     * TransactionSynchronizationManager thay vi RabbitMQ - day KHONG phai luong bat dong bo moi
     * theo GUARDRAIL 1 cua CLAUDE.md, chi doi THOI DIEM goi 1 lenh dong bo da co san (STOMP qua
     * SimpMessagingTemplate) sang sau commit, van trong cung 1 request/response.
     */
    private void publish(TaskApplicationParties parties, ChatMessageResponse response) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doPublish(parties, response);
                }
            });
        } else {
            doPublish(parties, response);
        }
    }

    /** Phan gui STOMP that su - xem Javadoc publish() ve ly do tach rieng va hoan lai sau commit. */
    private void doPublish(TaskApplicationParties parties, ChatMessageResponse response) {
        messagingTemplate.convertAndSend("/topic/chat/" + parties.applicationId(), response);
        InboxPingEvent ping = new InboxPingEvent(parties.applicationId());
        messagingTemplate.convertAndSendToUser(parties.posterId().toString(), "/queue/inbox", ping);
        messagingTemplate.convertAndSendToUser(parties.taskerId().toString(), "/queue/inbox", ping);
    }

    /** Doc TaskApplicationParties, 404 (tai dung ma APPLICATION_NOT_FOUND cua Task) neu application khong ton tai. */
    private TaskApplicationParties requireParties(UUID applicationId) {
        return taskFacade.getApplicationParties(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    /** 403 neu accountId khong phai Poster hoac Tasker cua application (dac ta muc 11). */
    private void requireParty(TaskApplicationParties parties, UUID accountId) {
        if (!accountId.equals(parties.posterId()) && !accountId.equals(parties.taskerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_CHANNEL);
        }
    }

    /** Doc amount tu TaskFacade neu la PRICE_PROPOSAL, null cho cac loai con lai. */
    private Long amountOf(ChatMessage message) {
        return message.getMessageType() == ChatMessageType.PRICE_PROPOSAL
                ? taskFacade.findPriceHistoryAmount(message.getRefPriceHistoryId()).orElse(null)
                : null;
    }

    /**
     * Doc chi tiet batch "Chi phi phat sinh" tu TaskFacade neu la EXTRA_COST_BATCH, null cho cac
     * loai con lai (them 2026-10-03) - LUON doc tuoi, khong cache, de status/items phan anh dung
     * trang thai hien tai cua task_extra_cost_batches (xem Javadoc ChatMessageType.EXTRA_COST_BATCH).
     */
    private ExtraCostBatchSummary extraCostBatchOf(ChatMessage message) {
        return message.getMessageType() == ChatMessageType.EXTRA_COST_BATCH
                ? taskFacade.findExtraCostBatch(message.getRefExtraCostBatchId()).orElse(null)
                : null;
    }

    /**
     * Anh xa entity ChatMessage sang DTO tra ve client - dung cho cac thao tac tren TUNG tin nhan
     * rieng le (gui/thu hoi/tha reaction/ghim...), tu truy van attachments/reactions/pin CUA
     * DUNG tin nay (vai truy van nho, chap nhan duoc o quy mo 2 nguoi/kenh). Khi liet ke CA kenh,
     * dung listMessagesOfChannel() (nap gop, tranh N+1) thay vi goi lap lai ham nay.
     */
    private ChatMessageResponse toResponse(ChatMessage message, TaskApplicationParties parties) {
        return toResponse(message, parties, amountOf(message));
    }

    /** Nhu toResponse() nhung nhan san amount (vd vua tao de xuat, tranh goi lai TaskFacade khong can thiet). */
    private ChatMessageResponse toResponse(ChatMessage message, TaskApplicationParties parties, Long amount) {
        List<ChatMessageAttachment> attachments = attachmentRepository
                .findByMessageIdOrderBySortOrderAsc(message.getId());
        List<ChatMessageReaction> reactions = reactionRepository.findByMessageIdIn(List.of(message.getId()));
        ChatPinnedMessage pinned = pinnedMessageRepository.findByChannelIdOrderByCreatedAtAsc(message.getChannelId())
                .stream().filter(p -> p.getMessageId().equals(message.getId())).findFirst().orElse(null);
        return assemble(message, parties, amount, attachments, reactions, pinned);
    }

    /**
     * Ghep 1 ChatMessage + du lieu lien quan (da nap san, tu toResponse() hoac
     * listMessagesOfChannel()) thanh DTO tra ve client. Khi recalledAt khac null: body/
     * attachments/reactions LUON rong/null trong response BAT KE du lieu that trong DB (thu hoi
     * la mem, xem Javadoc ChatMessage.markRecalled) - dung cho ca hai ben (nguoi gui va nguoi
     * nhan deu thay dong thay the, rule da chot voi nguoi dung 2026-09-26).
     */
    private ChatMessageResponse assemble(ChatMessage message, TaskApplicationParties parties, Long amount,
            List<ChatMessageAttachment> attachments, List<ChatMessageReaction> reactions, ChatPinnedMessage pinned) {
        UUID senderId = message.getSenderAccountId();
        boolean fromPoster = senderId != null && senderId.equals(parties.posterId());
        String senderName = senderId == null ? null : fromPoster ? parties.posterName() : parties.taskerName();
        String senderAvatarUrl = senderId == null ? null
                : fromPoster ? parties.posterAvatarUrl() : parties.taskerAvatarUrl();
        boolean recalled = message.isRecalled();
        List<ChatAttachmentResponse> attachmentResponses = recalled ? List.of()
                : attachments.stream().map(this::toAttachmentResponse).toList();
        List<ChatReactionResponse> reactionResponses = recalled ? List.of()
                : reactions.stream()
                        .map(r -> new ChatReactionResponse(r.getAccountId(), displayNameOf(parties, r.getAccountId()),
                                r.getEmoji()))
                        .toList();
        String body = recalled ? null : message.getBody();
        Instant recallableUntil = computeRecallableUntil(message);
        Instant pinnedAt = pinned != null ? pinned.getCreatedAt() : null;
        UUID pinnedByAccountId = pinned != null ? pinned.getPinnedByAccountId() : null;
        String pinnedByName = pinned != null ? displayNameOf(parties, pinned.getPinnedByAccountId()) : null;
        return new ChatMessageResponse(message.getId(), message.getChannelId(), senderId, senderName,
                senderAvatarUrl, message.getMessageType(), body, message.getRefPriceHistoryId(), amount,
                message.getProposedTime(), message.getProposalStatus(), message.getCreatedAt(), attachmentResponses,
                message.getReplyToMessageId(), message.getRecalledAt(), recallableUntil, reactionResponses,
                pinnedAt, pinnedByAccountId, pinnedByName, message.getRefTaskEditId(),
                message.getRefExtraCostBatchId(), extraCostBatchOf(message));
    }

    /** Moc thoi gian con thu hoi duoc - null neu da bi thu hoi hoac khong thuoc loai thu hoi duoc (xem RECALLABLE_MESSAGE_TYPES). */
    private Instant computeRecallableUntil(ChatMessage message) {
        if (message.isRecalled() || !RECALLABLE_MESSAGE_TYPES.contains(message.getMessageType())) {
            return null;
        }
        return message.getCreatedAt().plus(Duration.ofMinutes(adminFacade.getChatMessageRecallWindowMinutes()));
    }

    /** Anh xa 1 dong dinh kem sang DTO - ky MOI 1 presigned GET moi lan goi (prefix rieng tu, khong public-read). */
    private ChatAttachmentResponse toAttachmentResponse(ChatMessageAttachment attachment) {
        String url = s3Service.createPresignedGetUrl(attachment.getStorageKey(), ATTACHMENT_GET_URL_TTL);
        return new ChatAttachmentResponse(url, attachment.getFileName(), attachment.getMimeType(),
                attachment.getFileSizeBytes(), attachment.getSortOrder());
    }

    /** Lay dung 1 tin nhan (bat ky loai nao) thuoc dung kenh cua application - 404 neu khong khop. */
    private ChatMessage requireMessageInChannel(UUID applicationId, UUID messageId) {
        ChatChannel channel = channelRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!message.getChannelId().equals(channel.getId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return message;
    }

    /**
     * Kiem tra 1 replyToMessageId (neu co) hop le: ton tai, cung kenh cua application, khong
     * phai SYSTEM, chua bi thu hoi - tra ve nguyen id neu hop le, null neu dau vao la null.
     */
    private UUID validateReplyTarget(UUID applicationId, UUID replyToMessageId) {
        if (replyToMessageId == null) {
            return null;
        }
        ChatMessage target = requireMessageInChannel(applicationId, replyToMessageId);
        if (target.getMessageType() == ChatMessageType.SYSTEM) {
            throw new BusinessException(ErrorCode.CANNOT_REPLY_TO_SYSTEM);
        }
        if (target.isRecalled()) {
            throw new BusinessException(ErrorCode.MESSAGE_RECALLED);
        }
        return replyToMessageId;
    }

    /**
     * Kiem tra so luong file va dung luong khai bao cua tung file so voi nguong doc tu
     * AdminFacade (theo dung "kind" IMAGE/VIDEO/FILE/VOICE) - KHONG xac minh lai dung luong that
     * voi S3 (cung han che voi ADR-003/004, xem Javadoc ChatAttachmentUploadService). Rieng
     * VOICE: thoi luong toi da (chat_voice_max_duration_seconds) KHONG duoc kiem tra o day - BE
     * khong doc metadata audio, chi FE tu ngat ghi am dung nguong khi thu (xem
     * VoiceRecorderButton.tsx).
     */
    private void validateAttachmentLimits(ChatMessageType type,
            List<SendAttachmentMessageRequest.AttachmentInput> attachments) {
        int maxCount = switch (type) {
            case IMAGE -> adminFacade.getChatImageMaxCountPerMessage();
            case VIDEO -> adminFacade.getChatVideoMaxCountPerMessage();
            case FILE -> adminFacade.getChatFileMaxCountPerMessage();
            case VOICE -> 1;
            default -> 0;
        };
        long maxSizeBytes = switch (type) {
            case IMAGE -> adminFacade.getChatImageMaxSizeMb() * 1024 * 1024;
            case VIDEO -> adminFacade.getChatVideoMaxSizeMb() * 1024 * 1024;
            case FILE -> adminFacade.getChatFileMaxSizeMb() * 1024 * 1024;
            case VOICE -> adminFacade.getChatVoiceMaxSizeMb() * 1024 * 1024;
            default -> 0;
        };
        if (attachments.isEmpty() || attachments.size() > maxCount) {
            throw new BusinessException(ErrorCode.ATTACHMENT_LIMIT_EXCEEDED);
        }
        for (SendAttachmentMessageRequest.AttachmentInput input : attachments) {
            if (input.fileSizeBytes() > maxSizeBytes) {
                throw new BusinessException(ErrorCode.ATTACHMENT_LIMIT_EXCEEDED);
            }
        }
    }

    /**
     * Anh xa 1 channel + parties + tin nhan gan nhat sang 1 dong Inbox, tinh vai tro dong theo
     * accountId dang xem. paymentMethod chi doc qua BookingFacade (Chat -> Booking, dong bo, da
     * cho phep san) khi channel.bookingId != null - tranh goi thua cho cac dong chua co booking.
     */
    private ChatInboxItemResponse toInboxItem(ChatChannel channel, TaskApplicationParties parties,
            ChatMessage lastMessage, UUID viewerAccountId, boolean needsResponse) {
        boolean viewerIsPoster = viewerAccountId.equals(parties.posterId());
        boolean hasBooking = channel.getBookingId() != null;
        return new ChatInboxItemResponse(
                parties.applicationId(), channel.getId(), parties.taskId(), parties.taskTitle(),
                viewerIsPoster ? parties.taskerId() : parties.posterId(),
                viewerIsPoster ? parties.taskerName() : parties.posterName(),
                viewerIsPoster ? parties.taskerAvatarUrl() : parties.posterAvatarUrl(),
                viewerIsPoster ? "POSTER" : "TASKER",
                channel.getStatus(),
                lastMessage != null ? previewOf(lastMessage) : null,
                lastMessage != null ? lastMessage.getCreatedAt() : channel.getCreatedAt(),
                needsResponse,
                parties.status(),
                hasBooking,
                parties.taskStatus(),
                hasBooking ? bookingFacade.findByApplicationId(parties.applicationId())
                        .map(BookingSummary::paymentMethod).orElse(null) : null);
    }

    /**
     * Trang thai application + task cha cho 1 applicationId, KHONG doi hoi kenh vat ly da ton
     * tai - dung khi FE mo mot cuoc tro chuyen chua tung co kenh (xem Javadoc
     * ChatApplicationStatusResponse) de biet truoc co nen chan gui/hien thong bao truoc khi
     * nguoi dung thu gui tin dau tien.
     */
    @Transactional(readOnly = true)
    public ChatApplicationStatusResponse getApplicationChatStatus(UUID applicationId, UUID requesterAccountId) {
        TaskApplicationParties parties = requireParties(applicationId);
        requireParty(parties, requesterAccountId);
        boolean viewerIsPoster = requesterAccountId.equals(parties.posterId());
        return new ChatApplicationStatusResponse(
                parties.applicationId(), parties.taskId(), parties.taskTitle(),
                viewerIsPoster ? parties.taskerId() : parties.posterId(),
                viewerIsPoster ? parties.taskerName() : parties.posterName(),
                viewerIsPoster ? parties.taskerAvatarUrl() : parties.posterAvatarUrl(),
                viewerIsPoster ? "POSTER" : "TASKER",
                parties.status(), parties.taskStatus());
    }

    /**
     * Doan xem truoc hien trong Inbox. Uu tien kiem tra thu hoi truoc (them 2026-09-26) - tin da
     * thu hoi luon hien 1 dong co dinh, bat ke messageType goc la gi.
     */
    private String previewOf(ChatMessage message) {
        if (message.isRecalled()) {
            return "Tin nhắn đã được thu hồi";
        }
        return switch (message.getMessageType()) {
            case TEXT, SYSTEM -> message.getBody();
            case IMAGE -> "Đã gửi hình ảnh";
            case VIDEO -> "Đã gửi video";
            case VOICE -> "Đã gửi tin nhắn thoại";
            case FILE -> "Đã gửi file";
            case PRICE_PROPOSAL -> switch (message.getProposalStatus()) {
                case PROPOSED -> "Đề xuất giá mới";
                case ACCEPTED -> "Đã đồng ý mức giá";
                case REJECTED -> "Đề xuất giá đã kết thúc";
            };
            case RESCHEDULE_PROPOSAL -> switch (message.getProposalStatus()) {
                case PROPOSED -> "Đề xuất đổi lịch mới";
                case ACCEPTED -> "Đã đồng ý đổi lịch";
                case REJECTED -> "Đề xuất đổi lịch đã kết thúc";
            };
            case EXTRA_COST_BATCH -> "Chi phí phát sinh";
        };
    }

    /**
     * "Can phan hoi" = kenh dang OPEN VA (co 1 de xuat PROPOSED khong phai do minh tao, HOAC
     * tin nhan gan nhat khong phai do minh gui) - dac ta muc 10 "co tin/de xuat dang cho dung
     * luot ho xu ly". Ve du tach rieng đieu kien de xuat thay vi chi dua vao tin nhan gan nhat,
     * vi neu proposer nhan them 1 tin TEXT sau de xuat, tin gan nhat khong con la de xuat nua
     * nhung de xuat do VAN dang cho ben kia xu ly.
     */
    private boolean computeNeedsResponse(ChatChannel channel, ChatMessage lastMessage, UUID accountId) {
        if (channel.getStatus() != ChannelStatus.OPEN) {
            return false;
        }
        boolean hasPendingPriceProposalNotMine = messageRepository
                .existsByChannelIdAndMessageTypeAndProposalStatusAndSenderAccountIdNot(channel.getId(),
                        ChatMessageType.PRICE_PROPOSAL, ProposalStatus.PROPOSED, accountId);
        boolean hasPendingRescheduleProposalNotMine = messageRepository
                .existsByChannelIdAndMessageTypeAndProposalStatusAndSenderAccountIdNot(channel.getId(),
                        ChatMessageType.RESCHEDULE_PROPOSAL, ProposalStatus.PROPOSED, accountId);
        // SYSTEM khong tinh - senderAccountId luon null nen "!accountId.equals(null)" luon dung,
        // se bao needsResponse=true cho CA HAI phia moi khi tin gan nhat la 1 dong SYSTEM (vd
        // vua Chon nguoi nay/vua mo loi moi khong kem gia) du khong ai thuc su can tra loi ai -
        // phat hien 2026-09-21 khi kenh cua ung vien thang sau UC11 khong roi dung vao tab "Dang
        // thuc hien" nhu ky vong (xem docs/PROGRESS-CHAT-MODULE.md).
        boolean lastMessageNotMine = lastMessage != null && lastMessage.getMessageType() != ChatMessageType.SYSTEM
                && !accountId.equals(lastMessage.getSenderAccountId());
        return hasPendingPriceProposalNotMine || hasPendingRescheduleProposalNotMine || lastMessageNotMine;
    }

    /**
     * Tab nao khop voi 1 channel. SUA 2026-09-22 (thay dinh nghia tam cua Round B2 - xem Javadoc
     * InboxTab.java): IN_PROGRESS truoc day la "OPEN va khong can phan hoi ngay" - nghia la 1
     * kenh dang OPEN cho doi ben kia tra loi (chua ai duoc chon, chua co booking) cung nhay vao
     * day ngay khi minh vua tra loi xong, trong khi nguoi dung xac nhan IN_PROGRESS phai la
     * "da duoc chon/da tao booking" (dac ta UC11), khong phai suy tu needsResponse. Kenh dang
     * OPEN, khong can phan hoi, CHUA co booking gio khong khop tab rieng nao (chi con ALL) -
     * dung theo yeu cau nguoi dung, khong tu suy dien.
     */
    private boolean matchesTab(InboxTab tab, ChannelStatus status, boolean needsResponse, boolean hasBooking) {
        return switch (tab) {
            case ALL -> true;
            case NEEDS_RESPONSE -> needsResponse;
            case IN_PROGRESS -> status == ChannelStatus.OPEN && hasBooking;
            case CLOSED -> status == ChannelStatus.CLOSED;
        };
    }
}
