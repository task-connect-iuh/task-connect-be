package vn.taskconnect.task.api;

/**
 * Trang thai vong doi mot don ung tuyen (UC09/UC10/UC11/UC16), mo rong theo
 * TaskConnect_Chat_ImplementationSpec.md muc 1 va 5 (docs/PROGRESS-CHAT-MODULE.md).
 * PENDING, INQUIRING, INVITED, WITHDRAWN, REJECTED_AUTO, DECLINED, INVITE_EXPIRED la 7 gia
 * tri dang dung theo dac ta. ACCEPTED va NEEDS_RECONFIRM la gia tri CU (state machine truoc
 * khi co dac ta Chat) - giu lai trong DB/enum de tuong thich nguoc, nhung tu Round B4 tro di
 * (xem TaskApplicationService) chi con ACCEPTED duoc dung tiep - lam nhan danh dau rieng cho
 * nut "Tu choi" thu cong hop voi REJECTED (quyet dinh giu ngoai dac ta), con NEEDS_RECONFIRM
 * khong con code path nao set nua (REJECTED_AUTO thay the hoan toan cho cascade UC11).
 * REJECTED cung la gia tri CU, duoc GIU LAI theo quyet dinh cua nguoi dung (khac REJECTED_AUTO
 * - REJECTED la Poster chu dong tu choi 1 ung vien khi task con mo, REJECTED_AUTO la he thong
 * tu dong tu choi cac ung vien con lai khi UC11 chon xong nguoi thang).
 * CANCELLED va TIME_CHANGED_NEEDS_RECONFIRM la 2 gia tri moi cho UC07 (Poster huy/sua viec da
 * dang, xem V44__extend_task_application_status_for_cancel_and_time_change.sql) - them moi
 * hoan toan, KHONG tai su dung NEEDS_RECONFIRM du ten nghe tuong tu, vi gia tri do da la "chet"
 * va mang y nghia lich su khac (cascade UC11 truoc khi co REJECTED_AUTO).
 */
public enum TaskApplicationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    NEEDS_RECONFIRM,
    INQUIRING,
    INVITED,
    WITHDRAWN,
    REJECTED_AUTO,
    DECLINED,
    INVITE_EXPIRED,
    /** Poster huy ca cong viec khi con OPEN/PENDING_REVIEW (UC07) - cong viec khong con ton tai. */
    CANCELLED,
    /** Poster doi scheduledAt luc don dang PENDING (UC07 Tier 3) - Tasker phai xac nhan lai hoac rut. */
    TIME_CHANGED_NEEDS_RECONFIRM
}
