package vn.taskconnect.chat.infrastructure;

import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import vn.taskconnect.security.jwt.AuthenticatedPrincipal;
import vn.taskconnect.security.jwt.JwtTokenProvider;
import vn.taskconnect.task.api.TaskFacade;
import vn.taskconnect.task.api.dto.TaskApplicationParties;

/**
 * Xac thuc JWT tai buoc CONNECT cua STOMP, va kiem tra quyen tai buoc SUBSCRIBE cho topic
 * chat theo tung applicationId - tai su dung JwtTokenProvider giong JwtAuthenticationFilter
 * (REST), khong dung co che rieng. Client phai gui access token qua STOMP native header
 * "Authorization: Bearer <token>" trong frame CONNECT (khong dung query param de tranh lo
 * token vao access log). Thieu/sai token, hoac subscribe topic cua application khong phai cua
 * minh: nem MessagingException, Spring gui lai STOMP ERROR frame va dong ket noi/tu choi
 * subscribe - khong co GlobalExceptionHandler kieu REST o day.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String CHAT_TOPIC_PREFIX = "/topic/chat/";

    private final JwtTokenProvider tokenProvider;
    private final TaskFacade taskFacade;

    public StompAuthChannelInterceptor(JwtTokenProvider tokenProvider, TaskFacade taskFacade) {
        this.tokenProvider = tokenProvider;
        this.taskFacade = taskFacade;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // QUAN TRONG: phai dung getAccessor() (lay dung accessor mutable ma StompSubProtocolHandler
        // da tao san khi giai ma frame), khong dung StompHeaderAccessor.wrap(message) - wrap() tao
        // 1 accessor BAN SAO, accessor.setUser() goi tren ban sao khong duoc ghi nguoc lai vao
        // message that su duoc tra ve o cuoi method. Hau qua neu dung wrap(): setUser() luc CONNECT
        // khong co tac dung gi, phien WebSocket khong bao gio gan duoc Principal, moi lan SUBSCRIBE
        // sau do accessor.getUser() luon null -> luon bi tu choi CHT-403-FORBIDDEN_CHANNEL du dung
        // la Poster/Tasker that - phat hien qua smoke test WebSocket thu cong (2026-09-19).
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscribe(accessor);
        }
        return message;
    }

    /** Xac thuc token o frame CONNECT (1 lan/ket noi) - cac frame sau dung lai Principal da gan o day. */
    private void authenticate(StompHeaderAccessor accessor) {
        AuthenticatedPrincipal principal = parseToken(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION));
        if (principal == null) {
            throw new MessagingException("CHT-401-WS_UNAUTHENTICATED: khong xac thuc duoc ket noi WebSocket");
        }
        accessor.setUser(new StompPrincipal(principal.accountId()));
    }

    /**
     * Chi topic dang "/topic/chat/{applicationId}" moi can kiem tra rieng - phai la dung
     * Poster/Tasker cua application do (dac ta muc 11). Topic khac (vd "/user/.../queue/inbox")
     * da tu gioi han dung 1 tai khoan boi co che convertAndSendToUser cua Spring, khong can
     * kiem tra them o day.
     */
    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        UUID applicationId = extractChatApplicationId(accessor.getDestination());
        if (applicationId == null) {
            return;
        }
        UUID accountId = resolveAccountId(accessor.getUser());
        TaskApplicationParties parties = taskFacade.getApplicationParties(applicationId).orElse(null);
        boolean allowed = accountId != null && parties != null
                && (accountId.equals(parties.posterId()) || accountId.equals(parties.taskerId()));
        if (!allowed) {
            throw new MessagingException("CHT-403-FORBIDDEN_CHANNEL: khong co quyen xem kenh nay");
        }
    }

    /** Bore token khoi header "Bearer <token>" va xac thuc qua JwtTokenProvider - null neu thieu/sai. */
    private AuthenticatedPrincipal parseToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return tokenProvider.parse(authorizationHeader.substring("Bearer ".length())).orElse(null);
    }

    /** Lay accountId tu Principal da gan luc CONNECT - null neu vi ly do nao do chua co (khong nen xay ra sau authenticate()). */
    private UUID resolveAccountId(Principal user) {
        return user instanceof StompPrincipal stompPrincipal ? stompPrincipal.accountId() : null;
    }

    /** Tach applicationId tu destination dang "/topic/chat/{uuid}" - null neu khong dung dinh dang nay. */
    private UUID extractChatApplicationId(String destination) {
        if (destination == null || !destination.startsWith(CHAT_TOPIC_PREFIX)) {
            return null;
        }
        try {
            return UUID.fromString(destination.substring(CHAT_TOPIC_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
