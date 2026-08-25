package vn.taskconnect.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.taskconnect.auth.dto.request.LoginRequest;
import vn.taskconnect.auth.dto.request.RegisterRequest;
import vn.taskconnect.auth.dto.request.ResendVerificationRequest;
import vn.taskconnect.auth.dto.request.VerifyEmailRequest;
import vn.taskconnect.auth.dto.response.ResendVerificationResponse;
import vn.taskconnect.auth.dto.response.TokenResponse;
import vn.taskconnect.auth.service.AuthService;
import vn.taskconnect.common.response.ApiResponse;
import vn.taskconnect.security.jwt.JwtProperties;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /**
     * Ten va pham vi cookie chua refresh token. Path gioi han dung nhom endpoint auth -
     * cookie khong tu dinh kem o cac request khac (task, booking...), giam dien lo theo
     * 16-api-contract.md.
     */
    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        TokenResponse tokens = authService.register(request);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken()).toString());
        return ApiResponse.ok(tokens);
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        TokenResponse tokens = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken()).toString());
        return ApiResponse.ok(tokens);
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        TokenResponse tokens = authService.refresh(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken()).toString());
        return ApiResponse.ok(tokens);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString());
        return ApiResponse.ok(null);
    }

    /**
     * Dung ResponseCookie thay vi Cookie servlet thuong vi can dat SameSite - lop
     * jakarta.servlet.http.Cookie khong ho tro thuoc tinh nay.
     */
    private ResponseCookie refreshCookie(String rawToken) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(jwtProperties.refreshCookieSecure())
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ofDays(jwtProperties.refreshTokenTtlDays()))
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
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
}
