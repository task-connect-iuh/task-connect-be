package vn.taskconnect.chat.api;

/**
 * Trang thai xu ly 1 de xuat (PRICE_PROPOSAL hoac RESCHEDULE_PROPOSAL). Xem
 * TaskConnect_Chat_ImplementationSpec.md muc 3 - moi application toi da 1 de xuat o PROPOSED
 * tai 1 thoi diem, khong co co che tu dong thay the (supersede) de xuat cu.
 */
public enum ProposalStatus {
    PROPOSED,
    ACCEPTED,
    REJECTED
}
