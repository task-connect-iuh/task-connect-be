package vn.taskconnect.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.common.response.ApiResponse;

/**
 * Exception phat sinh trong filter chain cua Spring Security xay ra truoc DispatcherServlet
 * nen khong toi duoc GlobalExceptionHandler (xem ghi chu trong file do). Lop nay dam bao
 * request thieu/sai JWT van tra ve dung dinh dang ApiResponse thay vi trang loi mac dinh
 * cua Spring Security.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Object> body = ApiResponse.error(
                ErrorCode.UNAUTHENTICATED.code(), ErrorCode.UNAUTHENTICATED.defaultMessage());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
