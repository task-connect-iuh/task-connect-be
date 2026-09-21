package vn.taskconnect.matching.api;

/**
 * Trang thai vong doi mot loi moi Poster gui truc tiep cho Tasker (luong "Poster moi
 * Tasker" - song song voi luong pull TaskApplication/UC10, xem
 * task-connect-claude plan da duyet "AI Tasker Suggestion"). Mirror
 * {@link vn.taskconnect.task.api.TaskApplicationStatus} nhung them EXPIRED thay cho
 * NEEDS_RECONFIRM: PENDING -> ACCEPTED | DECLINED | EXPIRED. EXPIRED danh cho job don dep
 * dinh ky (chua hien thuc dot nay) khi Tasker khong phan hoi sau N ngay.
 */
public enum TaskerInviteStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED
}
