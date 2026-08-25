package vn.taskconnect.security.jwt;

import java.util.Set;
import java.util.UUID;

/**
 * Principal dat vao SecurityContext sau khi JwtAuthenticationFilter xac thuc thanh cong.
 * Chi mang accountId va ten vai tro (String) - khong phu thuoc kieu du lieu noi bo cua
 * module Auth, giu security/ tach biet khoi tung module nghiep vu.
 */
public record AuthenticatedPrincipal(UUID accountId, Set<String> roles) {
}
