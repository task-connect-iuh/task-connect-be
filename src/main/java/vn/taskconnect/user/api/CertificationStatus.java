package vn.taskconnect.user.api;

/**
 * Trang thai xet duyet cua mot lan nop chung chi hanh nghe. State machine chot trong
 * {@code .claude/rules/01-domain-glossary.md}: PENDING_REVIEW -> APPROVED | REJECTED |
 * EXPIRED. EXPIRED danh cho chung chi da APPROVED nhung qua expiry_date - chua co job tu
 * dong chuyen trang thai nay o Buoc 6, de ngo cho cong viec sau (lien quan OQ-10
 * docs/OPEN-QUESTIONS.md ve vong doi chung chi het han).
 */
public enum CertificationStatus {
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    EXPIRED
}
