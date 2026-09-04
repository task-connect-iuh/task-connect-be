package vn.taskconnect.user.api;

/**
 * Trang thai KYC cua ho so ca nhan. State machine chot trong
 * {@code .claude/rules/01-domain-glossary.md}: VERIFYING -> VERIFIED | REJECTED | CANCELLED,
 * cong them NOT_SUBMITTED lam trang thai khoi tao truoc khi nguoi dung nop ho so KYC (Buoc 4).
 * CANCELLED: chinh chu tu huy lan nop cua minh khi con dang VERIFYING (khac REJECTED - do
 * Admin quyet dinh), xem KycVerificationService.cancel().
 */
public enum KycStatus {
    NOT_SUBMITTED,
    VERIFYING,
    VERIFIED,
    REJECTED,
    CANCELLED
}
