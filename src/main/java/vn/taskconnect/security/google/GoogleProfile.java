package vn.taskconnect.security.google;

/**
 * Du lieu rut gon lay tu payload cua mot ID token Google da verify thanh cong - xem
 * GoogleTokenVerifierService.verify().
 *
 * @param googleId      claim "sub" - dinh danh Google duy nhat, dung tim/gan AuthAccount.googleId
 * @param email         claim "email", chua chuan hoa (AuthService tu normalize truoc khi dung)
 * @param emailVerified claim "email_verified" - false thi tu choi ngay, khong tao/lien ket tai khoan
 * @param name          claim "name" (ten hien thi Google) - co the null neu Google khong tra ve
 *                       claim nay (vd. token duoc phat voi scope thu hep); AuthService.
 *                       createGoogleAccount() fallback ve email khi null/rong.
 */
public record GoogleProfile(String googleId, String email, boolean emailVerified, String name) {
}
