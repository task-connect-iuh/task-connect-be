package vn.taskconnect.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.auth.api.AccountRole;
import vn.taskconnect.auth.api.AccountStatus;
import vn.taskconnect.auth.api.event.EmailVerificationRequestedEvent;
import vn.taskconnect.auth.api.event.PasswordResetRequestedEvent;
import vn.taskconnect.auth.dto.request.ForgotPasswordRequest;
import vn.taskconnect.auth.dto.request.LoginRequest;
import vn.taskconnect.auth.dto.request.RegisterRequest;
import vn.taskconnect.auth.dto.request.ResendVerificationRequest;
import vn.taskconnect.auth.dto.request.ResetPasswordRequest;
import vn.taskconnect.auth.dto.request.VerifyEmailRequest;
import vn.taskconnect.auth.dto.response.ForgotPasswordResponse;
import vn.taskconnect.auth.dto.response.ResendVerificationResponse;
import vn.taskconnect.auth.dto.response.TokenResponse;
import vn.taskconnect.auth.entity.AuthAccount;
import vn.taskconnect.auth.entity.AuthAccountRole;
import vn.taskconnect.auth.entity.AuthEmailVerificationToken;
import vn.taskconnect.auth.entity.AuthPasswordResetToken;
import vn.taskconnect.auth.entity.AuthRefreshToken;
import vn.taskconnect.auth.repository.AuthAccountRepository;
import vn.taskconnect.auth.repository.AuthAccountRoleRepository;
import vn.taskconnect.auth.repository.AuthEmailVerificationTokenRepository;
import vn.taskconnect.auth.repository.AuthPasswordResetTokenRepository;
import vn.taskconnect.auth.repository.AuthRefreshTokenRepository;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.security.jwt.JwtProperties;
import vn.taskconnect.security.jwt.JwtTokenProvider;

/**
 * Nghiep vu dang ky, dang nhap, refresh, logout va xac minh email cua module Auth.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /**
     * TODO: cac nguong nay phai doc tu admin.system_parameters (xem
     * .claude/rules/02-source-of-truth.md - "Ngưỡng nghiệp vụ không bao giờ hardcode").
     * Hardcode tam vi module Admin chua duoc tao, da thong nhat voi nguoi dung trong
     * phien lam viec ngay tao file nay. Chuyen sang doc tu DB ngay khi Admin co bang nay.
     */
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    /**
     * TODO: cung ly do TODO tren - hardcode tam, da thong nhat voi nguoi dung.
     */
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration OTP_RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int OTP_BOUND = 1_000_000;

    private final AuthAccountRepository accountRepository;
    private final AuthAccountRoleRepository accountRoleRepository;
    private final AuthRefreshTokenRepository refreshTokenRepository;
    private final AuthEmailVerificationTokenRepository verificationTokenRepository;
    private final AuthPasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final Duration refreshTokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AuthAccountRepository accountRepository, AuthAccountRoleRepository accountRoleRepository,
            AuthRefreshTokenRepository refreshTokenRepository,
            AuthEmailVerificationTokenRepository verificationTokenRepository,
            AuthPasswordResetTokenRepository passwordResetTokenRepository, PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider, JwtProperties jwtProperties, ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.accountRoleRepository = accountRoleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.refreshTokenTtl = Duration.ofDays(jwtProperties.refreshTokenTtlDays());
    }

    /**
     * Check-then-insert o duoi khong du chong race condition (2 request dang ky cung email
     * gan nhu dong thoi). UNIQUE constraint tren auth_accounts.email/phone (xem
     * V1__create_auth_tables.sql) la lop chan cuoi cung - saveAndFlush() de bat duoc
     * DataIntegrityViolationException ngay tai day va convert thanh loi nghiep vu ro rang,
     * thay vi de lo loi DB tho hoac roi vao COMMON-409-DATA_CONFLICT chung chung.
     *
     * <p>Khong cap token o day: tai khoan vua tao dang UNVERIFIED, va login() gio chan
     * hoan toan trang thai nay - cap token ngay luc dang ky se cho phep dung app ma khong
     * can xac minh email, di nguoc quyet dinh chan UNVERIFIED. Nguoi dung phai xac minh OTP
     * roi tu dang nhap that qua login().
     */
    @Transactional
    public void register(RegisterRequest request) {
        if (request.roles().contains(AccountRole.ADMIN)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Không thể tự đăng ký vai trò Admin.");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Mật khẩu xác nhận không khớp.");
        }

        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone());

        if (accountRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
        if (phone != null && accountRepository.existsByPhone(phone)) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

        Instant now = clock.instant();
        AuthAccount account = new AuthAccount(UUID.randomUUID(), email, phone,
                passwordEncoder.encode(request.password()), AccountStatus.UNVERIFIED, now);
        try {
            accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateAccountError(ex);
        }

        for (AccountRole role : request.roles()) {
            accountRoleRepository.save(new AuthAccountRole(UUID.randomUUID(), account.getId(), role, now));
        }

        issueAndDispatchVerificationOtp(account, now);
    }

    /**
     * Dang nhap chi thanh cong voi tai khoan da ACTIVE - chan UNVERIFIED (chua xac minh
     * email), LOCKED (dang trong thoi gian khoa) va SUSPENDED. Quyet dinh sua doi tu cho
     * phep UNVERIFIED dang nhap sang chan hoan toan da xac nhan voi nguoi dung.
     *
     * <p>{@code noRollbackFor}: nhanh sai mat khau luu failed_login_count roi nem
     * BusinessException ngay sau - mac dinh Spring se rollback ca thay doi do, khien
     * so lan sai khong bao gio duoc ghi nhan that va tai khoan khong bao gio bi khoa.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse login(LoginRequest request) {
        AuthAccount account = accountRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        Instant now = clock.instant();
        if (account.getStatus() == AccountStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (account.getStatus() == AccountStatus.UNVERIFIED) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (account.isLocked(now)) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            account.registerFailedLogin(now, MAX_FAILED_LOGIN_ATTEMPTS, LOCK_DURATION);
            accountRepository.save(account);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        account.recordSuccessfulLogin(now);
        accountRepository.save(account);

        return issueTokens(account, rolesOf(account.getId()));
    }

    /**
     * Xoay vong refresh token: thu hoi token cu, phat access token va refresh token moi.
     * {@code rawRefreshToken} den tu cookie httpOnly - null/blank nghia la client khong
     * gui kem cookie (het han o tang browser, bi xoa tay...), coi nhu phien khong hop le
     * thay vi de NullPointerException lot ra ngoai.
     *
     * <p>Reuse detection: token da bi revoke (da rotate qua roi hoac da bi logout) ma van
     * bi dem len day chi co the la do client dung ban cu (vd tab khac chua kip cap nhat
     * cookie) hoac do bi danh cap raw token tu truoc. Khong the phan biet chac chan 2 truong
     * hop nay, nen xu ly theo huong an toan: thu hoi toan bo phien con hieu luc khac cua tai
     * khoan, ep moi thiet bi (ke ca ke tan cong neu co) phai dang nhap lai bang mat khau
     * that. Tha lam phien nguoi dung that con hon de ke tan cong tiep tuc dung ngam session.
     *
     * <p>{@code noRollbackFor}: nhanh reuse detection o tren ghi nhan revoke roi moi throw
     * BusinessException ngay sau - mac dinh Spring se rollback ca thay doi do, khien token
     * khong bao gio thuc su bi thu hoi.
     *
     * <p>Dung {@code findByTokenHashForUpdate} (SELECT ... FOR UPDATE) thay vi
     * findByTokenHash thuong: chan race condition khi 2 request refresh cung token toi gan
     * nhu dong thoi (vd StrictMode goi effect 2 lan o FE) - neu khong khoa, ca hai co the
     * cung doc duoc trang thai "chua revoke" va cung rotate thanh cong, sinh 2 token con
     * hieu luc tu 1 token goc thay vi 1.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        AuthRefreshToken stored = refreshTokenRepository.findByTokenHashForUpdate(hash(rawRefreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        Instant now = clock.instant();

        if (stored.getRevokedAt() != null) {
            log.warn("Phat hien tai su dung refresh token da bi thu hoi - accountId={}, thu hoi toan bo phien",
                    stored.getAccountId());
            refreshTokenRepository.findByAccountIdAndRevokedAtIsNull(stored.getAccountId())
                    .forEach(token -> token.revoke(now));
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (stored.getExpiresAt().isBefore(now)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        AuthAccount account = accountRepository.findById(stored.getAccountId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        stored.revoke(now);

        return issueTokens(account, rolesOf(account.getId()));
    }

    /**
     * Thu hoi refresh token hien tai. Idempotent va khoan dung voi rawRefreshToken null
     * (client goi logout khi da khong con cookie) - khong bao loi, chi don gian khong co
     * gi de thu hoi.
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .ifPresent(token -> {
                    token.revoke(clock.instant());
                    refreshTokenRepository.save(token);
                });
    }

    /**
     * Idempotent: tai khoan da ACTIVE nghia la nguoi dung bam xac minh lai (vd double-click)
     * - tra ve thanh cong, khong bao loi.
     *
     * <p>Tra cuu ma OTP dinh pham vi theo tai khoan (khong tra cuu toan cuc theo hash):
     * OTP 6 chu so chi co khong gian 10^6 gia tri, tra cuu toan cuc se cho phep ke tan cong
     * doan dai mot ma bat ky va khop trung ma cua bat ky tai khoan nao dang cho xac minh.
     *
     * <p>{@code noRollbackFor}: nhanh sai ma tang attempt_count roi nem BusinessException
     * ngay sau - dung bay da gap o login(). Day la lop chan brute-force chinh cho OTP,
     * mat no la mat luon gioi han 5 lan nhap sai.
     *
     * <p>Thu tu kiem tra: het han va so lan sai duoc kiem truoc, so sanh BCrypt (~100ms)
     * lam sau cung, de mot ma da het han/da bi dot khong ep server ton CPU vo ich.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public void verifyEmail(VerifyEmailRequest request) {
        AuthAccount account = accountRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_VERIFICATION_OTP));

        if (account.getStatus() == AccountStatus.ACTIVE) {
            return;
        }
        if (account.getStatus() != AccountStatus.UNVERIFIED) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_OTP);
        }

        Instant now = clock.instant();
        AuthEmailVerificationToken token = verificationTokenRepository
                .findFirstByAccountIdAndUsedAtIsNullOrderByCreatedAtDesc(account.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_VERIFICATION_OTP));

        if (token.isExpired(now)) {
            throw new BusinessException(ErrorCode.EXPIRED_VERIFICATION_OTP);
        }

        if (!passwordEncoder.matches(request.otp(), token.getTokenHash())) {
            boolean burned = token.registerFailedAttempt(now, MAX_OTP_ATTEMPTS);
            verificationTokenRepository.save(token);
            throw new BusinessException(
                    burned ? ErrorCode.TOO_MANY_OTP_ATTEMPTS : ErrorCode.INVALID_VERIFICATION_OTP);
        }

        token.markUsed(now);
        verificationTokenRepository.save(token);

        account.activate(now);
        accountRepository.save(account);
    }

    /**
     * Khong bao gio bao loi khi email khong ton tai hoac da xac minh, tranh lo thong tin
     * email nao da dang ky (user enumeration). Cung vi ly do do, luon tra ve cung mot
     * {@link ResendVerificationResponse} bat ke email co ton tai, da xac minh, hay dang
     * trong thoi gian cooldown hay khong - phia frontend tu quan ly nut "gui lai" bang
     * dem nguoc 60 giay co dinh, khong dua vao noi dung phan hoi de biet con bao lau.
     *
     * <p>Vo hieu hoa moi ma cu chua dung truoc khi phat ma moi, tranh nhieu ma cung song
     * hieu luc cho cung mot tai khoan.
     */
    @Transactional
    public ResendVerificationResponse resendVerification(ResendVerificationRequest request) {
        Instant now = clock.instant();
        accountRepository.findByEmail(normalizeEmail(request.email()))
                .filter(account -> account.getStatus() == AccountStatus.UNVERIFIED)
                .ifPresent(account -> {
                    boolean withinCooldown = verificationTokenRepository
                            .findFirstByAccountIdAndUsedAtIsNullOrderByCreatedAtDesc(account.getId())
                            .map(latest -> latest.getCreatedAt().plus(OTP_RESEND_COOLDOWN).isAfter(now))
                            .orElse(false);
                    if (withinCooldown) {
                        return;
                    }

                    verificationTokenRepository.findByAccountIdAndUsedAtIsNull(account.getId())
                            .forEach(oldToken -> oldToken.markUsed(now));
                    issueAndDispatchVerificationOtp(account, now);
                });

        return new ResendVerificationResponse((int) OTP_RESEND_COOLDOWN.toSeconds());
    }

    /**
     * Khong bao gio bao loi khi email khong ton tai hay tai khoan dang SUSPENDED, tranh lo
     * thong tin email nao da dang ky (user enumeration) - cung ly do nhu resendVerification.
     * Khac resendVerification o dieu kien loc: ap dung cho moi trang thai tru SUSPENDED (tai
     * khoan bi dinh chi vinh vien khong tu phuc hoi duoc), khong rieng UNVERIFIED - nguoi
     * quen mat khau co the o bat ky trang thai nao khac.
     *
     * <p>Luon tra ve cung mot {@link ForgotPasswordResponse} bat ke nhanh nao o tren co
     * chay hay khong.
     */
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        Instant now = clock.instant();
        accountRepository.findByEmail(normalizeEmail(request.email()))
                .filter(account -> account.getStatus() != AccountStatus.SUSPENDED)
                .ifPresent(account -> {
                    boolean withinCooldown = passwordResetTokenRepository
                            .findFirstByAccountIdAndUsedAtIsNullOrderByCreatedAtDesc(account.getId())
                            .map(latest -> latest.getCreatedAt().plus(OTP_RESEND_COOLDOWN).isAfter(now))
                            .orElse(false);
                    if (withinCooldown) {
                        return;
                    }

                    passwordResetTokenRepository.findByAccountIdAndUsedAtIsNull(account.getId())
                            .forEach(oldToken -> oldToken.markUsed(now));
                    issueAndDispatchPasswordResetOtp(account, now);
                });

        return new ForgotPasswordResponse((int) OTP_RESEND_COOLDOWN.toSeconds());
    }

    /**
     * Xac minh OTP quen mat khau va dat mat khau moi. Cung bay noRollbackFor nhu
     * verifyEmail - nhanh sai OTP tang attempt_count roi nem BusinessException ngay sau,
     * mac dinh Spring se rollback ca thay doi do neu thieu no.
     *
     * <p>Dat lai thanh cong thu hoi toan bo refresh token dang hieu luc cua tai khoan (dang
     * xuat khoi moi thiet bi) - da xac nhan voi nguoi dung, vi ly do dat lai mat khau
     * thuong la nghi ngo lo mat khau, phien cu (co the da bi ke tan cong chiem) khong nen
     * tiep tuc song.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Mật khẩu xác nhận không khớp.");
        }

        AuthAccount account = accountRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_RESET_OTP));

        Instant now = clock.instant();
        AuthPasswordResetToken token = passwordResetTokenRepository
                .findFirstByAccountIdAndUsedAtIsNullOrderByCreatedAtDesc(account.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_RESET_OTP));

        if (token.isExpired(now)) {
            throw new BusinessException(ErrorCode.EXPIRED_RESET_OTP);
        }

        if (!passwordEncoder.matches(request.otp(), token.getTokenHash())) {
            boolean burned = token.registerFailedAttempt(now, MAX_OTP_ATTEMPTS);
            passwordResetTokenRepository.save(token);
            throw new BusinessException(
                    burned ? ErrorCode.TOO_MANY_RESET_OTP_ATTEMPTS : ErrorCode.INVALID_RESET_OTP);
        }

        token.markUsed(now);
        passwordResetTokenRepository.save(token);

        account.resetPassword(passwordEncoder.encode(request.newPassword()), now);
        accountRepository.save(account);

        refreshTokenRepository.findByAccountIdAndRevokedAtIsNull(account.getId())
                .forEach(refreshToken -> refreshToken.revoke(now));
    }

    /**
     * Sinh ma OTP, luu duoi dang BCrypt roi publish su kien yeu cau gui mail. Publish
     * BAT BUOC nam trong transaction dang mo cua caller (register/resendVerification):
     * EmailVerificationRequestedListener dung @TransactionalEventListener(AFTER_COMMIT),
     * chi dang ky duoc khi co transaction dang chay, va chi thuc thi sau khi transaction
     * do commit thanh cong. Nho vay SMTP khong bao gio nam trong transaction nghiep vu.
     */
    private void issueAndDispatchVerificationOtp(AuthAccount account, Instant now) {
        String otp = generateOtp();
        verificationTokenRepository.save(new AuthEmailVerificationToken(
                UUID.randomUUID(), account.getId(), passwordEncoder.encode(otp), now.plus(OTP_TTL), now));
        eventPublisher.publishEvent(
                new EmailVerificationRequestedEvent(account.getId(), account.getEmail(), otp, OTP_TTL));
    }

    /**
     * Cung co che voi issueAndDispatchVerificationOtp, cho luong quen mat khau - xem
     * Javadoc do de biet ly do publish phai nam trong transaction dang mo.
     */
    private void issueAndDispatchPasswordResetOtp(AuthAccount account, Instant now) {
        String otp = generateOtp();
        passwordResetTokenRepository.save(new AuthPasswordResetToken(
                UUID.randomUUID(), account.getId(), passwordEncoder.encode(otp), now.plus(OTP_TTL), now));
        eventPublisher.publishEvent(
                new PasswordResetRequestedEvent(account.getId(), account.getEmail(), otp, OTP_TTL));
    }

    /**
     * SecureRandom#nextInt(int) dung rejection sampling nen phan bo deu voi moi bound -
     * bias chi xuat hien neu lay nextInt() % 1_000_000 hoac chia du tu nextBytes(), khong
     * lam nhu vay o day. Locale.ROOT bat buoc: %06d o mot so locale (vd ar) sinh chu so
     * khong phai ASCII.
     */
    String generateOtp() {
        return String.format(Locale.ROOT, "%06d", secureRandom.nextInt(OTP_BOUND));
    }

    private TokenResponse issueTokens(AuthAccount account, Set<AccountRole> roles) {
        Set<String> roleNames = roles.stream().map(Enum::name).collect(Collectors.toSet());
        String accessToken = tokenProvider.generateAccessToken(account.getId(), roleNames);

        String rawRefreshToken = randomToken();
        Instant now = clock.instant();
        AuthRefreshToken refreshToken = new AuthRefreshToken(UUID.randomUUID(), account.getId(),
                hash(rawRefreshToken), now.plus(refreshTokenTtl), now);
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, rawRefreshToken, "Bearer", tokenProvider.accessTokenTtlSeconds(),
                account.getId(), account.getStatus(), roles);
    }

    private Set<AccountRole> rolesOf(UUID accountId) {
        return accountRoleRepository.findByAccountId(accountId).stream()
                .map(AuthAccountRole::getRole)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return phone.trim();
    }

    private BusinessException duplicateAccountError(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        if (message != null && message.toLowerCase(Locale.ROOT).contains("phone")) {
            return new BusinessException(ErrorCode.PHONE_EXISTS);
        }
        return new BusinessException(ErrorCode.EMAIL_EXISTS);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 khong kha dung", ex);
        }
    }
}
