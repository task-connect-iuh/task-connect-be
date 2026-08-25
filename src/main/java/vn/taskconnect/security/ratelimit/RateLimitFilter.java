package vn.taskconnect.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.common.response.ApiResponse;

/**
 * Chan spam/brute-force theo IP cho cac endpoint cong khai de bi lam dung (dang ky, xac minh
 * email, gui lai email xac minh). Khong dat o tang Gateway rieng vi kien truc la modular
 * monolith, khong co Gateway (xem .claude/rules/00-architecture.md).
 *
 * <p>Dung request.getRemoteAddr() thay vi tin header X-Forwarded-For - he thong khong co
 * reverse proxy nao dat header nay, tin no se cho phep gia mao IP de vuot rate limit.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Map<String, Rule> RULES = Map.of(
            "/api/v1/auth/register", new Rule(5, Duration.ofHours(1)),
            "/api/v1/auth/verify-email", new Rule(10, Duration.ofHours(1)),
            "/api/v1/auth/resend-verification", new Rule(5, Duration.ofHours(1))
    );

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Rule rule = RULES.get(request.getRequestURI());
        if (rule != null) {
            String key = "ratelimit:" + request.getRequestURI() + ":" + request.getRemoteAddr();
            if (!rateLimiter.allow(key, rule.limit(), rule.window())) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                ApiResponse<Object> body = ApiResponse.error(
                        ErrorCode.RATE_LIMIT_EXCEEDED.code(), ErrorCode.RATE_LIMIT_EXCEEDED.defaultMessage());
                objectMapper.writeValue(response.getWriter(), body);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private record Rule(int limit, Duration window) {
    }
}
