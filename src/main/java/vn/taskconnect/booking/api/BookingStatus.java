package vn.taskconnect.booking.api;

/**
 * Trang thai booking-lite. Tu truoc chi co dung 1 gia tri PENDING_ESCROW (module Payment/escrow
 * that chua ton tai) - tu 2026-10-02 (quyet dinh nguoi dung) module Payment da co ban ghi tam giu
 * that (gia lap, xem payment.api.EscrowHoldStatus), nen GUARDRAIL 2 CLAUDE.md ("khong booking nao
 * CONFIRMED neu chua giu tien thanh cong") thoa man duoc that su: BookingFacadeImpl.
 * createFromApplication() CHI set CONFIRMED SAU KHI PaymentFacade.holdInitial() tra ve thanh cong,
 * khong con nhanh nao tao booking CONFIRMED ma thieu buoc nay. KHONG them COMPLETED/CANCELLED -
 * giai ngan/huy booking that van ngoai pham vi dot nay, se mo rong tiep bang 1 thay doi rieng kem
 * migration fix-forward khi lam.
 */
public enum BookingStatus {
    /** Chua con duong nao tao duoc trang thai nay nua tu 2026-10-02 - giu lai gia tri cu de tuong thich cac booking da tao truoc do. */
    PENDING_ESCROW,
    /** Da giu tien thanh cong (xem V49) - trang thai duy nhat BookingFacadeImpl.createFromApplication() tao ra tu 2026-10-02. */
    CONFIRMED
}
