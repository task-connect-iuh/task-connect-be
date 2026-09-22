package vn.taskconnect.task.api;

/**
 * Tinh trang vat tu Poster da chuan bi cho cong viec luc dang tin, xem
 * V26__add_supplies_fields_to_task_tasks.sql. FULL/PARTIAL nghia la da co it nhieu vat tu,
 * UNKNOWN nghia la chua ro, can Tasker tu van truoc. Bat buoc chon 1 trong 3 luc dang viec
 * (khong co gia tri mac dinh o FE) - xem CreateTaskRequest.suppliesStatus.
 */
public enum SuppliesStatus {
    FULL,
    PARTIAL,
    UNKNOWN
}
