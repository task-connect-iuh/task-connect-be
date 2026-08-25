package vn.taskconnect.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.taskconnect.support.AbstractIntegrationTest;

/**
 * Kiem tra tang he thong cho hop dong cookie httpOnly cua refresh token, theo
 * 16-api-contract.md: refresh token khong duoc lo ra JSON body, chi truyen qua
 * Set-Cookie, gioi han Path=/api/v1/auth.
 */
@AutoConfigureMockMvc
class AuthCookieApiTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_setHttpOnlyCookieAndHideFromBody_when_registerSucceeds() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(uniqueEmail())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().path("refresh_token", "/api/v1/auth"));
    }

    @Test
    void should_rotateCookie_when_refreshWithValidCookie() throws Exception {
        Cookie refreshCookie = registerAndGetRefreshCookie();

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
        Cookie refreshCookie = registerAndGetRefreshCookie();

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk());

        // Token cu da bi thu hoi (xoay vong) - dung lai phai bi tu choi.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH-401-INVALID_REFRESH_TOKEN"));
    }

    @Test
    void should_clearCookie_when_logout() throws Exception {
        Cookie refreshCookie = registerAndGetRefreshCookie();

        mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refresh_token", 0));
    }

    private Cookie registerAndGetRefreshCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(uniqueEmail())))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie refreshCookie = result.getResponse().getCookie("refresh_token");
        assertThat(refreshCookie).isNotNull();
        return refreshCookie;
    }

    private String registerBody(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"Password123\","
                + "\"confirmPassword\":\"Password123\",\"roles\":[\"TASK_POSTER\"]}";
    }

    private String uniqueEmail() {
        return "cookie-test-" + UUID.randomUUID() + "@example.com";
    }
}
