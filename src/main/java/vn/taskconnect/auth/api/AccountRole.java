package vn.taskconnect.auth.api;

/**
 * Vai tro tai khoan. Mot tai khoan mang duoc dong thoi TASK_POSTER va TASKER.
 * ADMIN la vai tro tach biet, khong tu dang ky qua API cong khai.
 */
public enum AccountRole {
    TASK_POSTER,
    TASKER,
    ADMIN
}
