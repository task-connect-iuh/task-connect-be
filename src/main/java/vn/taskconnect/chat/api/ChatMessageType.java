package vn.taskconnect.chat.api;

/**
 * Loai 1 tin nhan/de xuat trong kenh chat. Xem TaskConnect_Chat_ImplementationSpec.md muc 1.
 * PRICE_PROPOSAL va RESCHEDULE_PROPOSAL luon di kem proposalStatus, SYSTEM khong co
 * senderAccountId, TEXT la tin nhan tu do khong duoc he thong doi chieu.
 */
public enum ChatMessageType {
    TEXT,
    SYSTEM,
    PRICE_PROPOSAL,
    RESCHEDULE_PROPOSAL
}
