package vn.taskconnect.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sinh va xac thuc access token JWT. Refresh token khong phai JWT - la chuoi ngau nhien
 * luu hash trong DB, xem AuthService.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String ROLES_CLAIM = "roles";

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = Duration.ofMinutes(properties.accessTokenTtlMinutes());
    }

    public String generateAccessToken(UUID accountId, Set<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(accountId.toString())
                .claim(ROLES_CLAIM, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    /**
     * Rong neu token khong hop le, sai chu ky, hoac het han - khong nem exception ra
     * ngoai vi day la trang thai binh thuong cua mot request khong (con) xac thuc.
     */
    @SuppressWarnings("unchecked")
    public Optional<AuthenticatedPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID accountId = UUID.fromString(claims.getSubject());
            List<String> roles = claims.get(ROLES_CLAIM, List.class);
            return Optional.of(new AuthenticatedPrincipal(accountId, new HashSet<>(roles)));
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Access token khong hop le: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
