package vn.taskconnect.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.taskconnect.auth.dto.request.ChangePasswordRequest;
import vn.taskconnect.auth.dto.request.ForgotPasswordRequest;
import vn.taskconnect.auth.dto.request.GrantAdminRoleRequest;
import vn.taskconnect.auth.dto.request.LoginRequest;
import vn.taskconnect.auth.dto.request.RegisterRequest;
import vn.taskconnect.auth.dto.request.ResendVerificationRequest;
import vn.taskconnect.auth.dto.request.ResetPasswordRequest;
import vn.taskconnect.auth.dto.request.RevokeAdminRoleRequest;
import vn.taskconnect.auth.dto.request.VerifyEmailRequest;
import vn.taskconnect.auth.dto.response.ForgotPasswordResponse;
import vn.taskconnect.auth.dto.response.ResendVerificationResponse;
import vn.taskconnect.auth.dto.response.TokenResponse;
import vn.taskconnect.auth.service.AuthService;
import vn.taskconnect.common.response.ApiResponse;
import vn.taskconnect.security.jwt.AuthenticatedPrincipal;
import vn.taskconnect.security.jwt.JwtProperties;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /**
     * Ten va pham vi cookie chua refresh token. Path gioi han dung nhom endpoint auth -
     * cookie khong tu dinh kem o cac request khac (task, booking...), giam dien lo theo
     * 16-api-contract.md.
     *
     * <p>Ten cookie phan biet theo header {@code X-App} ("admin" hoac "client", moi FE tu
     * gan vao moi request toi /auth/*) - {@code refresh_token_admin} va {@code
     * refresh_token_client}. Ca hai app deu goi chung mot backend o cung origin
     * (localhost luc dev, cung mot domain API luc production), nen neu dung chung MOT ten
     * cookie, dang nhap o app nay se ghi de refresh token cua app kia trong cung trinh
     * duyet - dang nhap admin roi dang nhap client (hoac nguoc lai) se lam phien con lai
     * bi "danh cap" cookie va bi coi la refresh-token-reuse, tu dong bi thu hoi toan bo
     * phien. Tach ten cookie theo app giai quyet dut diem, khong can subdomain rieng.
     * Header thieu hoac gia tri la la mac dinh ve "client".
     */
    private static final String REFRESH_COOKIE_PREFIX = "refresh_token_";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";
    private static final String APP_HEADER = "X-App";
    private static final String DEFAULT_APP = "client";
    private static final Set<String> KNOWN_APPS = Set.of("admin", "client");

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok(null, "Đăng ký thành công. Vui lòng kiểm tra email để lấy mã xác minh.");
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request,
            @RequestHeader(name = APP_HEADER, required = false) String app, HttpServletResponse response) {
        TokenResponse tokens = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(app, tokens.refreshToken()).toString());
        return ApiResponse.ok(tokens);
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(
            @RequestHeader(name = APP_HEADER, required = false) String app,
            HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = readRefreshCookie(request, app);
        TokenResponse tokens = authService.refresh(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(app, tokens.refreshToken()).toString());
        return ApiResponse.ok(tokens);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(name = APP_HEADER, required = false) String app,
            HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = readRefreshCookie(request, app);
        authService.logout(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshCookie(app).toString());
        return ApiResponse.ok(null);
    }

    /** Header X-App thieu hoac gia tri la ("khong phai admin/client") deu coi nhu "client". */
    private String resolveApp(String app) {
        return (app != null && KNOWN_APPS.contains(app)) ? app : DEFAULT_APP;
    }

    private String cookieName(String app) {
        return REFRESH_COOKIE_PREFIX + resolveApp(app);
    }

    private String readRefreshCookie(HttpServletRequest request, String app) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String name = cookieName(app);
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Dung ResponseCookie thay vi Cookie servlet thuong vi can dat SameSite - lop
     * jakarta.servlet.http.Cookie khong ho tro thuoc tinh nay.
     */
    private ResponseCookie refreshCookie(String app, String rawToken) {
        return ResponseCookie.from(cookieName(app), rawToken)
                .httpOnly(true)
                .secure(jwtProperties.refreshCookieSecure())
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ofDays(jwtProperties.refreshTokenTtlDays()))
                .build();
    }

    private ResponseCookie clearRefreshCookie(String app) {
        return ResponseCookie.from(cookieName(app), "")
                .httpOnly(true)
                .secure(jwtProperties.refreshCookieSecure())
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ApiResponse.ok(null, "Xác minh email thành công.");
    }

    @PostMapping("/resend-verification")
    public ApiResponse<ResendVerificationResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        ResendVerificationResponse response = authService.resendVerification(request);
        return ApiResponse.ok(response,
                "Nếu email của bạn đang chờ xác minh, mã xác minh mới đã được gửi. Mã có hiệu lực trong 5 phút.");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = authService.forgotPassword(request);
        return ApiResponse.ok(response,
                "Nếu email của bạn tồn tại trong hệ thống, mã đặt lại mật khẩu đã được gửi. "
                        + "Mã có hiệu lực trong 5 phút.");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok(null, "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.");
    }

    /**
     * Doi mat khau khi da dang nhap. Nam duoi /api/v1/auth (PUBLIC_ENDPOINTS o SecurityConfig
     * cho phep moi request toi day di qua tang filter chain), nhung @PreAuthorize van chan
     * dung request khong kem access token hop le - JwtAuthenticationFilter luon chay truoc,
     * doc Authorization header bat ke duong dan co nam trong PUBLIC_ENDPOINTS hay khong.
     * Thu hoi het refresh token (xem AuthService.changePassword) nen xoa luon cookie phien
     * hien tai, giong het cach logout() dang lam.
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader(name = APP_HEADER, required = false) String app, HttpServletResponse response) {
        authService.changePassword(principal.accountId(), request);
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshCookie(app).toString());
        return ApiResponse.ok(null, "Đổi mật khẩu thành công. Vui lòng đăng nhập lại.");
    }

    /**
     * Chi super-admin: gan role ADMIN cho tai khoan co email trong request. Yeu cau
     * hasRole('ADMIN') o day chi loc bot request tu tai khoan khong phai admin nao ca -
     * kiem tra "co dung la super-admin hay khong" nam o AuthService.grantAdminRole(), vi
     * admin thuong cung mang ROLE_ADMIN nhung khong duoc phep goi endpoint nay.
     */
    @PostMapping("/admins/grant")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> grantAdminRole(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody GrantAdminRoleRequest request) {
        authService.grantAdminRole(principal.accountId(), request);
        return ApiResponse.ok(null, "Đã gán quyền quản trị.");
    }

    /** Chi super-admin: thu hoi role ADMIN cua tai khoan co email trong request. */
    @PostMapping("/admins/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> revokeAdminRole(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody RevokeAdminRoleRequest request) {
        authService.revokeAdminRole(principal.accountId(), request);
        return ApiResponse.ok(null, "Đã thu hồi quyền quản trị.");
    }
}
