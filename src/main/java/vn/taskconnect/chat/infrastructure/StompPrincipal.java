package vn.taskconnect.chat.infrastructure;

import java.security.Principal;
import java.util.UUID;

/**
 * Adapter cho accountId (UUID) thanh java.security.Principal - Spring STOMP can Principal that
 * de dinh tuyen tin nhan rieng cho tung nguoi dung (convertAndSendToUser, "/user/queue/...").
 * getName() tra ve chuoi accountId, dung khop voi tham so goi convertAndSendToUser o ChatFacade.
 */
public record StompPrincipal(UUID accountId) implements Principal {

    /** Ten dinh danh Principal - chinh la accountId dang chuoi. */
    @Override
    public String getName() {
        return accountId.toString();
    }
}
