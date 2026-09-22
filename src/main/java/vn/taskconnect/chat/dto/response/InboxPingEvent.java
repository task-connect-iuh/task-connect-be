package vn.taskconnect.chat.dto.response;

import java.util.UUID;

/**
 * Tin hieu nhe bao FE co thay doi lien quan toi 1 application, publish qua WebSocket toi
 * "/user/{accountId}/queue/inbox" - FE nhan duoc thi goi lai GET /api/v1/chat/inbox de lam
 * moi danh sach/badge, khong nhet san du lieu day du de tranh trung lap logic loc tab o server.
 */
public record InboxPingEvent(UUID applicationId) {
}
