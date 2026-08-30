package vn.taskconnect.auth.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import vn.taskconnect.auth.api.AccountStatus;
import vn.taskconnect.auth.repository.AuthAccountRepository;

/**
 * Unit test thuan tuy (repository mock, khong cham DB) - chi xac nhan
 * cleanupUnverifiedAccounts tinh dung moc cutoff (now - 24 gio) va goi dung repository.
 */
class AuthAccountCleanupServiceTest {

    @Test
    void should_deleteUnverifiedAccountsOlderThan24Hours_when_cleanupRuns() {
        AuthAccountRepository accountRepository = mock(AuthAccountRepository.class);

        Instant now = Instant.parse("2026-08-30T00:00:00Z");
        Instant expectedCutoff = now.minus(Duration.ofHours(24));
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);

        when(accountRepository.deleteByStatusAndCreatedAtBefore(AccountStatus.UNVERIFIED, expectedCutoff))
                .thenReturn(4);

        AuthAccountCleanupService service = new AuthAccountCleanupService(accountRepository, clock);

        service.cleanupUnverifiedAccounts();

        verify(accountRepository).deleteByStatusAndCreatedAtBefore(eq(AccountStatus.UNVERIFIED), eq(expectedCutoff));
    }
}
