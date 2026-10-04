package vn.taskconnect.task.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.booking.api.BookingFacade;
import vn.taskconnect.booking.api.PaymentMethod;
import vn.taskconnect.booking.api.dto.BookingEscrowSummary;
import vn.taskconnect.chat.api.ChatFacade;
import vn.taskconnect.chat.api.ChatSystemMessages;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.task.api.ExtraCostBatchStatus;
import vn.taskconnect.task.api.TaskStatus;
import vn.taskconnect.task.dto.request.ExtraCostItemInput;
import vn.taskconnect.task.dto.response.ExtraCostBatchResponse;
import vn.taskconnect.task.dto.response.ExtraCostItemResponse;
import vn.taskconnect.task.dto.response.ExtraCostMoneySummaryResponse;
import vn.taskconnect.task.entity.Task;
import vn.taskconnect.task.entity.TaskApplication;
import vn.taskconnect.task.entity.TaskExtraCostBatch;
import vn.taskconnect.task.entity.TaskExtraCostItem;
import vn.taskconnect.task.repository.TaskApplicationRepository;
import vn.taskconnect.task.repository.TaskExtraCostBatchRepository;
import vn.taskconnect.task.repository.TaskExtraCostItemRepository;
import vn.taskconnect.task.repository.TaskRepository;
import vn.taskconnect.user.api.UserFacade;
import vn.taskconnect.user.api.dto.UserProfileSummary;

/**
 * Nghiep vu "Chi phi phat sinh" (quyet dinh nguoi dung 2026-10-02) - Tasker da ung tien mua vat
 * tu/cong lam phat sinh, dang len lam bang chung minh bach de Poster tra lai sau khi hoan thanh.
 * CHI ap dung khi booking cua application da chon FULL_ESCROW (xem requireFullEscrow) - PA
 * FEE_ONLY_ESCROW khong dung tinh nang nay vi 92% da thanh toan ngoai he thong tu dau, khong can
 * ghi nhan gi them qua day. Day la co che RIENG, KHONG tai su dung task_price_history/ChangeType
 * (xem Javadoc TaskPriceNegotiationService): price_history chi luu 1 so tien THAY THE (renegotiate
 * gia chot), con o day la danh sach nhieu khoan DOC LAP cong don vao so tien Poster phai tra THEM,
 * moi khoan co ten/anh rieng, gom thanh tung "batch" (xem TaskExtraCostBatch/TaskExtraCostItem).
 *
 * <p>approve()/reject()/withdraw() CHI doi trang thai batch, KHONG tu dong nap tien - Poster phai
 * chu dong bam "Nap" (topUp()) de thuc su chuyen tien vao tam giu, dung y ảnh mau nguoi dung gui
 * (duyet xong van con hien "Can nap them" rieng biet voi hanh dong Dong y).
 */
@Service
public class TaskExtraCostService {

    private final TaskApplicationRepository applicationRepository;
    private final TaskRepository taskRepository;
    private final TaskExtraCostBatchRepository batchRepository;
    private final TaskExtraCostItemRepository itemRepository;
    private final BookingFacade bookingFacade;
    private final ChatFacade chatFacade;
    private final UserFacade userFacade;
    private final Clock clock;

    public TaskExtraCostService(TaskApplicationRepository applicationRepository, TaskRepository taskRepository,
            TaskExtraCostBatchRepository batchRepository, TaskExtraCostItemRepository itemRepository,
            BookingFacade bookingFacade, ChatFacade chatFacade, UserFacade userFacade, Clock clock) {
        this.applicationRepository = applicationRepository;
        this.taskRepository = taskRepository;
        this.batchRepository = batchRepository;
        this.itemRepository = itemRepository;
        this.bookingFacade = bookingFacade;
        this.chatFacade = chatFacade;
        this.userFacade = userFacade;
        this.clock = clock;
    }

    /**
     * Tasker dang 1 batch moi (co the gom nhieu khoan). Gate: application phai thuoc ve chinh
     * taskerId nay, task phai dang ASSIGNED, booking phai chon FULL_ESCROW, va khong duoc co
     * batch nao khac dang PENDING cho cung application (suy luan tuong tu rule "khong co de xuat
     * gia dang cho" cua TaskApplicationService.confirm() - chua duoc nguoi dung xac nhan minh
     * thi, co the dieu chinh neu can). totalAmount cache luc dang, khong tinh lai sau do. Dang
     * xong gui 1 tin nhan EXTRA_COST_BATCH vao chat (the rieng, giong PRICE_PROPOSAL - bo sung
     * 2026-10-03), KHONG con dong SYSTEM text rieng nhu truoc.
     */
    @Transactional
    public ExtraCostMoneySummaryResponse submit(UUID taskerId, UUID applicationId, String note,
            List<ExtraCostItemInput> items) {
        TaskApplication application = requireOwnTaskerApplication(taskerId, applicationId);
        Task task = requireTask(application.getTaskId());
        requireAssigned(task);
        requireFullEscrow(applicationId);
        if (batchRepository.existsByApplicationIdAndStatus(applicationId, ExtraCostBatchStatus.PENDING)) {
            throw new BusinessException(ErrorCode.EXTRA_COST_BATCH_PENDING);
        }

        long totalAmount = items.stream().mapToLong(ExtraCostItemInput::amount).sum();
        int batchNo = batchRepository.countByApplicationId(applicationId) + 1;
        Instant now = clock.instant();
        TaskExtraCostBatch batch = TaskExtraCostBatch.submit(UUID.randomUUID(), batchNo, applicationId, totalAmount,
                note, taskerId, now);
        batchRepository.save(batch);
        itemRepository.saveAll(toItemEntities(batch.getId(), items, now));

        chatFacade.postExtraCostBatchMessageIfOpen(applicationId, taskerId, batch.getId());
        return buildSummary(applicationId);
    }

    /** Tasker tu thu hoi batch do chinh minh dang, chi khi dang PENDING. */
    @Transactional
    public ExtraCostMoneySummaryResponse withdraw(UUID taskerId, UUID applicationId, UUID batchId) {
        requireOwnTaskerApplication(taskerId, applicationId);
        TaskExtraCostBatch batch = requirePendingBatch(applicationId, batchId);
        batch.withdraw(clock.instant());
        chatFacade.postSystemMessageIfOpen(applicationId,
                ChatSystemMessages.extraCostWithdrawn(resolveDisplayName(taskerId)));
        return buildSummary(applicationId);
    }

    /** Poster dong y batch dang PENDING - chi doi trang thai, KHONG tu dong nap tien (xem Javadoc class, Poster phai tu bam "Nap" rieng). */
    @Transactional
    public ExtraCostMoneySummaryResponse approve(UUID posterId, UUID applicationId, UUID batchId) {
        requireOwnPosterApplication(posterId, applicationId);
        TaskExtraCostBatch batch = requirePendingBatch(applicationId, batchId);
        batch.approve(posterId, clock.instant());
        chatFacade.postSystemMessageIfOpen(applicationId,
                ChatSystemMessages.extraCostApproved(resolveDisplayName(posterId), batch.getTotalAmount()));
        return buildSummary(applicationId);
    }

    /** Poster tu choi batch dang PENDING. */
    @Transactional
    public ExtraCostMoneySummaryResponse reject(UUID posterId, UUID applicationId, UUID batchId) {
        requireOwnPosterApplication(posterId, applicationId);
        TaskExtraCostBatch batch = requirePendingBatch(applicationId, batchId);
        batch.reject(posterId, clock.instant());
        chatFacade.postSystemMessageIfOpen(applicationId,
                ChatSystemMessages.extraCostRejected(resolveDisplayName(posterId)));
        return buildSummary(applicationId);
    }

    /**
     * Poster bam "Nap" - nap them (gia lap) de so dang tam giu dat dung requiredTotal hien tai
     * (chi phi chot ban dau + tong cac batch da duyet). Khong lam gi neu da du (xem
     * PaymentFacadeImpl.topUpToTarget, khong nap am/nap trung).
     */
    @Transactional
    public ExtraCostMoneySummaryResponse topUp(UUID posterId, UUID applicationId) {
        requireOwnPosterApplication(posterId, applicationId);
        ExtraCostMoneySummaryResponse current = buildSummary(applicationId);
        bookingFacade.topUpEscrow(applicationId, current.requiredTotal());
        return buildSummary(applicationId);
    }

    /** Xem tong hop tien cua 1 application - ca Poster va Tasker cua don do xem duoc. */
    @Transactional(readOnly = true)
    public ExtraCostMoneySummaryResponse getSummary(UUID viewerId, UUID applicationId) {
        requireParty(viewerId, applicationId);
        return buildSummary(applicationId);
    }

    /** Application phai ton tai va thuoc ve dung taskerId nay - gop 403/404 thanh APPLICATION_NOT_FOUND, tranh lo du lieu. */
    private TaskApplication requireOwnTaskerApplication(UUID taskerId, UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .filter(application -> application.getTaskerId().equals(taskerId))
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    /** Application phai ton tai va task cha phai thuoc ve dung posterId nay - gop 403/404 thanh APPLICATION_NOT_FOUND. */
    private TaskApplication requireOwnPosterApplication(UUID posterId, UUID applicationId) {
        TaskApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        Task task = requireTask(application.getTaskId());
        if (!task.getPosterId().equals(posterId)) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }
        return application;
    }

    /** Nguoi xem phai la Poster (chu task) hoac chinh Tasker cua application nay. */
    private void requireParty(UUID viewerId, UUID applicationId) {
        TaskApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        Task task = requireTask(application.getTaskId());
        boolean isParty = viewerId.equals(task.getPosterId()) || viewerId.equals(application.getTaskerId());
        if (!isParty) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private Task requireTask(UUID taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
    }

    /** Chi dang chi phi phat sinh duoc khi cong viec dang ASSIGNED (da giao, chua ket thuc). */
    private void requireAssigned(Task task) {
        if (task.getStatus() != TaskStatus.ASSIGNED) {
            throw new BusinessException(ErrorCode.TASK_NOT_ASSIGNED_FOR_EXTRA_COST);
        }
    }

    /** Chi dung duoc khi booking cua application nay chon FULL_ESCROW - xem Javadoc class. */
    private void requireFullEscrow(UUID applicationId) {
        BookingEscrowSummary escrow = bookingFacade.getEscrowBreakdown(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));
        if (escrow.paymentMethod() != PaymentMethod.FULL_ESCROW) {
            throw new BusinessException(ErrorCode.EXTRA_COST_NOT_ALLOWED_FOR_PAYMENT_METHOD);
        }
    }

    /** 1 batch phai ton tai, thuoc dung application, va dang PENDING - dung chung cho withdraw/approve/reject. */
    private TaskExtraCostBatch requirePendingBatch(UUID applicationId, UUID batchId) {
        TaskExtraCostBatch batch = batchRepository.findByIdAndApplicationId(batchId, applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXTRA_COST_BATCH_NOT_FOUND));
        if (batch.getStatus() != ExtraCostBatchStatus.PENDING) {
            throw new BusinessException(ErrorCode.EXTRA_COST_BATCH_NOT_PENDING);
        }
        return batch;
    }

    private List<TaskExtraCostItem> toItemEntities(UUID batchId, List<ExtraCostItemInput> items, Instant now) {
        List<TaskExtraCostItem> entities = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ExtraCostItemInput input = items.get(i);
            entities.add(TaskExtraCostItem.of(UUID.randomUUID(), batchId, input.name(), input.amount(),
                    input.photoUrl(), i, now));
        }
        return entities;
    }

    /**
     * Ghep feeBaseAmount/paymentMethod/heldAmount that (qua BookingFacade) voi danh sach batch
     * da duyet + batch dang cho (neu co) thanh 1 ExtraCostMoneySummaryResponse day du. requiredTotal
     * = feeBaseAmount + tong cac batch APPROVED; deltaNeeded = phan con thieu so voi heldAmount
     * hien tai (khong am - da du hoac thua thi deltaNeeded = 0).
     */
    private ExtraCostMoneySummaryResponse buildSummary(UUID applicationId) {
        BookingEscrowSummary escrow = bookingFacade.getEscrowBreakdown(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));
        List<TaskExtraCostBatch> batches = batchRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId);
        Map<UUID, List<TaskExtraCostItem>> itemsByBatchId = itemRepository
                .findByBatchIdInOrderBySortOrderAsc(batches.stream().map(TaskExtraCostBatch::getId).toList())
                .stream().collect(Collectors.groupingBy(TaskExtraCostItem::getBatchId));

        List<ExtraCostBatchResponse> approvedBatches = batches.stream()
                .filter(batch -> batch.getStatus() == ExtraCostBatchStatus.APPROVED)
                .map(batch -> toBatchResponse(batch, itemsByBatchId.getOrDefault(batch.getId(), List.of())))
                .toList();
        ExtraCostBatchResponse pendingBatch = batches.stream()
                .filter(batch -> batch.getStatus() == ExtraCostBatchStatus.PENDING)
                .findFirst()
                .map(batch -> toBatchResponse(batch, itemsByBatchId.getOrDefault(batch.getId(), List.of())))
                .orElse(null);

        long approvedExtraTotal = approvedBatches.stream().mapToLong(ExtraCostBatchResponse::totalAmount).sum();
        long requiredTotal = escrow.feeBaseAmount() + approvedExtraTotal;
        long deltaNeeded = Math.max(0, requiredTotal - escrow.heldAmount());
        return new ExtraCostMoneySummaryResponse(escrow.feeBaseAmount(), escrow.paymentMethod(), escrow.heldAmount(),
                approvedBatches, pendingBatch, approvedExtraTotal, requiredTotal, deltaNeeded);
    }

    private ExtraCostBatchResponse toBatchResponse(TaskExtraCostBatch batch, List<TaskExtraCostItem> items) {
        List<ExtraCostItemResponse> itemResponses = items.stream()
                .map(item -> new ExtraCostItemResponse(item.getId(), item.getName(), item.getAmount(),
                        item.getPhotoUrl()))
                .toList();
        String reviewedByName = batch.getReviewedByAccountId() != null
                ? resolveDisplayName(batch.getReviewedByAccountId()) : null;
        return new ExtraCostBatchResponse(batch.getId(), batch.getBatchNo(), batch.getStatus(), batch.getNote(),
                itemResponses, batch.getTotalAmount(), batch.getSubmittedByAccountId(),
                resolveDisplayName(batch.getSubmittedByAccountId()), batch.getSubmittedAt(),
                batch.getReviewedByAccountId(), reviewedByName, batch.getReviewedAt());
    }

    private String resolveDisplayName(UUID accountId) {
        return userFacade.findProfile(accountId).map(UserProfileSummary::fullName).orElse(null);
    }
}
