package vn.taskconnect.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.taskconnect.chat.api.ChatMessageType;
import vn.taskconnect.chat.api.ProposalStatus;

/**
 * Mot tin nhan hoac de xuat trong 1 kenh chat. Xem TaskConnect_Chat_ImplementationSpec.md
 * muc 1. Factory rieng cho PRICE_PROPOSAL/RESCHEDULE_PROPOSAL them o Round B3/B6 khi can toi.
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "channel_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID channelId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "sender_account_id", columnDefinition = "BINARY(16)", updatable = false)
    private UUID senderAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20, updatable = false)
    private ChatMessageType messageType;

    @Column(name = "body", columnDefinition = "TEXT", updatable = false)
    private String body;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "ref_price_history_id", columnDefinition = "BINARY(16)", updatable = false)
    private UUID refPriceHistoryId;

    /** Id dong task_edit_events tuong ung - chi co khi messageType = SYSTEM sinh tu UC07 (Poster sua viec). */
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "ref_task_edit_id", columnDefinition = "BINARY(16)", updatable = false)
    private UUID refTaskEditId;

    /** Id dong task_extra_cost_batches tuong ung - chi co khi messageType = EXTRA_COST_BATCH (them 2026-10-03). */
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "ref_extra_cost_batch_id", columnDefinition = "BINARY(16)", updatable = false)
    private UUID refExtraCostBatchId;

    @Column(name = "proposed_time", updatable = false)
    private Instant proposedTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposal_status", length = 10)
    private ProposalStatus proposalStatus;

    /** Khac null = tin da bi chinh nguoi gui thu hoi (them 2026-09-26, an voi ca hai ben). */
    @Column(name = "recalled_at")
    private Instant recalledAt;

    /** Tin nhan dang tra loi (quote), cung kenh - null neu khong phai tra loi (them 2026-09-26). */
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "reply_to_message_id", columnDefinition = "BINARY(16)", updatable = false)
    private UUID replyToMessageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatMessage() {
        // JPA
    }

    /** Tin nhan tu do giua Poster/Tasker - noi dung khong duoc he thong theo doi/doi chieu (dac ta muc 3). */
    public static ChatMessage text(UUID id, UUID channelId, UUID senderAccountId, String body, Instant now) {
        return text(id, channelId, senderAccountId, body, null, now);
    }

    /** Nhu text() nhung co the tra loi (quote) 1 tin nhan khac cung kenh (them 2026-09-26). */
    public static ChatMessage text(UUID id, UUID channelId, UUID senderAccountId, String body,
            UUID replyToMessageId, Instant now) {
        ChatMessage message = new ChatMessage();
        message.id = id;
        message.channelId = channelId;
        message.senderAccountId = senderAccountId;
        message.messageType = ChatMessageType.TEXT;
        message.body = body;
        message.replyToMessageId = replyToMessageId;
        message.createdAt = now;
        return message;
    }

    /**
     * Tin nhan dinh kem file (IMAGE/FILE/VIDEO) - cac dong chat_message_attachments duoc luu
     * RIENG boi ChatService sau khi message nay da co id (them 2026-09-26). caption la chu
     * thich tuy chon (co the null), tra loi (quote) cung tuy chon nhu text().
     */
    public static ChatMessage attachmentMessage(UUID id, UUID channelId, UUID senderAccountId,
            ChatMessageType type, String caption, UUID replyToMessageId, Instant now) {
        ChatMessage message = new ChatMessage();
        message.id = id;
        message.channelId = channelId;
        message.senderAccountId = senderAccountId;
        message.messageType = type;
        message.body = caption;
        message.replyToMessageId = replyToMessageId;
        message.createdAt = now;
        return message;
    }

    /** Tin nhan he thong tu dong sinh ra (mo kenh, dong kenh...) - khong co nguoi gui, xem ChatSystemMessages. */
    public static ChatMessage system(UUID id, UUID channelId, String body, Instant now) {
        ChatMessage message = new ChatMessage();
        message.id = id;
        message.channelId = channelId;
        message.messageType = ChatMessageType.SYSTEM;
        message.body = body;
        message.createdAt = now;
        return message;
    }

    /**
     * Nhu system() nhung mang kem id cua 1 su kien Poster sua cong viec (UC07) - de FE hien nut
     * "Xem chi tiet thay doi" tren dung tin nay. messageType van la SYSTEM, khong them loai moi.
     */
    public static ChatMessage taskEditSystem(UUID id, UUID channelId, String body, UUID refTaskEditId,
            Instant now) {
        ChatMessage message = system(id, channelId, body, now);
        message.refTaskEditId = refTaskEditId;
        return message;
    }

    /**
     * De xuat gia moi, luon bat dau PROPOSED - so tien that nam o task_price_history (qua
     * refPriceHistoryId), khong luu lai o day de tranh 2 nguon su that (dac ta muc 3).
     */
    public static ChatMessage priceProposal(UUID id, UUID channelId, UUID senderAccountId, String note,
            UUID refPriceHistoryId, Instant now) {
        ChatMessage message = new ChatMessage();
        message.id = id;
        message.channelId = channelId;
        message.senderAccountId = senderAccountId;
        message.messageType = ChatMessageType.PRICE_PROPOSAL;
        message.body = note;
        message.refPriceHistoryId = refPriceHistoryId;
        message.proposalStatus = ProposalStatus.PROPOSED;
        message.createdAt = now;
        return message;
    }

    /**
     * De xuat doi lich lam viec moi (UC16 muc 9, Round B6), luon bat dau PROPOSED - khac
     * PRICE_PROPOSAL: khong co bang lich su rieng nao o module Booking, gia tri de xuat
     * (proposedTime) nam thang tren dong nay cho toi khi duoc Dong y (luc do
     * BookingFacade.acceptReschedule() moi ghi that vao booking_bookings.scheduled_at).
     */
    public static ChatMessage rescheduleProposal(UUID id, UUID channelId, UUID senderAccountId, String note,
            Instant proposedTime, Instant now) {
        ChatMessage message = new ChatMessage();
        message.id = id;
        message.channelId = channelId;
        message.senderAccountId = senderAccountId;
        message.messageType = ChatMessageType.RESCHEDULE_PROPOSAL;
        message.body = note;
        message.proposedTime = proposedTime;
        message.proposalStatus = ProposalStatus.PROPOSED;
        message.createdAt = now;
        return message;
    }

    /**
     * The "Chi phi phat sinh" moi, tham chieu toi 1 batch da ton tai o module Task (them
     * 2026-10-03) - KHONG luu lai status/items/totalAmount o day (doc tuoi qua
     * TaskFacade.findExtraCostBatch() moi lan hien, xem Javadoc ChatMessageType.EXTRA_COST_BATCH),
     * khac PRICE_PROPOSAL (co proposalStatus rieng vi khong co bang nao khac giu trang thai do).
     */
    public static ChatMessage extraCostBatch(UUID id, UUID channelId, UUID senderAccountId,
            UUID refExtraCostBatchId, Instant now) {
        ChatMessage message = new ChatMessage();
        message.id = id;
        message.channelId = channelId;
        message.senderAccountId = senderAccountId;
        message.messageType = ChatMessageType.EXTRA_COST_BATCH;
        message.refExtraCostBatchId = refExtraCostBatchId;
        message.createdAt = now;
        return message;
    }

    /** Danh dau de xuat (PRICE_PROPOSAL/RESCHEDULE_PROPOSAL) da duoc Dong y - khong sua gi khac tren dong nay. */
    public void markProposalAccepted() {
        this.proposalStatus = ProposalStatus.ACCEPTED;
    }

    /**
     * Danh dau de xuat bi Tu choi HOAC duoc chinh nguoi tao Thu hoi - CUNG 1 gia tri REJECTED,
     * chi khac nhau o noi dung SYSTEM message rieng sinh kem theo (dac ta muc 3).
     */
    public void markProposalRejected() {
        this.proposalStatus = ProposalStatus.REJECTED;
    }

    /**
     * Danh dau tin nhan nay da bi chinh nguoi gui thu hoi (them 2026-09-26) - chi goi sau khi
     * ChatService da kiem tra dieu kien (dung nguoi gui, dung loai, con trong han 15 phut).
     * KHONG xoa body/attachments that su, chi danh dau - tang tra ve (toResponse) chiu trach
     * nhiem an noi dung khi recalledAt khac null.
     */
    public void markRecalled(Instant now) {
        this.recalledAt = now;
    }

    /** Tin nhan nay da bi thu hoi chua (them 2026-09-26). */
    public boolean isRecalled() {
        return recalledAt != null;
    }

    /** Id noi bo cua tin nhan/de xuat nay. */
    public UUID getId() {
        return id;
    }

    /** Id kenh chat chua tin nhan nay. */
    public UUID getChannelId() {
        return channelId;
    }

    /** Id tai khoan gui - null neu messageType = SYSTEM. */
    public UUID getSenderAccountId() {
        return senderAccountId;
    }

    /** Loai tin nhan/de xuat. */
    public ChatMessageType getMessageType() {
        return messageType;
    }

    /** Noi dung TEXT/SYSTEM, hoac ghi chu kem theo de xuat gia/doi lich. */
    public String getBody() {
        return body;
    }

    /** Id dong task_price_history tuong ung - chi co khi messageType = PRICE_PROPOSAL. */
    public UUID getRefPriceHistoryId() {
        return refPriceHistoryId;
    }

    /** Id dong task_edit_events tuong ung - chi co khi tin nay la SYSTEM sinh tu UC07. */
    public UUID getRefTaskEditId() {
        return refTaskEditId;
    }

    /** Id dong task_extra_cost_batches tuong ung - chi co khi messageType = EXTRA_COST_BATCH. */
    public UUID getRefExtraCostBatchId() {
        return refExtraCostBatchId;
    }

    /** Thoi gian de xuat doi sang - chi co khi messageType = RESCHEDULE_PROPOSAL. */
    public Instant getProposedTime() {
        return proposedTime;
    }

    /** Trang thai xu ly de xuat - ap dung cho PRICE_PROPOSAL va RESCHEDULE_PROPOSAL, null cho TEXT/SYSTEM. */
    public ProposalStatus getProposalStatus() {
        return proposalStatus;
    }

    /** Thoi diem gui, khong doi sau do. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Thoi diem bi thu hoi - null neu chua tung bi thu hoi (them 2026-09-26). */
    public Instant getRecalledAt() {
        return recalledAt;
    }

    /** Id tin nhan dang tra loi (quote) - null neu khong phai tra loi (them 2026-09-26). */
    public UUID getReplyToMessageId() {
        return replyToMessageId;
    }
}
