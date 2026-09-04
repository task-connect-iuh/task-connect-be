package vn.taskconnect.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Than request cho ca 2 endpoint dang nhap Google: POST /auth/google (lan dau, co the can
 * xac nhan lien ket) va POST /auth/google/confirm-link (xac nhan lien ket sau khi nguoi dung
 * dong y o man hoi lai). idToken la ID token JWT do Google Identity Services phat, FE lay tu
 * @react-oauth/google, chua tung qua xu ly hay giai ma o phia FE.
 */
public record GoogleLoginRequest(
        @NotBlank String idToken
) {
}
