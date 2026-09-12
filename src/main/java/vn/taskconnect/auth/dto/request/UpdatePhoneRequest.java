package vn.taskconnect.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * @param phone              So dien thoai MOI o dang dia phuong (0xxxxxxxxx) - phai khop voi
 *                           claim phone_number cua newFirebaseIdToken (sau khi quy doi E.164),
 *                           xem AuthService.updatePhone().
 * @param newFirebaseIdToken ID token Firebase Phone Auth vua xac minh xong cho so MOI o FE
 *                           (xem features/auth/PhoneVerificationFlow.tsx) - backend xac minh
 *                           lai qua Firebase Admin SDK, khong tin tuong ket qua client bao ve.
 * @param oldFirebaseIdToken ID token Firebase Phone Auth xac minh so HIEN TAI - bat buoc khi
 *                           tai khoan da co so dien thoai duoc xac minh truoc do (doi so),
 *                           bo trong khi day la lan dau them so (chua co gi de chung minh
 *                           quyen so huu). Xem AuthService.updatePhone().
 */
public record UpdatePhoneRequest(@NotBlank String phone, @NotBlank String newFirebaseIdToken,
        String oldFirebaseIdToken) {
}
