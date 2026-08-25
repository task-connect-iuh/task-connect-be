package vn.taskconnect.auth.api;

/**
 * Trang thai tai khoan. State machine chot trong
 * {@code .claude/rules/01-domain-glossary.md}: UNVERIFIED -> ACTIVE -> LOCKED | SUSPENDED.
 */
public enum AccountStatus {
    UNVERIFIED,
    ACTIVE,
    LOCKED,
    SUSPENDED
}
