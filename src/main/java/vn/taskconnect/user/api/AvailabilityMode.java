package vn.taskconnect.user.api;

/**
 * Che do lich lam viec Tasker tu khai bao: FLEXIBLE (gio linh hoat, nhan viec bat cu luc
 * nao, khong khai bao khung gio cu the) hoac CUSTOM (tu chon khung gio ranh cu the trong
 * tuan, xem TaskerAvailability). Null (chua khai bao) duoc FE xu ly nhu CUSTOM de khong doi
 * hanh vi cua tai khoan cu. Xem V43__add_availability_mode_to_user_profiles.sql.
 */
public enum AvailabilityMode {
    FLEXIBLE,
    CUSTOM
}
