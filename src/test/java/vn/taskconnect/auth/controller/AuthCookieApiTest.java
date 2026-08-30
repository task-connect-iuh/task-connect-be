package vn.taskconnect.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.taskconnect.notification.infrastructure.EmailSender;
import vn.taskconnect.support.AbstractIntegrationTest;

/**
 * Kiem tra tang he thong cho hop dong cookie httpOnly cua refresh token, theo
 * 16-api-contract.md: refresh token khong duoc lo ra JSON body, chi truyen qua
 * Set-Cookie, gioi han Path=/api/v1/auth.
 *
 * <p>register() khong con cap token/cookie (tai khoan moi tao la UNVERIFIED, va login()
 * gio chan hoan toan trang thai nay) - phai di het luong that: dang ky, lay OTP tu
 * EmailSender (mock), xac minh, roi moi dang nhap de co cookie that su.
 */
@AutoConfigureMockMvc
class AuthCookieApiTest extends AbstractIntegrationTest {

    private static final Pattern SIX_DIGITS = Pattern.compile("\\d{6}");
    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailSender emailSender;

    @Test
    void should_notSetCookie_when_registerSucceeds() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(uniqueEmail())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(cookie().doesNotExist("refresh_token"));
    }

    @Test
    void should_setHttpOnlyCookieAndHideFromBody_when_loginSucceeds() throws Exception {
        String email = uniqueEmail();
        registerAndVerify(email);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().path("refresh_token", "/api/v1/auth"));
    }

    @Test
    void should_rotateCookie_when_refreshWithValidCookie() throws Exception {
        Cookie refreshCookie = loginAndGetRefreshCookie();

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    void should_return401_when_refreshWithoutCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH-401-INVALID_REFRESH_TOKEN"));
    }

    @Test
    void should_return401_when_reusingRevokedCookieAfterRefresh() throws Exception {
        Cookie refreshCookie = loginAndGetRefreshCookie();

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk());

        // Token cu da bi thu hoi (xoay vong) - dung lai phai bi tu choi.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH-401-INVALID_REFRESH_TOKEN"));
    }

    @Test
    void should_clearCookie_when_logout() throws Exception {
        Cookie refreshCookie = loginAndGetRefreshCookie();

        mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refresh_token", 0));
    }

    private Cookie loginAndGetRefreshCookie() throws Exception {
        String email = uniqueEmail();
        registerAndVerify(email);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = result.getResponse().getCookie("refresh_token");
        assertThat(refreshCookie).isNotNull();
        return refreshCookie;
    }

    private void registerAndVerify(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        String otp = captureLatestOtp(email);

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk());
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

    private String registerBody(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\","
                + "\"confirmPassword\":\"" + PASSWORD + "\",\"roles\":[\"TASK_POSTER\"]}";
    }

    private String loginBody(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
    }

    private String uniqueEmail() {
        return "cookie-test-" + UUID.randomUUID() + "@example.com";
    }
}
