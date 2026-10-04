package vn.taskconnect.task.api;

/**
 * Cac truong Poster sua duoc sau khi da dang viec (UC07), dung de bao "field nao bi khoa" trong
 * loi FIELD_LOCKED_HAS_APPLICANTS va "field nao vua doi" trong TaskEditSummary (nut "Xem chi
 * tiet thay doi" ben chat). title/description/anh/addressText/lat/lng/categoryId/
 * estimatedWorkersNeeded khoa cung vinh vien - KHONG nam trong enum nay.
 */
public enum TaskEditableField {
    LOCATION_TYPE,
    ARRIVAL_NOTES,
    BUDGET_AMOUNT,
    SUPPLIES_STATUS,
    SUPPLIES_NOTE,
    SCHEDULED_AT
}
