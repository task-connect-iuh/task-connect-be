package vn.taskconnect.auth.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import vn.taskconnect.auth.repository.AuthEmailVerificationTokenRepository;
import vn.taskconnect.auth.repository.AuthPasswordResetTokenRepository;
import vn.taskconnect.auth.repository.AuthRefreshTokenRepository;

/**
 * Unit test thuan tuy (repository mock, khong cham DB) - chi xac nhan
 * cleanupExpiredTokens goi dung ca ba repository voi cung moc thoi gian tu Clock.
 */
class AuthTokenCleanupServiceTest {

    @Test
    void should_deleteExpiredTokensOnAllThreeRepositories_when_cleanupRuns() {
        AuthRefreshTokenRepository refreshTokenRepository = mock(AuthRefreshTokenRepository.class);
        AuthEmailVerificationTokenRepository verificationTokenRepository =
                mock(AuthEmailVerificationTokenRepository.class);
        AuthPasswordResetTokenRepository passwordResetTokenRepository = mock(AuthPasswordResetTokenRepository.class);

        Instant now = Instant.parse("2026-08-29T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);

        when(refreshTokenRepository.deleteByExpiresAtBefore(now)).thenReturn(3);
        when(verificationTokenRepository.deleteByExpiresAtBefore(now)).thenReturn(2);
        when(passwordResetTokenRepository.deleteByExpiresAtBefore(now)).thenReturn(1);

        AuthTokenCleanupService service = new AuthTokenCleanupService(refreshTokenRepository,
                verificationTokenRepository, passwordResetTokenRepository, clock);

        service.cleanupExpiredTokens();

        verify(refreshTokenRepository).deleteByExpiresAtBefore(eq(now));
        verify(verificationTokenRepository).deleteByExpiresAtBefore(eq(now));
        verify(passwordResetTokenRepository).deleteByExpiresAtBefore(eq(now));
    }
}
