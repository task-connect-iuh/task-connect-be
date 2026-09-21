package vn.taskconnect.task.api;

/**
 * Ly do mot cong viec bi gan co hau kiem cho Admin sau khi AI phan loai category luc dang
 * viec (xem .claude/rules/15-ai-module.md). Gan co KHONG chan luong dang viec - task van
 * chuyen OPEN ngay, Admin chi xem lai sau (hau kiem hoan toan, khong tien kiem).
 */
public enum TaskAiFlagReason {

    /** AI tra ve OTHER - mo ta khong khop ro danh muc nao trong 5 nhom dich vu. */
    OTHER_CATEGORY,

    /** AI phat hien dau hieu yeu cau viec nguy hiem/trai phep ngoai pham vi dien-nuoc dan dung. */
    SUSPICIOUS_UNSAFE,

    /** AI phat hien dau hieu spam/tai khoan rac (mo ta rong, lap tu khoa, gia bat thuong). */
    SUSPICIOUS_SPAM,

    /** AI phat hien ngon tu phan cam/quay roi/phan biet doi xu trong mo ta. */
    SUSPICIOUS_HARASSMENT,

    /** AI khong phan loai duoc (het quota, loi mang, loi parse) - xem AiFacade.classifyTaskCategory(). */
    CLASSIFICATION_FAILED
}
