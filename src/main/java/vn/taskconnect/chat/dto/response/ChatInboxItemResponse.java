package vn.taskconnect.chat.dto.response;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.booking.api.PaymentMethod;
import vn.taskconnect.chat.api.ChannelStatus;
import vn.taskconnect.task.api.TaskApplicationStatus;
import vn.taskconnect.task.api.TaskStatus;

/**
 * Mot dong trong Inbox (dac ta muc 10) - viewerRole tinh dong theo tung channel (so sanh
 * accountId dang xem voi posterId/taskerId), khong theo "che do" dang bat cua ca app.
 * applicationStatus them de FE hien badge ly do dong kenh cu the (WITHDRAWN/DECLINED/REJECTED/
 * REJECTED_AUTO/INVITE_EXPIRED...) thay vi 1 nhan "Da dong" chung chung khi status=CLOSED -
 * khong dung khi channel con OPEN (chi la du lieu tham khao, khong anh huong logic 4 tab hien
 * co, van chi dua tren ChannelStatus/needsResponse nhu truoc). hasBooking them 2026-09-22 (tu
 * channel.bookingId != null - gan boi ChatService.attachBooking() dung luc UC11 confirm) de tab
 * IN_PROGRESS phan biet dung "da duoc chon/da co booking" voi "dang OPEN cho doi ben kia tra
 * loi, chua ai chon ai" - xem Javadoc InboxTab.java va matchesTab() trong ChatService.java.
 * taskStatus them de FE phan biet duoc kenh dong vi Admin tu choi task cha (hau kiem) voi cac ly
 * do dong khac von chi suy tu applicationStatus - xem Javadoc TaskApplicationParties.taskStatus.
 * paymentMethod them 2026-10-02 (doc qua BookingFacade khi hasBooking = true, null neu chua co
 * booking) de FE quyet dinh hien nut "Chi phi phat sinh" thay "De xuat gia" (chi khi ASSIGNED +
 * FULL_ESCROW, xem Javadoc TaskExtraCostService).
 */
public record ChatInboxItemResponse(
        UUID applicationId,
        UUID channelId,
        UUID taskId,
        String taskTitle,
        UUID counterpartAccountId,
        String counterpartName,
        String counterpartAvatarUrl,
        String viewerRole,
        ChannelStatus status,
        String lastMessagePreview,
        Instant lastMessageAt,
        boolean needsResponse,
        TaskApplicationStatus applicationStatus,
        boolean hasBooking,
        TaskStatus taskStatus,
        PaymentMethod paymentMethod
) {
}
