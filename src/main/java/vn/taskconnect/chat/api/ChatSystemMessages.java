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

    /** Dinh dang tien theo .claude/rules/22-vietnamese-copy.md: dau cham ngan nghin, ky hieu ₫ sau co khoang trang. */
    private static String formatVnd(long amount) {
        return String.format("%,d", amount).replace(',', '.') + " ₫";
    }
}
