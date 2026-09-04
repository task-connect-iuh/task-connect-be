package vn.taskconnect.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.taskconnect.auth.api.AccountRole;
import vn.taskconnect.auth.api.AccountStatus;
import vn.taskconnect.auth.dto.request.GoogleLoginRequest;
import vn.taskconnect.auth.entity.AuthAccount;
import vn.taskconnect.auth.entity.AuthAccountRole;
import vn.taskconnect.auth.repository.AuthAccountRepository;
import vn.taskconnect.auth.repository.AuthAccountRoleRepository;
import vn.taskconnect.auth.repository.AuthEmailVerificationTokenRepository;
import vn.taskconnect.auth.repository.AuthPasswordResetTokenRepository;
import vn.taskconnect.auth.repository.AuthRefreshTokenRepository;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.security.AdminProperties;
import vn.taskconnect.security.google.GoogleProfile;
import vn.taskconnect.security.google.GoogleTokenVerifierService;
import vn.taskconnect.security.jwt.JwtProperties;
import vn.taskconnect.security.jwt.JwtTokenProvider;

/**
 * Unit test thuan tuy (khong DB, khong Spring context) cho AuthService.loginWithGoogle()/
 * confirmGoogleLink(), dung Mockito mock cac repository va GoogleTokenVerifierService. Xem
 * ghi chu tren tung method trong AuthService de biet ly do cua tung nhanh.
 */
class AuthServiceGoogleLoginTest {

    private static final String SUPER_ADMIN_EMAIL = "super-admin@taskconnect.vn";
    private static final Instant FIXED_NOW = Instant.parse("2026-08-31T10:00:00Z");
    private static final String ID_TOKEN = "fake-id-token";
    private static final String GOOGLE_ID = "google-sub-123";
    private static final String EMAIL = "khanh@example.com";

    private final AuthAccountRepository accountRepository = mock(AuthAccountRepository.class);
    private final AuthAccountRoleRepository accountRoleRepository = mock(AuthAccountRoleRepository.class);
    private final GoogleTokenVerifierService googleTokenVerifier = mock(GoogleTokenVerifierService.class);
    private final JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private final AuthService service = new AuthService(
            accountRepository,
            accountRoleRepository,
            mock(AuthRefreshTokenRepository.class),
            mock(AuthEmailVerificationTokenRepository.class),
            mock(AuthPasswordResetTokenRepository.class),
            mock(PasswordEncoder.class),
            tokenProvider,
            googleTokenVerifier,
            new JwtProperties("test-secret", 15, 15, false),
            new AdminProperties(SUPER_ADMIN_EMAIL),
            mock(ApplicationEventPublisher.class),
            clock);

    private GoogleLoginRequest request() {
        return new GoogleLoginRequest(ID_TOKEN);
    }

    private GoogleProfile verifiedProfile() {
        return new GoogleProfile(GOOGLE_ID, EMAIL, true);
    }

    // loginWithGoogle - chua ai tung dang nhap Google hay dang ky email nay, tao tai khoan moi.
    @Test
    void should_createNewActiveAccount_when_noExistingAccountMatchesGoogleIdOrEmail() {
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(verifiedProfile());
        when(accountRepository.findByGoogleId(GOOGLE_ID)).thenReturn(Optional.empty());
        when(accountRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        service.loginWithGoogle(request());

        ArgumentCaptor<AuthAccount> captor = ArgumentCaptor.forClass(AuthAccount.class);
        verify(accountRepository, times(1)).saveAndFlush(captor.capture());
        AuthAccount created = captor.getValue();
        assertThat(created.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(created.getPasswordHash()).isNull();
        assertThat(created.getGoogleId()).isEqualTo(GOOGLE_ID);

        verify(accountRoleRepository, times(1)).save(argThatRole(AccountRole.TASK_POSTER, created.getId()));
        verify(accountRoleRepository, times(1)).save(argThatRole(AccountRole.TASKER, created.getId()));
        verify(tokenProvider, times(1)).generateAccessToken(any(), any());
    }

    // loginWithGoogle - google_id da khop san, day la lan dang nhap lai.
    @Test
    void should_signInExistingAccount_when_googleIdAlreadyLinked() {
        AuthAccount existing = accountOf(UUID.randomUUID(), EMAIL, AccountStatus.ACTIVE);
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(verifiedProfile());
        when(accountRepository.findByGoogleId(GOOGLE_ID)).thenReturn(Optional.of(existing));

        service.loginWithGoogle(request());

        verify(accountRepository, never()).saveAndFlush(any());
        verify(accountRepository, times(1)).save(existing);
        verify(tokenProvider, times(1)).generateAccessToken(any(), any());
    }

    // loginWithGoogle - email trung mot tai khoan mat khau cu, chua tung gan Google: KHONG tu
    // lien ket ngam, chi bao FE hoi lai nguoi dung, khong doi gi trong DB.
    @Test
    void should_throwLinkConfirmationRequired_when_emailMatchesExistingPasswordAccount() {
        AuthAccount existing = accountOf(UUID.randomUUID(), EMAIL, AccountStatus.ACTIVE);
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(verifiedProfile());
        when(accountRepository.findByGoogleId(GOOGLE_ID)).thenReturn(Optional.empty());
        when(accountRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.loginWithGoogle(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.GOOGLE_LINK_CONFIRMATION_REQUIRED);

        assertThat(existing.getGoogleId()).isNull();
        verify(accountRepository, never()).save(any());
        verify(accountRepository, never()).saveAndFlush(any());
        verify(tokenProvider, never()).generateAccessToken(any(), any());
    }

    // loginWithGoogle - email Google chua duoc Google xac thuc.
    @Test
    void should_throwGoogleEmailNotVerified_when_profileEmailNotVerified() {
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(new GoogleProfile(GOOGLE_ID, EMAIL, false));

        assertThatThrownBy(() -> service.loginWithGoogle(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.GOOGLE_EMAIL_NOT_VERIFIED);
    }

    // loginWithGoogle - token khong hop le, GoogleTokenVerifierService da tu nem loi rieng.
    @Test
    void should_propagateInvalidGoogleToken_when_verifierRejectsToken() {
        when(googleTokenVerifier.verify(ID_TOKEN)).thenThrow(new BusinessException(ErrorCode.INVALID_GOOGLE_TOKEN));

        assertThatThrownBy(() -> service.loginWithGoogle(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.INVALID_GOOGLE_TOKEN);
    }

    // loginWithGoogle - tai khoan Google da lien ket nhung dang bi khoa tam thoi.
    @Test
    void should_throwAccountLocked_when_existingGoogleAccountIsLocked() {
        AuthAccount locked = accountOf(UUID.randomUUID(), EMAIL, AccountStatus.LOCKED);
        locked.registerFailedLogin(FIXED_NOW, 1, java.time.Duration.ofMinutes(15));
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(verifiedProfile());
        when(accountRepository.findByGoogleId(GOOGLE_ID)).thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> service.loginWithGoogle(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED);
    }

    // confirmGoogleLink - lien ket thanh cong, tai khoan UNVERIFIED chuyen thang ACTIVE.
    @Test
    void should_linkAndActivateAccount_when_confirmingUnverifiedAccount() {
        AuthAccount unverified = accountOf(UUID.randomUUID(), EMAIL, AccountStatus.UNVERIFIED);
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(verifiedProfile());
        when(accountRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unverified));

        service.confirmGoogleLink(request());

        assertThat(unverified.getGoogleId()).isEqualTo(GOOGLE_ID);
        assertThat(unverified.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(tokenProvider, times(1)).generateAccessToken(any(), any());
    }

    // confirmGoogleLink - tai khoan goc khong con ton tai giua chung (bien hiem).
    @Test
    void should_throwAccountNotFound_when_emailNoLongerExists() {
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(verifiedProfile());
        when(accountRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmGoogleLink(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    // confirmGoogleLink - tai khoan dich dang SUSPENDED, van chan du da xac nhan lien ket.
    @Test
    void should_throwAccountSuspended_when_confirmingSuspendedAccount() {
        AuthAccount suspended = accountOf(UUID.randomUUID(), EMAIL, AccountStatus.SUSPENDED);
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(verifiedProfile());
        when(accountRepository.findByEmail(EMAIL)).thenReturn(Optional.of(suspended));

        assertThatThrownBy(() -> service.confirmGoogleLink(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.ACCOUNT_SUSPENDED);
    }

    private AuthAccount accountOf(UUID id, String email, AccountStatus status) {
        return new AuthAccount(id, email, null, "hash", status, FIXED_NOW);
    }

    private AuthAccountRole argThatRole(AccountRole role, UUID accountId) {
        return org.mockito.ArgumentMatchers.argThat(saved -> saved != null
                && saved.getAccountId().equals(accountId)
                && saved.getRole() == role);
    }
}
