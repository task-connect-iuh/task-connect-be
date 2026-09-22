package vn.taskconnect.booking.api;

/**
 * Trang thai booking-lite. CHI CO DUNG 1 GIA TRI - KHONG PHAI THIEU SOT. GUARDRAIL 2 cua
 * CLAUDE.md: khong booking nao duoc CONFIRMED neu chua giu tien thanh cong, khong ngoai le.
 * Module Payment/escrow that chua ton tai (xem docs/PROGRESS-CHAT-MODULE.md muc 0.2), nen
 * chua co code path nao duoc phep tao booking o trang thai khac PENDING_ESCROW. Se mo rong
 * enum nay (them CONFIRMED/COMPLETED/CANCELLED) bang 1 thay doi rieng khi module Payment
 * that duoc hien thuc, kem migration fix-forward noi rong CHECK constraint tuong ung.
 */
public enum BookingStatus {
    PENDING_ESCROW
}
