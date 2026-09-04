package vn.taskconnect.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cau hinh dang nhap Google, doc tu {@code app.google.*} trong application.yml (anh xa tu
 * bien moi truong GOOGLE_CLIENT_ID qua .env).
 *
 * @param clientId Client ID cua OAuth 2.0 Client (loai Web application) tao trong Google
 *                 Cloud Console - GoogleIdTokenVerifier dung gia tri nay lam audience bat
 *                 buoc khop khi verify ID token, xem security/google/GoogleTokenVerifierService.java.
 */
@ConfigurationProperties(prefix = "app.google")
public record GoogleProperties(String clientId) {
}
