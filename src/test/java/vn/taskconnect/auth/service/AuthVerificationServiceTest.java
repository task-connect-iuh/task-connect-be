package vn.taskconnect.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
import vn.taskconnect.auth.dto.request.RegisterRequest;
import vn.taskconnect.auth.dto.request.ResendVerificationRequest;
import vn.taskconnect.auth.dto.request.VerifyEmailRequest;
import vn.taskconnect.auth.dto.response.ResendVerificationResponse;
import vn.taskconnect.auth.entity.AuthAccount;
import vn.taskconnect.auth.entity.AuthEmailVerificationToken;
import vn.taskconnect.auth.repository.AuthAccountRepository;
import vn.taskconnect.auth.repository.AuthEmailVerificationTokenRepository;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.notification.infrastructure.EmailSender;
import vn.taskconnect.support.AbstractIntegrationTest;

/**
 * Luong xac minh email bang OTP, xuyen tu AuthService qua module Notification, voi DB
 * that (Testcontainers MariaDB - Flyway chay V3 that, tu no la mot phep thu migration).
 *
 * <p>EmailSender duoc thay bang mock: khong goi SMTP that, nhung van cho phep bat lai
 * noi dung email de lay ra ma OTP that (khong co cach nao doc OTP tu DB vi no duoc luu
 * duoi dang BCrypt).
 */
class AuthVerificationServiceTest extends AbstractIntegrationTest {

    private static final Pattern SIX_DIGITS = Pattern.compile("\\d{6}");

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthAccountRepository accountRepository;

    @Autowired
    private AuthEmailVerificationTokenRepository verificationTokenRepository;

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
    void should_storeOtpHashed_when_registering() {
        String email = uniqueEmail("hashed");
        authService.register(registerRequest(email));
        String otp = captureLatestOtp(email);

        AuthEmailVerificationToken token = latestActiveToken(email);

        assertThat(token.getTokenHash()).isNotEqualTo(otp);
        assertThat(passwordEncoder.matches(otp, token.getTokenHash())).isTrue();
    }

    @Test
    void should_activateAccount_when_otpCorrect() {
        String email = uniqueEmail("activate");
        authService.register(registerRequest(email));
        String otp = captureLatestOtp(email);

        authService.verifyEmail(new VerifyEmailRequest(email, otp));

        assertThat(accountRepository.findByEmail(email).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void should_returnSuccess_when_verifyingAlreadyActiveAccount() {
        String email = uniqueEmail("idempotent");
        authService.register(registerRequest(email));
        String otp = captureLatestOtp(email);
        authService.verifyEmail(new VerifyEmailRequest(email, otp));

        assertThatCode(() -> authService.verifyEmail(new VerifyEmailRequest(email, otp)))
                .doesNotThrowAnyException();
    }

    /**
     * Test gia tri nhat trong bo: chot chan hoi quy cho @Transactional(noRollbackFor =
     * BusinessException.class) tren verifyEmail(). Thieu no, nhanh sai ma tang
     * attempt_count roi nem BusinessException se bi Spring rollback ca thay doi do, va
     * gioi han so lan nhap sai khong bao gio co hieu luc (dung bay da gap o login()).
     */
    @Test
    void should_persistAttemptCount_when_verifyFails() {
        String email = uniqueEmail("attempt");
        authService.register(registerRequest(email));
        String correctOtp = captureLatestOtp(email);
        String wrongOtp = wrongOtpFor(correctOtp);

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest(email, wrongOtp)))
                .isInstanceOf(BusinessException.class);

        AuthEmailVerificationToken token = latestActiveToken(email);
        assertThat(token.getAttemptCount())
                .as("attempt_count phai duoc luu lai du nhanh sai nem BusinessException ngay sau")
                .isEqualTo(1);
    }

    @Test
    void should_burnOtp_when_reachingMaxAttempts() {
        String email = uniqueEmail("burn");
        authService.register(registerRequest(email));
        String correctOtp = captureLatestOtp(email);
        String wrongOtp = wrongOtpFor(correctOtp);

        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest(email, wrongOtp)))
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.INVALID_VERIFICATION_OTP);
        }

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest(email, wrongOtp)))
                .as("lan sai thu 5 phai dot ma, khong con la loi 'sai ma' don thuan")
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.TOO_MANY_OTP_ATTEMPTS);

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest(email, correctOtp)))
                .as("ma da bi dot, ke ca nhap dung cung phai bi tu choi")
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.INVALID_VERIFICATION_OTP);
    }

    @Test
    void should_rejectOtp_when_expired() {
        String email = uniqueEmail("expired");
        authService.register(registerRequest(email));
        String otp = captureLatestOtp(email);

        backdateActiveToken(email, "expiresAt", Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest(email, otp)))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.EXPIRED_VERIFICATION_OTP);
    }

    @Test
    void should_invalidatePreviousOtp_when_resending() {
        String email = uniqueEmail("resend");
        authService.register(registerRequest(email));
        String firstOtp = captureLatestOtp(email);

        // Dua thoi diem tao ma dau tien lui qua nguong cooldown 60 giay de lan resend
        // duoi day khong bi chan boi cooldown.
        backdateActiveToken(email, "createdAt", Instant.now().minusSeconds(61));

        ResendVerificationResponse response = authService.resendVerification(new ResendVerificationRequest(email));
        assertThat(response.retryAfterSeconds()).isEqualTo(60);

        String secondOtp = captureLatestOtp(email);
        assertThat(secondOtp).isNotEqualTo(firstOtp);

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest(email, firstOtp)))
                .as("ma dau tien phai bi vo hieu hoa sau khi gui lai")
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.INVALID_VERIFICATION_OTP);

        authService.verifyEmail(new VerifyEmailRequest(email, secondOtp));
        assertThat(accountRepository.findByEmail(email).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void should_skipSending_when_withinResendCooldown() {
        String email = uniqueEmail("cooldown");
        authService.register(registerRequest(email));
        captureLatestOtp(email);
        reset(emailSender);

        ResendVerificationResponse response = authService.resendVerification(new ResendVerificationRequest(email));

        assertThat(response.retryAfterSeconds()).isEqualTo(60);
        verifyNoInteractions(emailSender);
    }

    @Test
    void should_returnIdenticalBody_when_resendingForUnknownEmail() {
        ResendVerificationResponse response =
                authService.resendVerification(new ResendVerificationRequest(uniqueEmail("khong-ton-tai")));

        assertThat(response.retryAfterSeconds()).isEqualTo(60);
        verifyNoInteractions(emailSender);
    }

    /**
     * Bao dam AFTER_COMMIT: register() participation trong mot transaction ngoai do
     * TransactionTemplate mo, sau do bi cho rollback co chu y. Ca account lan email deu
     * khong duoc phep ton tai sau do - dung dung nghia AFTER_COMMIT, khong phai "publish
     * la gui ngay".
     */
    @Test
    void should_notSendMail_when_registrationRollsBack() {
        String email = uniqueEmail("rollback");

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            authService.register(registerRequest(email));
            throw new IllegalStateException("buoc rollback de kiem tra AFTER_COMMIT");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(accountRepository.existsByEmail(email)).isFalse();
        verifyNoInteractions(emailSender);
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

    private AuthEmailVerificationToken latestActiveToken(String email) {
        AuthAccount account = accountRepository.findByEmail(email).orElseThrow();
        return verificationTokenRepository.findFirstByAccountIdAndUsedAtIsNullOrderByCreatedAtDesc(account.getId())
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
     * chuan cho test, khac voi dirty-checking tu dong cua entity. Chay trong transaction
     * rieng cua no (moi loi goi AuthService cung mo transaction rieng, khong co
     * persistence context nao dung chung giua cac buoc de phai lo stale cache), mo
     * phong dung thuc te la thoi gian troi qua giua hai request khac nhau.
     */
    private void backdateActiveToken(String email, String field, Instant value) {
        AuthAccount account = accountRepository.findByEmail(email).orElseThrow();
        transactionTemplate.executeWithoutResult(status -> entityManager
                .createQuery("UPDATE AuthEmailVerificationToken t SET t." + field + " = :value "
                        + "WHERE t.accountId = :accountId AND t.usedAt IS NULL")
                .setParameter("value", value)
                .setParameter("accountId", account.getId())
                .executeUpdate());
    }
}
