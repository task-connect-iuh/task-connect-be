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
 * Kiem tra tang he thong (MockMvc, qua that ca filter chain va Bean Validation) cho
 * hop dong moi cua POST /api/v1/auth/verify-email: nhan {email, otp} thay vi {token}.
 */
@AutoConfigureMockMvc
class AuthVerificationApiTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return400_when_otpIsNotSixDigits() throws Exception {
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@example.com\",\"otp\":\"12a456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON-400-VALIDATION_FAILED"));
    }

    @Test
    void should_return400_when_emailMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON-400-VALIDATION_FAILED"));
    }
}
