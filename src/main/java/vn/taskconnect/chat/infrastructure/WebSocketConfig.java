package vn.taskconnect.chat.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import vn.taskconnect.security.CorsProperties;

/**
 * Ha tang WebSocket/STOMP cho Chat thoi gian thuc (UC16). Broker don gian trong-tien-trinh
 * (enableSimpleBroker) - khong dung RabbitMQ STOMP relay, dung quy mo do an tot nghiep khong
 * can broker ngoai (xem docs/PROGRESS-CHAT-MODULE.md Round B1). Client chi SUBSCRIBE (nhan tin
 * moi/cap nhat de xuat), khong SEND qua STOMP - gui tin nhan/de xuat van qua REST endpoint
 * (Round B2 tro di), ChatFacade tu publish len broker sau khi ghi DB thanh cong.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor authChannelInterceptor;
    private final CorsProperties corsProperties;

    public WebSocketConfig(StompAuthChannelInterceptor authChannelInterceptor, CorsProperties corsProperties) {
        this.authChannelInterceptor = authChannelInterceptor;
        this.corsProperties = corsProperties;
    }

    /** Dang ky endpoint bat tay STOMP tai /ws - cung danh sach origin voi CORS REST (SecurityConfig), khong wildcard. */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(corsProperties.allowedOrigins().toArray(new String[0]));
    }

    /**
     * /topic cho kenh chat (broadcast toi moi nguoi dang subscribe dung applicationId), /queue
     * (qua tien to /user, xem setUserDestinationPrefix) cho badge Inbox rieng tung tai khoan.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    /** Gan StompAuthChannelInterceptor vao kenh inbound de xac thuc JWT ngay luc CONNECT. */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }
}
