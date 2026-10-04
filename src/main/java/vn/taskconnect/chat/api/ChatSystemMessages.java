package vn.taskconnect.chat.api;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Noi dung SYSTEM message chuan hoa, dat mot noi duy nhat de moi module (Task goi khi mo/dong
 * kenh, Chat tu dung khi lazy-create) dung chung, tranh sai lech copy. Cac chuoi lay dung
 * nguyen van dac ta TaskConnect_Chat_ImplementationSpec.md muc 2 va 5;
 * posterRejectedManually() va TASK_ASSIGNED_TO_ANOTHER la phan MO RONG ngoai dac ta (nut Tu
 * choi thu cong giu lai theo yeu cau nguoi dung, xem docs/PROGRESS-CHAT-MODULE.md) - can duyet
 * lai theo .claude/rules/22-vietnamese-copy.md truoc khi FE dung that.
 */
public final class ChatSystemMessages {

    /** Cascade tu dong khi UC11 chon xong 1 ung vien (Round B4) - dung lai dung chuoi da co o ErrorCode.TASK_ALREADY_ASSIGNED. */
    public static final String TASK_ASSIGNED_TO_ANOTHER = "Công việc đã được giao cho Tasker khác.";

    /**
     * SYSTEM message khi Admin tu choi mot cong viec dang hau kiem (TaskService.rejectFlaggedTask,
     * chuyen Task.status sang REJECTED) - gui vao TAT CA kenh con OPEN cua cac don ung tuyen
     * thuoc task nay kem dong kenh (xem TaskService.rejectFlaggedTask), MO RONG ngoai dac ta Chat
     * goc (tinh nang hau kiem thuoc module Task, theo yeu cau nguoi dung).
     */
    public static final String TASK_REJECTED_BY_ADMIN = "Quản trị viên đã huỷ bỏ công việc này.";

    /**
     * SYSTEM message vao kenh cua ung vien THANG khi Poster bam "Chon nguoi nay" (UC11) - dac ta
     * khong cho san chuoi chinh xac cho truong hop nay (chi co san TASK_ASSIGNED_TO_ANOTHER cho
     * cac ung vien thua), suy luan theo van phong cac message khac. Them 2026-09-21 theo bao cao
     * nguoi dung: truoc day kenh cua ung vien thang khong nhan duoc thong bao gi ca khi duoc
     * chon, khong doi xung voi cac ung vien thua (xem docs/PROGRESS-CHAT-MODULE.md).
     */
    public static String taskerConfirmed(String posterName) {
        return posterName + " đã chọn bạn cho công việc này.";
    }

    /** SYSTEM message khi tien trinh nen tu dong danh dau 1 loi moi INVITED qua han (Round B5, dac ta muc 8). */
    public static final String INVITE_EXPIRED = "Lời mời đã hết hạn do không có phản hồi.";

    /**
     * SYSTEM message khi Poster tu huy cong viec luc con OPEN/PENDING_REVIEW (UC07) - gui vao
     * TAT CA kenh con OPEN cua cac don ung tuyen thuoc task nay kem dong kenh (xem
     * TaskService.cancelTask), MO RONG ngoai dac ta Chat goc giong TASK_REJECTED_BY_ADMIN.
     */
    public static final String TASK_CANCELLED_BY_POSTER = "Công việc đã bị huỷ.";

    /**
     * SYSTEM message khi Poster sua thanh cong vat tu (suppliesStatus/suppliesNote) HOAC
     * scheduledAt cua cong viec (UC07) - CHI gui vao kenh dang INQUIRING/INVITED, KHONG dong
     * kenh, kem ref_task_edit_id de FE hien nut "Xem chi tiết thay đổi" (xem TaskService.
     * updateTask). budgetAmount KHONG bao gio roi vao nhanh nay - bi khoa (BROAD_LOCK_FIELDS) tu
     * truoc khi toi duoc day neu con INQUIRING/INVITED.
     */
    public static final String TASK_UPDATED_BY_POSTER = "Poster vừa cập nhật thông tin công việc";

    /**
     * SYSTEM message vao kenh cua 1 don dang PENDING khi Poster doi "Thoi gian mong muon" (UC07
     * Tier 3) - don do chuyen sang TIME_CHANGED_NEEDS_RECONFIRM, can Tasker xac nhan lai hoac rut.
     */
    public static final String TASK_TIME_CHANGED_NEEDS_RECONFIRM =
            "Poster vừa đổi thời gian mong muốn. Hãy xác nhận lại nếu bạn vẫn nhận việc này.";

    /** SYSTEM message khi Tasker bam "Van nhan viec" sau khi Poster doi thoi gian mong muon (UC07 Tier 3). */
    public static String taskerReconfirmedAfterTimeChange(String taskerName) {
        return taskerName + " đã xác nhận vẫn nhận công việc này.";
    }

    /**
     * Dinh dang ngay gio theo .claude/rules/22-vietnamese-copy.md ("09/08 · 10:20", khong nam,
     * dau cham giua ngay va gio), gio Viet Nam (UTC+7) - du an chi phuc vu nguoi dung trong
     * nuoc, khong can xu ly da mui gio.
     */
    private static final DateTimeFormatter VN_DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM · HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private ChatSystemMessages() {
        // Chi chua static method, khong tao instance.
    }

    /** SYSTEM message mo kenh khi Tasker ung tuyen thang (status PENDING, dac ta muc 2). */
    public static String taskerApplied(String taskerName) {
        return taskerName + " đã ứng tuyển vào công việc này.";
    }

    /** SYSTEM message mo kenh khi Tasker bam "Nhan tin hoi them" (status INQUIRING, dac ta muc 2). */
    public static String taskerInquiring(String taskerName) {
        return taskerName + " muốn tìm hiểu thêm về công việc này.";
    }

    /**
     * SYSTEM message khi Tasker bam "Ung tuyen" tu 1 don dang INQUIRING de chuyen thang thanh
     * PENDING - MO RONG ngoai dac ta muc 3 (ban dau chi nang cap qua acceptPriceProposal),
     * theo yeu cau nguoi dung 2026-09-21 (xem PROGRESS-TASK-TASKER-MODULE.md). Gui vao kenh
     * chat da mo san cua don hoi them, KHONG dong kenh.
     */
    public static String taskerAppliedFromInquiry(String taskerName) {
        return taskerName + " đã chính thức ứng tuyển vào công việc này.";
    }

    /** SYSTEM message mo kenh khi Poster moi truc tiep (status INVITED, dac ta muc 2, dung tu Round B5). */
    public static String posterInvited(String posterName) {
        return posterName + " mời bạn nhận công việc này.";
    }

    /**
     * SYSTEM message mo kenh khi Tasker ung tuyen kem "De nghi mot muc khac" (status PENDING,
     * yeu cau nguoi dung 2026-09-30) - dung cung mau voi posterInvited(), khac o cho ben mo kenh
     * la Tasker (nguoi ung tuyen) thay vi Poster (nguoi moi).
     */
    public static String taskerAppliedWithPriceProposal(String taskerName) {
        return taskerName + " đã ứng tuyển và đề nghị một mức giá khác.";
    }

    /** SYSTEM message khi Tasker rut mot don dang PENDING (dac ta muc 5 bang o cuoi). */
    public static String taskerWithdrewApplication(String taskerName) {
        return taskerName + " đã rút ứng tuyển.";
    }

    /** SYSTEM message khi Tasker dung mot don dang INQUIRING (dac ta muc 5 bang o cuoi). */
    public static String taskerStoppedInquiring(String taskerName) {
        return taskerName + " không tiếp tục tìm hiểu công việc này.";
    }

    /** SYSTEM message khi Poster tu choi thu cong 1 ung vien - NGOAI dac ta, xem Javadoc class. */
    public static String posterRejectedManually(String posterName) {
        return posterName + " đã từ chối ứng viên này.";
    }

    /** SYSTEM message khi Tasker tu choi mot loi moi truc tiep (INVITED, Round B5, dac ta muc 5/8). */
    public static String taskerDeclinedInvite(String taskerName) {
        return taskerName + " đã từ chối lời mời này.";
    }

    /**
     * SYSTEM message khi 1 de xuat gia duoc Dong y (dac ta muc 3 khong cho san chuoi chinh
     * xac - suy luan theo van phong cac message khac, xem docs/PROGRESS-CHAT-MODULE.md).
     */
    public static String priceProposalAccepted(String accepterName, long amount) {
        return accepterName + " đã đồng ý mức giá " + formatVnd(amount) + ".";
    }

    /** SYSTEM message khi chinh nguoi tao Thu hoi de xuat gia con PROPOSED (dac ta muc 3). */
    public static String priceProposalWithdrawn(String proposerName) {
        return proposerName + " đã thu hồi đề xuất giá.";
    }

    /**
     * SYSTEM message khi ben con lai Tu choi de xuat gia con PROPOSED - PHAN BIET voi
     * priceProposalWithdrawn(), dung tu "đã bị từ chối" nhu dac ta muc 3 nhac toi, du dac ta
     * khong mo ta rieng nut Tu choi nay nhu mot bullet doc lap (suy luan, xem
     * docs/PROGRESS-CHAT-MODULE.md).
     */
    public static String priceProposalRejected(String rejecterName) {
        return rejecterName + " đã từ chối đề xuất giá.";
    }

    /**
     * SYSTEM message khi 1 de xuat doi lich duoc Dong y (Round B6, dac ta muc 9 khong cho san
     * chuoi chinh xac - suy luan theo van phong cac message xu ly de xuat khac, xem
     * docs/PROGRESS-CHAT-MODULE.md).
     */
    public static String rescheduleProposalAccepted(String accepterName, Instant newScheduledAt) {
        return accepterName + " đã đồng ý đổi lịch sang " + VN_DATETIME_FORMAT.format(newScheduledAt) + ".";
    }

    /** SYSTEM message khi chinh nguoi tao Thu hoi de xuat doi lich con PROPOSED (dac ta muc 9, cung co che voi price proposal). */
    public static String rescheduleProposalWithdrawn(String proposerName) {
        return proposerName + " đã thu hồi đề xuất đổi lịch.";
    }

    /** SYSTEM message khi ben con lai Tu choi de xuat doi lich con PROPOSED - phan biet voi rescheduleProposalWithdrawn(). */
    public static String rescheduleProposalRejected(String rejecterName) {
        return rejecterName + " đã từ chối đề xuất đổi lịch.";
    }

    /** SYSTEM message khi Poster dong y 1 batch chi phi phat sinh dang PENDING. */
    public static String extraCostApproved(String posterName, long totalAmount) {
        return posterName + " đã đồng ý khoản chi phí phát sinh " + formatVnd(totalAmount) + ".";
    }

    /** SYSTEM message khi Poster tu choi 1 batch chi phi phat sinh dang PENDING. */
    public static String extraCostRejected(String posterName) {
        return posterName + " đã từ chối khoản chi phí phát sinh.";
    }

    /** SYSTEM message khi Tasker tu thu hoi 1 batch chi phi phat sinh do chinh minh dang, dang PENDING. */
    public static String extraCostWithdrawn(String taskerName) {
        return taskerName + " đã thu hồi khoản chi phí phát sinh.";
    }

    /** Dinh dang tien theo .claude/rules/22-vietnamese-copy.md: dau cham ngan nghin, ky hieu ₫ sau co khoang trang. */
    private static String formatVnd(long amount) {
        return String.format("%,d", amount).replace(',', '.') + " ₫";
    }
}
