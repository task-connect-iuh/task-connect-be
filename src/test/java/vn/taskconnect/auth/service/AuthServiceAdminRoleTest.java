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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.taskconnect.auth.api.AccountRole;
import vn.taskconnect.auth.api.AccountStatus;
import vn.taskconnect.auth.dto.request.GrantAdminRoleRequest;
import vn.taskconnect.auth.dto.request.RevokeAdminRoleRequest;
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
import vn.taskconnect.security.jwt.JwtProperties;
import vn.taskconnect.security.jwt.JwtTokenProvider;
import vn.taskconnect.user.api.UserFacade;

/**
 * Unit test thuan tuy (khong DB, khong Spring context) cho AuthService.grantAdminRole()/
 * revokeAdminRole(), dung Mockito mock cac repository. Xem docs/PROGRESS-ADMIN-MODULE.md
 * "Cac quyet dinh da chot" de biet ly do cua tung nhanh loi.
 */
class AuthServiceAdminRoleTest {

    private static final String SUPER_ADMIN_EMAIL = "super-admin@taskconnect.vn";
    private static final UUID SUPER_ADMIN_ID = UUID.randomUUID();
    private static final UUID ORDINARY_ADMIN_ID = UUID.randomUUID();
    private static final Instant FIXED_NOW = Instant.parse("2026-08-31T10:00:00Z");

    private final AuthAccountRepository accountRepository = mock(AuthAccountRepository.class);
    private final AuthAccountRoleRepository accountRoleRepository = mock(AuthAccountRoleRepository.class);
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private final AuthService service = new AuthService(
            accountRepository,
            accountRoleRepository,
            mock(AuthRefreshTokenRepository.class),
            mock(AuthEmailVerificationTokenRepository.class),
            mock(AuthPasswordResetTokenRepository.class),
            mock(PasswordEncoder.class),
            mock(JwtTokenProvider.class),
            new JwtProperties("test-secret", 15, 15, false),
            new AdminProperties(SUPER_ADMIN_EMAIL),
            mock(UserFacade.class),
            mock(ApplicationEventPublisher.class),
            clock);

    private AuthAccount accountOf(UUID id, String email) {
        return new AuthAccount(id, email, null, "hash", AccountStatus.ACTIVE, FIXED_NOW);
    }

    // Grant - super-admin thanh cong.
    @Test
    void should_grantAdminRole_when_calledBySuperAdmin() {
        AuthAccount superAdmin = accountOf(SUPER_ADMIN_ID, SUPER_ADMIN_EMAIL);
        AuthAccount target = accountOf(UUID.randomUUID(), "tasker@example.com");
        when(accountRepository.findById(SUPER_ADMIN_ID)).thenReturn(Optional.of(superAdmin));
        when(accountRepository.findByEmail("tasker@example.com")).thenReturn(Optional.of(target));
        when(accountRoleRepository.existsByAccountIdAndRole(target.getId(), AccountRole.ADMIN)).thenReturn(false);

        service.grantAdminRole(SUPER_ADMIN_ID, new GrantAdminRoleRequest("tasker@example.com"));

        verify(accountRoleRepository, times(1)).save(any(AuthAccountRole.class));
    }

    // Grant - tai khoan goi khong phai super-admin (kem ca truong hop no cung mang ROLE_ADMIN).
    @Test
    void should_throwNotSuperAdmin_when_grantCalledByOrdinaryAdmin() {
        AuthAccount ordinaryAdmin = accountOf(ORDINARY_ADMIN_ID, "ordinary-admin@example.com");
        when(accountRepository.findById(ORDINARY_ADMIN_ID)).thenReturn(Optional.of(ordinaryAdmin));

        assertThatThrownBy(() -> service.grantAdminRole(ORDINARY_ADMIN_ID, new GrantAdminRoleRequest("someone@example.com")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.NOT_SUPER_ADMIN);
        verify(accountRoleRepository, never()).save(any());
    }

    // Grant - tai khoan dich khong ton tai.
    @Test
    void should_throwAccountNotFound_when_grantTargetEmailDoesNotExist() {
        AuthAccount superAdmin = accountOf(SUPER_ADMIN_ID, SUPER_ADMIN_EMAIL);
        when(accountRepository.findById(SUPER_ADMIN_ID)).thenReturn(Optional.of(superAdmin));
        when(accountRepository.findByEmail("khong-ton-tai@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.grantAdminRole(SUPER_ADMIN_ID, new GrantAdminRoleRequest("khong-ton-tai@example.com")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    // Grant - tai khoan dich da co role ADMIN tu truoc.
    @Test
    void should_throwRoleAlreadyGranted_when_targetAlreadyHasAdminRole() {
        AuthAccount superAdmin = accountOf(SUPER_ADMIN_ID, SUPER_ADMIN_EMAIL);
        AuthAccount target = accountOf(UUID.randomUUID(), "already-admin@example.com");
        when(accountRepository.findById(SUPER_ADMIN_ID)).thenReturn(Optional.of(superAdmin));
        when(accountRepository.findByEmail("already-admin@example.com")).thenReturn(Optional.of(target));
        when(accountRoleRepository.existsByAccountIdAndRole(target.getId(), AccountRole.ADMIN)).thenReturn(true);

        assertThatThrownBy(() -> service.grantAdminRole(SUPER_ADMIN_ID, new GrantAdminRoleRequest("already-admin@example.com")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.ROLE_ALREADY_GRANTED);
        verify(accountRoleRepository, never()).save(any());
    }

    // Revoke - super-admin thu hoi role cua admin thuong thanh cong.
    @Test
    void should_revokeAdminRole_when_calledBySuperAdmin() {
        AuthAccount superAdmin = accountOf(SUPER_ADMIN_ID, SUPER_ADMIN_EMAIL);
        AuthAccount target = accountOf(ORDINARY_ADMIN_ID, "ordinary-admin@example.com");
        when(accountRepository.findById(SUPER_ADMIN_ID)).thenReturn(Optional.of(superAdmin));
        when(accountRepository.findByEmail("ordinary-admin@example.com")).thenReturn(Optional.of(target));
        when(accountRoleRepository.deleteByAccountIdAndRole(ORDINARY_ADMIN_ID, AccountRole.ADMIN)).thenReturn(1L);

        service.revokeAdminRole(SUPER_ADMIN_ID, new RevokeAdminRoleRequest("ordinary-admin@example.com"));

        verify(accountRoleRepository, times(1)).deleteByAccountIdAndRole(ORDINARY_ADMIN_ID, AccountRole.ADMIN);
    }

    // Revoke - tai khoan dich chua tung co role ADMIN.
    @Test
    void should_throwRoleNotAssigned_when_revokeTargetHasNoAdminRole() {
        AuthAccount superAdmin = accountOf(SUPER_ADMIN_ID, SUPER_ADMIN_EMAIL);
        AuthAccount target = accountOf(UUID.randomUUID(), "not-admin@example.com");
        when(accountRepository.findById(SUPER_ADMIN_ID)).thenReturn(Optional.of(superAdmin));
        when(accountRepository.findByEmail("not-admin@example.com")).thenReturn(Optional.of(target));
        when(accountRoleRepository.deleteByAccountIdAndRole(target.getId(), AccountRole.ADMIN)).thenReturn(0L);

        assertThatThrownBy(() -> service.revokeAdminRole(SUPER_ADMIN_ID, new RevokeAdminRoleRequest("not-admin@example.com")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.ROLE_NOT_ASSIGNED);
    }

    // Revoke - khong the tu thu hoi role cua chinh super-admin, du chinh super-admin la nguoi goi.
    @Test
    void should_throwCannotRevokeSuperAdmin_when_targetIsSuperAdminItself() {
        AuthAccount superAdmin = accountOf(SUPER_ADMIN_ID, SUPER_ADMIN_EMAIL);
        when(accountRepository.findById(SUPER_ADMIN_ID)).thenReturn(Optional.of(superAdmin));

        assertThatThrownBy(() -> service.revokeAdminRole(SUPER_ADMIN_ID, new RevokeAdminRoleRequest(SUPER_ADMIN_EMAIL)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.CANNOT_REVOKE_SUPER_ADMIN);
        verify(accountRoleRepository, never()).deleteByAccountIdAndRole(any(), any());
    }

    // Revoke - tai khoan goi khong phai super-admin bi chan truoc ca khi kiem tra target.
    @Test
    void should_throwNotSuperAdmin_when_revokeCalledByOrdinaryAdmin() {
        AuthAccount ordinaryAdmin = accountOf(ORDINARY_ADMIN_ID, "ordinary-admin@example.com");
        when(accountRepository.findById(ORDINARY_ADMIN_ID)).thenReturn(Optional.of(ordinaryAdmin));

        assertThatThrownBy(() -> service.revokeAdminRole(ORDINARY_ADMIN_ID, new RevokeAdminRoleRequest("someone@example.com")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.NOT_SUPER_ADMIN);
        verify(accountRoleRepository, never()).deleteByAccountIdAndRole(any(), any());
    }
}
