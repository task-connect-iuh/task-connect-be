package vn.taskconnect.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cau hinh JWT, doc tu {@code app.jwt.*} trong application.yml (anh xa tu bien moi
 * truong JWT_SECRET / JWT_ACCESS_TOKEN_TTL_MINUTES / JWT_REFRESH_TOKEN_TTL_DAYS /
 * JWT_REFRESH_COOKIE_SECURE qua .env).
 *
 * @param refreshCookieSecure thuoc tinh Secure cua cookie refresh token (chi gui qua
 *                            HTTPS). Mac dinh false de dev chay duoc tren http://localhost
 *                            khong co HTTPS - BAT BUOC set true khi len production.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessTokenTtlMinutes,
        long refreshTokenTtlDays,
        boolean refreshCookieSecure
) {
}
