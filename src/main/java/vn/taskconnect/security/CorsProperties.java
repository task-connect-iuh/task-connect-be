package vn.taskconnect.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Danh sach origin duoc phep goi API kem cookie, doc tu {@code app.cors.allowed-origins}
 * trong application.yml (anh xa tu bien moi truong APP_CORS_ALLOWED_ORIGINS qua .env).
 * Phai la origin cu the (khong duoc "*") vi CORS voi allowCredentials=true bi Fetch spec
 * cam dung wildcard.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
