package vn.taskconnect.security.firebase;

/**
 * Du lieu rut gon lay tu mot Firebase ID token da verify thanh cong - xem
 * FirebaseTokenVerifierService.verify().
 *
 * @param uid         claim "sub"/"uid" - dinh danh nguoi dung Firebase (rieng biet voi
 *                    AuthAccount.id cua TaskConnect, khong luu lai o dau ca, chi dung de log).
 * @param phoneNumber claim "phone_number", luon o dang E.164 (vd "+84901234567") - day la
 *                    bang chung da xac minh qua Firebase Phone Auth (test-number whitelist
 *                    hoac SMS that), AuthService.updatePhone() doi chieu claim nay voi so
 *                    nguoi dung nhap truoc khi cho phep luu.
 */
public record FirebasePhoneProfile(String uid, String phoneNumber) {
}
