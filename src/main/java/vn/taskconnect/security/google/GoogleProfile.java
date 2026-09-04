package vn.taskconnect.security.google;

/**
 * Du lieu rut gon lay tu payload cua mot ID token Google da verify thanh cong - xem
 * GoogleTokenVerifierService.verify(). Khong mang theo ten/anh dai dien: pham vi hien tai
 * chua prefill ho so tu Google (xem AuthService.loginWithGoogle()).
 *
 * @param googleId      claim "sub" - dinh danh Google duy nhat, dung tim/gan AuthAccount.googleId
 * @param email         claim "email", chua chuan hoa (AuthService tu normalize truoc khi dung)
 * @param emailVerified claim "email_verified" - false thi tu choi ngay, khong tao/lien ket tai khoan
 */
public record GoogleProfile(String googleId, String email, boolean emailVerified) {
}
