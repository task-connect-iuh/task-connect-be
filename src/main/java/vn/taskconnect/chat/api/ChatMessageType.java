package vn.taskconnect.chat.api;

/**
 * Loai 1 tin nhan/de xuat trong kenh chat. Xem TaskConnect_Chat_ImplementationSpec.md muc 1.
 * PRICE_PROPOSAL va RESCHEDULE_PROPOSAL luon di kem proposalStatus, SYSTEM khong co
 * senderAccountId, TEXT la tin nhan tu do khong duoc he thong doi chieu. IMAGE/FILE/VIDEO/VOICE
 * (them 2026-09-26) la tin nhan dinh kem file (xem chat_message_attachments), body cua no la
 * chu thich (caption) tuy chon, khong bat buoc. EXTRA_COST_BATCH (them 2026-10-03) la the "Chi
 * phi phat sinh" hien truc tiep trong khung chat, cung tinh than voi PRICE_PROPOSAL nhung KHONG
 * co proposalStatus rieng - status/items/totalAmount doc tuoi tu TaskFacade.findExtraCostBatch()
 * qua refExtraCostBatchId moi lan hien (task_extra_cost_batches.status la nguon su that duy
 * nhat, khac PRICE_PROPOSAL khong co bang rieng nao khac giu trang thai).
 */
public enum ChatMessageType {
    TEXT,
    SYSTEM,
    PRICE_PROPOSAL,
    RESCHEDULE_PROPOSAL,
    IMAGE,
    FILE,
    VIDEO,
    VOICE,
    EXTRA_COST_BATCH
}
