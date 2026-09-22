package vn.taskconnect.chat.dto.response;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.chat.api.ChannelStatus;
import vn.taskconnect.task.api.TaskApplicationStatus;

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
        boolean hasBooking
) {
}
