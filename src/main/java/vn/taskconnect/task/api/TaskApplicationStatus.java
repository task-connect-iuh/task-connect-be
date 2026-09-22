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
    INVITE_EXPIRED
}
