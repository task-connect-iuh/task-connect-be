package vn.taskconnect.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import vn.taskconnect.support.AbstractIntegrationTest;

/**
 * Kiem tra tang he thong (MockMvc, qua that ca filter chain va Bean Validation) cho hop
 * dong POST /api/v1/auth/forgot-password va POST /api/v1/auth/reset-password. Mirror
 * AuthVerificationApiTest - chi kiem tra loi validate tang HTTP, luong nghiep vu day du
 * (OTP that) da co o AuthPasswordResetServiceTest.
 */
@AutoConfigureMockMvc
class AuthPasswordResetApiTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return400_when_forgotPasswordEmailMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"khong-phai-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON-400-VALIDATION_FAILED"));
    }

    @Test
    void should_return400_when_resetPasswordOtpIsNotSixDigits() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@example.com\",\"otp\":\"12a456\","
                                + "\"newPassword\":\"MatKhauMoi@123\",\"confirmNewPassword\":\"MatKhauMoi@123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON-400-VALIDATION_FAILED"));
    }

    @Test
    void should_return400_when_resetPasswordNewPasswordTooWeak() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@example.com\",\"otp\":\"123456\","
                                + "\"newPassword\":\"yeuqua\",\"confirmNewPassword\":\"yeuqua\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON-400-VALIDATION_FAILED"));
    }

    @Test
    void should_return400_when_resetPasswordConfirmDoesNotMatch() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@example.com\",\"otp\":\"123456\","
                                + "\"newPassword\":\"MatKhauMoi@123\",\"confirmNewPassword\":\"KhacHoanToan@123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON-400-VALIDATION_FAILED"));
    }
}
