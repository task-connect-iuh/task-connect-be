package vn.taskconnect.chat.api;

/**
 * 4 tab Inbox theo TaskConnect_Chat_ImplementationSpec.md muc 10: Tat ca / Can phan hoi / Dang
 * thuc hien / Da dong. IN_PROGRESS CHOT 2026-09-22 la "kenh OPEN va da co booking" (application
 * da duoc Poster chon o UC11, xem ChatChannel.attachBooking()) - dinh nghia tam cua Round B2
 * ("OPEN nhung khong can phan hoi ngay") bi bo vi khien 1 kenh dang thuong luong, chi vua tra
 * loi xong va dang cho doi phuong, nhay nham vao day. Kenh OPEN, khong can phan hoi, CHUA co
 * booking gio khong khop tab rieng nao (chi con o tab ALL) - xem docs/PROGRESS-CHAT-MODULE.md.
 */
public enum InboxTab {
    ALL,
    NEEDS_RESPONSE,
    IN_PROGRESS,
    CLOSED
}
