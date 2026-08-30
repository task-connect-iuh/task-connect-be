package vn.taskconnect.auth.service;

import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.auth.repository.AuthEmailVerificationTokenRepository;
import vn.taskconnect.auth.repository.AuthPasswordResetTokenRepository;
import vn.taskconnect.auth.repository.AuthRefreshTokenRepository;

/**
 * Don dep cac token da het han cua module Auth (refresh token, OTP xac minh email, OTP
 * dat lai mat khau). Ca ba bang la append-only - khong dong nao tu xoa ngoai
 * ON DELETE CASCADE khi tai khoan goc bi xoa - nen can job rieng, khong thi bang phinh
 * vo han theo so luot dang ky/quen mat khau/dang nhap, khong theo so tai khoan.
 */
@Service
public class AuthTokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenCleanupService.class);

    private final AuthRefreshTokenRepository refreshTokenRepository;
    private final AuthEmailVerificationTokenRepository verificationTokenRepository;
    private final AuthPasswordResetTokenRepository passwordResetTokenRepository;
    private final Clock clock;

    public AuthTokenCleanupService(AuthRefreshTokenRepository refreshTokenRepository,
            AuthEmailVerificationTokenRepository verificationTokenRepository,
            AuthPasswordResetTokenRepository passwordResetTokenRepository, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.clock = clock;
    }

    /**
     * Xoa moi dong co expires_at truoc thoi diem goi ham, tren ca ba bang. Dung
     * expires_at lam moc duy nhat, khong them nguong "giu them N ngay sau khi het han":
     * mot token da het han la vo dung ngay lap tuc bat ke used_at, con khoang dem truoc
     * khi bi don den tu chinh chu ky goi job (AuthTokenCleanupScheduler, 7 ngay/lan).
     */
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = clock.instant();
        int refreshDeleted = refreshTokenRepository.deleteByExpiresAtBefore(now);
        int verificationDeleted = verificationTokenRepository.deleteByExpiresAtBefore(now);
        int resetDeleted = passwordResetTokenRepository.deleteByExpiresAtBefore(now);
        log.info(
                "Don dep token Auth: xoa {} refresh token, {} ma xac minh email, {} ma dat lai mat khau da het han",
                refreshDeleted, verificationDeleted, resetDeleted);
    }
}
