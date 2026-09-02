package vn.taskconnect.user.api;

/**
 * Trang thai xac minh cua mot ho so ky nang Tasker cho mot nhom dich vu cu the. State
 * machine chot trong {@code .claude/rules/01-domain-glossary.md}:
 * PENDING -> VERIFIED | REJECTED. Chuyen VERIFIED khi MOT chung chi hop le (quan he OR,
 * xem V5__seed_user_certificate_types.sql) cua cung category duoc Admin duyet - khong phai
 * do Admin duyet truc tiep hop so ky nang. Module Matching (sau nay) chi lay ban ghi
 * VERIFIED khi ghep viec, dung nhu comment cua user_tasker_skill_profiles trong V2 migration.
 */
public enum SkillVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
