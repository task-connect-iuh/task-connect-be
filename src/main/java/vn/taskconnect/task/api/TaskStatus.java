package vn.taskconnect.task.api;

/**
 * Trang thai vong doi cong viec, dung theo state machine da chot trong
 * .claude/rules/01-domain-glossary.md: PENDING_REVIEW -> OPEN -> ASSIGNED -> COMPLETED ->
 * CLOSED, nhanh thoat CANCELLED (tu PENDING_REVIEW hoac OPEN) hoac REJECTED (Admin tu choi).
 * Dot 1 (dang viec toi gian) chi tao Task o thang OPEN ngay (xem Task.createOpen()), khong
 * qua PENDING_REVIEW/duyet AI hay Admin - quyet dinh da chot voi nguoi dung cho pham vi dot
 * nay, xem docs/PROGRESS-TASK-POSTER-MODULE.md. Van giu du 7 gia tri de khop CHECK constraint
 * cua V18__create_task_tables.sql va khong phai sua schema khi dot sau them chuyen trang thai.
 */
public enum TaskStatus {
    PENDING_REVIEW,
    OPEN,
    ASSIGNED,
    COMPLETED,
    CLOSED,
    CANCELLED,
    REJECTED
}
