package vn.taskconnect.auth.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.taskconnect.auth.api.AccountStatus;
import vn.taskconnect.auth.repository.AuthAccountRepository;
import vn.taskconnect.user.api.UserFacade;

/**
 * Unit test thuan tuy (repository va UserFacade mock, khong cham DB) - chi xac nhan
 * cleanupUnverifiedAccounts tinh dung moc cutoff (now - 24 gio), xoa ho so truoc qua
 * UserFacade roi moi xoa tai khoan theo dung danh sach id, va khong goi gi them khi
 * khong co tai khoan nao qua han.
 */
class AuthAccountCleanupServiceTest {

    @Test
    void should_deleteProfilesThenAccounts_when_expiredUnverifiedAccountsFound() {
        AuthAccountRepository accountRepository = mock(AuthAccountRepository.class);
        UserFacade userFacade = mock(UserFacade.class);

        Instant now = Instant.parse("2026-08-30T00:00:00Z");
        Instant expectedCutoff = now.minus(Duration.ofHours(24));
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);

        List<UUID> expiredIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(accountRepository.findIdsByStatusAndCreatedAtBefore(AccountStatus.UNVERIFIED, expectedCutoff))
                .thenReturn(expiredIds);
        when(accountRepository.deleteByIdIn(expiredIds)).thenReturn(2);

        AuthAccountCleanupService service = new AuthAccountCleanupService(accountRepository, userFacade, clock);

        service.cleanupUnverifiedAccounts();

        verify(userFacade).deleteProfilesByAccountIds(expiredIds);
        verify(accountRepository).deleteByIdIn(eq(expiredIds));
    }

    @Test
    void should_doNothingElse_when_noExpiredUnverifiedAccountsFound() {
        AuthAccountRepository accountRepository = mock(AuthAccountRepository.class);
        UserFacade userFacade = mock(UserFacade.class);

        Instant now = Instant.parse("2026-08-30T00:00:00Z");
        Instant expectedCutoff = now.minus(Duration.ofHours(24));
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);

        when(accountRepository.findIdsByStatusAndCreatedAtBefore(AccountStatus.UNVERIFIED, expectedCutoff))
                .thenReturn(List.of());

        AuthAccountCleanupService service = new AuthAccountCleanupService(accountRepository, userFacade, clock);

        service.cleanupUnverifiedAccounts();

        verify(userFacade, never()).deleteProfilesByAccountIds(org.mockito.ArgumentMatchers.anyList());
        verify(accountRepository, never()).deleteByIdIn(org.mockito.ArgumentMatchers.anyList());
    }
}
