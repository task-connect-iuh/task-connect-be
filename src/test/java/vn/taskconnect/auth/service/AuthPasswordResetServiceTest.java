package vn.taskconnect.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.taskconnect.auth.api.AccountRole;
import vn.taskconnect.auth.api.AccountStatus;
import vn.taskconnect.auth.dto.request.ForgotPasswordRequest;
import vn.taskconnect.auth.dto.request.LoginRequest;
import vn.taskconnect.auth.dto.request.RegisterRequest;
import vn.taskconnect.auth.dto.request.ResetPasswordRequest;
import vn.taskconnect.auth.dto.request.VerifyEmailRequest;
import vn.taskconnect.auth.dto.response.ForgotPasswordResponse;
import vn.taskconnect.auth.dto.response.TokenResponse;
import vn.taskconnect.auth.entity.AuthAccount;
import vn.taskconnect.auth.entity.AuthPasswordResetToken;
import vn.taskconnect.auth.repository.AuthAccountRepository;
import vn.taskconnect.auth.repository.AuthPasswordResetTokenRepository;
import vn.taskconnect.auth.repository.AuthRefreshTokenRepository;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.notification.infrastructure.EmailSender;
import vn.taskconnect.support.AbstractIntegrationTest;

/**
 * Luong quen mat khau bang OTP, xuyen tu AuthService qua module Notification, voi DB that
 * (Testcontainers MariaDB - Flyway chay V4 that, tu no la mot phep thu migration).
 *
 * <p>Cau truc mirror AuthVerificationServiceTest: EmailSender bi mock nhung van cho phep
 * bat lai noi dung email de lay OTP that (khong co cach nao doc OTP tu DB vi luu BCrypt).
 */
class AuthPasswordResetServiceTest extends AbstractIntegrationTest {

    private static final Pattern SIX_DIGITS = Pattern.compile("\\d{6}");

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthAccountRepository accountRepository;

    @Autowired
    private AuthPasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private AuthRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoBean
    private EmailSender emailSender;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void should_storeOtpHashed_when_requestingReset() {
        String email = registerActiveAccount("hashed");
        reset(emailSender);

        authService.forgotPassword(new ForgotPasswordRequest(email));
        String otp = captureLatestOtp(email);

        AuthPasswordResetToken token = latestActiveToken(email);
        assertThat(token.getTokenHash()).isNotEqualTo(otp);
        assertThat(passwordEncoder.matches(otp, token.getTokenHash())).isTrue();
    }

    @Test
    void should_changePassword_when_otpCorrect() {
        String email = registerActiveAccount("reset-ok");
        authService.forgotPassword(new ForgotPasswordRequest(email));
        String otp = captureLatestOtp(email);

        authService.resetPassword(new ResetPasswordRequest(email, otp, "MatKhauMoi@123", "MatKhauMoi@123"));

        assertThatThrownBy(() -> authService.login(new LoginRequest(email, "Matkhau@123")))
                .as("mat khau cu phai bi tu choi sau khi da doi")
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        TokenResponse tokens = authService.login(new LoginRequest(email, "MatKhauMoi@123"));
        assertThat(tokens.accessToken()).isNotBlank();
    }

    @Test
    void should_revokeAllRefreshTokens_when_resetSucceeds() {
        String email = registerActiveAccount("revoke-all");
        TokenResponse loginTokens = authService.login(new LoginRequest(email, "Matkhau@123"));

        authService.forgotPassword(new ForgotPasswordRequest(email));
        String otp = captureLatestOtp(email);
        authService.resetPassword(new ResetPasswordRequest(email, otp, "MatKhauMoi@123", "MatKhauMoi@123"));

        assertThatThrownBy(() -> authService.refresh(loginTokens.refreshToken()))
                .as("refresh token cap truoc khi dat lai mat khau phai bi thu hoi")
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void should_persistAttemptCount_when_resetFails() {
        String email = registerActiveAccount("attempt");
        authService.forgotPassword(new ForgotPasswordRequest(email));
        String correctOtp = captureLatestOtp(email);
        String wrongOtp = wrongOtpFor(correctOtp);

        assertThatThrownBy(() -> authService.resetPassword(
                new ResetPasswordRequest(email, wrongOtp, "MatKhauMoi@123", "MatKhauMoi@123")))
                .isInstanceOf(BusinessException.class);

        AuthPasswordResetToken token = latestActiveToken(email);
        assertThat(token.getAttemptCount())
                .as("attempt_count phai duoc luu lai du nhanh sai nem BusinessException ngay sau")
                .isEqualTo(1);
    }

    @Test
    void should_burnOtp_when_reachingMaxAttempts() {
        String email = registerActiveAccount("burn");
        authService.forgotPassword(new ForgotPasswordRequest(email));
        String correctOtp = captureLatestOtp(email);
        String wrongOtp = wrongOtpFor(correctOtp);

        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThatThrownBy(() -> authService.resetPassword(
                    new ResetPasswordRequest(email, wrongOtp, "MatKhauMoi@123", "MatKhauMoi@123")))
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.INVALID_RESET_OTP);
        }

        assertThatThrownBy(() -> authService.resetPassword(
                new ResetPasswordRequest(email, wrongOtp, "MatKhauMoi@123", "MatKhauMoi@123")))
                .as("lan sai thu 5 phai dot ma, khong con la loi 'sai ma' don thuan")
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.TOO_MANY_RESET_OTP_ATTEMPTS);

        assertThatThrownBy(() -> authService.resetPassword(
                new ResetPasswordRequest(email, correctOtp, "MatKhauMoi@123", "MatKhauMoi@123")))
                .as("ma da bi dot, ke ca nhap dung cung phai bi tu choi")
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.INVALID_RESET_OTP);
    }

    @Test
    void should_rejectOtp_when_expired() {
        String email = registerActiveAccount("expired");
        authService.forgotPassword(new ForgotPasswordRequest(email));
        String otp = captureLatestOtp(email);

        backdateActiveToken(email, "expiresAt", Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> authService.resetPassword(
                new ResetPasswordRequest(email, otp, "MatKhauMoi@123", "MatKhauMoi@123")))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.EXPIRED_RESET_OTP);
    }

    @Test
    void should_skipSending_when_withinResendCooldown() {
        String email = registerActiveAccount("cooldown");
        authService.forgotPassword(new ForgotPasswordRequest(email));
        captureLatestOtp(email);
        reset(emailSender);

        ForgotPasswordResponse response = authService.forgotPassword(new ForgotPasswordRequest(email));

        assertThat(response.retryAfterSeconds()).isEqualTo(60);
        verifyNoInteractions(emailSender);
    }

    @Test
    void should_returnIdenticalBody_when_requestingResetForUnknownEmail() {
        ForgotPasswordResponse response =
                authService.forgotPassword(new ForgotPasswordRequest(uniqueEmail("khong-ton-tai")));

        assertThat(response.retryAfterSeconds()).isEqualTo(60);
        verifyNoInteractions(emailSender);
    }

    @Test
    void should_activateAccount_when_resetPasswordSucceedsForUnverifiedAccount() {
        String email = uniqueEmail("unverified-reset");
        authService.register(registerRequest(email));
        reset(emailSender);

        authService.forgotPassword(new ForgotPasswordRequest(email));
        String otp = captureLatestOtp(email);
        authService.resetPassword(new ResetPasswordRequest(email, otp, "MatKhauMoi@123", "MatKhauMoi@123"));

        AuthAccount account = accountRepository.findByEmail(email).orElseThrow();
        assertThat(account.getStatus())
                .as("OTP gui toi email la bang chung xac minh email, tai khoan phai duoc kich hoat luon")
                .isEqualTo(AccountStatus.ACTIVE);

        TokenResponse tokens = authService.login(new LoginRequest(email, "MatKhauMoi@123"));
        assertThat(tokens.accessToken()).isNotBlank();
    }

    @Test
    void should_notSendMail_when_accountSuspended() {
        String email = registerActiveAccount("suspended");
        suspendAccount(email);

        ForgotPasswordResponse response = authService.forgotPassword(new ForgotPasswordRequest(email));

        assertThat(response.retryAfterSeconds()).isEqualTo(60);
        verifyNoInteractions(emailSender);
    }

    private String registerActiveAccount(String tag) {
        String email = uniqueEmail(tag);
        authService.register(registerRequest(email));
        String otp = captureLatestOtp(email);
        authService.verifyEmail(new VerifyEmailRequest(email, otp));
        reset(emailSender);
        return email;
    }

    private RegisterRequest registerRequest(String email) {
        return new RegisterRequest("Nguyen Van A", email, null, "Matkhau@123", "Matkhau@123", Set.of(AccountRole.TASK_POSTER));
    }

    private String uniqueEmail(String tag) {
        return tag + "-" + UUID.randomUUID() + "@example.com";
    }

    private String wrongOtpFor(String correctOtp) {
        return correctOtp.equals("111111") ? "222222" : "111111";
    }

    private AuthPasswordResetToken latestActiveToken(String email) {
        AuthAccount account = accountRepository.findByEmail(email).orElseThrow();
        return passwordResetTokenRepository
                .findFirstByAccountIdAndUsedAtIsNullOrderByCreatedAtDesc(account.getId())
                .orElseThrow();
    }

    private String captureLatestOtp(String email) {
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender, atLeastOnce()).send(eq(email), any(), bodyCaptor.capture());
        List<String> bodies = bodyCaptor.getAllValues();
        return extractOtp(bodies.get(bodies.size() - 1));
    }

    private String extractOtp(String emailBody) {
        Matcher matcher = SIX_DIGITS.matcher(emailBody);
        if (!matcher.find()) {
            throw new IllegalStateException("Khong tim thay ma OTP 6 chu so trong noi dung email test");
        }
        return matcher.group();
    }

    /**
     * Bo qua {@code updatable = false} cua Hibernate bang JPQL bulk update - ky thuat
     * chuan cho test, mo phong dung thuc te la thoi gian troi qua giua hai request khac
     * nhau. Xem chu thich chi tiet hon o AuthVerificationServiceTest.backdateActiveToken.
     */
    private void backdateActiveToken(String email, String field, Instant value) {
        AuthAccount account = accountRepository.findByEmail(email).orElseThrow();
        transactionTemplate.executeWithoutResult(status -> entityManager
                .createQuery("UPDATE AuthPasswordResetToken t SET t." + field + " = :value "
                        + "WHERE t.accountId = :accountId AND t.usedAt IS NULL")
                .setParameter("value", value)
                .setParameter("accountId", account.getId())
                .executeUpdate());
    }

    /**
     * Dat status = SUSPENDED truc tiep bang JPQL: AuthAccount khong co method nghiep vu
     * nao chuyen sang SUSPENDED (chi Admin lam duoc, module Admin chua ton tai), test can
     * dung thang toi DB de mo phong trang thai nay.
     */
    private void suspendAccount(String email) {
        AuthAccount account = accountRepository.findByEmail(email).orElseThrow();
        transactionTemplate.executeWithoutResult(status -> entityManager
                .createQuery("UPDATE AuthAccount a SET a.status = vn.taskconnect.auth.api.AccountStatus.SUSPENDED "
                        + "WHERE a.id = :accountId")
                .setParameter("accountId", account.getId())
                .executeUpdate());
    }
}
