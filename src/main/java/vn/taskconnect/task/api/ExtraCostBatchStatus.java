package vn.taskconnect.task.api;

/**
 * Trang thai 1 batch chi phi phat sinh (task_extra_cost_batches) - quyet dinh nguoi dung
 * 2026-10-02. PENDING khi vua dang, Tasker co the Thu hoi (WITHDRAWN); Poster quyet dinh Dong y
 * (APPROVED, cong them vao requiredTotal) hoac Tu choi (REJECTED). Khong co duong quay lai tu 3
 * trang thai ket thuc (APPROVED/REJECTED/WITHDRAWN) ve PENDING.
 */
public enum ExtraCostBatchStatus {
    PENDING,
    APPROVED,
    REJECTED,
    WITHDRAWN
}
