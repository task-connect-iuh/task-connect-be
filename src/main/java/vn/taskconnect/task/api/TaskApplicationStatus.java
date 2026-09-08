package vn.taskconnect.task.api;

/**
 * Trang thai vong doi mot don ung tuyen (UC10/UC11), dung theo state machine da chot trong
 * .claude/rules/01-domain-glossary.md: PENDING -> ACCEPTED | REJECTED | NEEDS_RECONFIRM.
 * NEEDS_RECONFIRM la trang thai cac don PENDING con lai cua cung 1 task tu dong chuyen sang
 * khi Poster xac nhan mot ung vien khac (OQ-08: 1 Tasker/task).
 */
public enum TaskApplicationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    NEEDS_RECONFIRM
}
