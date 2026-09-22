package vn.taskconnect.task.api;

/**
 * Loai thay doi gia trong 1 dong task_price_history - xac dinh tu dong theo task.status tai
 * thoi diem tao de xuat (dac ta muc 3): INITIAL_AGREEMENT truoc khi task ASSIGNED,
 * SCOPE_CHANGE sau khi da ASSIGNED (chua COMPLETED).
 */
public enum ChangeType {
    INITIAL_AGREEMENT,
    SCOPE_CHANGE
}
