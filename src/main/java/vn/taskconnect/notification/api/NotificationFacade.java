package vn.taskconnect.notification.api;

import vn.taskconnect.notification.api.dto.EmailChangedNotice;
import vn.taskconnect.notification.api.dto.EmailOtpMessage;

/**
 * Be mat cong khai duy nhat cua module Notification. Module khac chi duoc goi qua day,
 * cam import entity/service/infrastructure ben trong module Notification.
 */
public interface NotificationFacade {

    /**
     * Gui ma xac minh email. Goi dong bo (khong ra JMS/broker), nhung ban than method
     * nay luon duoc goi tu mot luong da tach khoi request HTTP (xem
     * EmailVerificationRequestedListener trong module Auth), nen SMTP khong bao gio nam
     * trong transaction nghiep vu va khong keo dai thoi gian phan hoi API.
     *
     * <p>Khong bao gio nem exception ra ngoai: nguoi goi (listener bat dong bo) khong co
     * cach nao phan hoi loi ve lai request HTTP da tra loi tu truoc. That bai gui mail
     * duoc nuot va ghi log ben trong, nguoi dung khac phuc bang chuc nang gui lai ma.
     */
    void sendEmailVerificationOtp(EmailOtpMessage message);

    /**
     * Gui ma OTP dat lai mat khau. Cung dieu kien goi va cung cam ket khong nem exception
     * nhu {@link #sendEmailVerificationOtp(EmailOtpMessage)} - xem Javadoc do de biet ly do.
     */
    void sendPasswordResetOtp(EmailOtpMessage message);

    /**
     * Gui ma OTP xac minh quyen so huu email HIEN TAI - buoc 1 cua luong doi email. Cung
     * dieu kien goi va cam ket khong nem exception nhu sendEmailVerificationOtp.
     */
    void sendEmailChangeOldOtp(EmailOtpMessage message);

    /**
     * Gui ma OTP xac minh quyen so huu email MOI - buoc 2 cua luong doi email. Cung dieu
     * kien goi va cam ket khong nem exception nhu sendEmailVerificationOtp.
     */
    void sendEmailChangeNewOtp(EmailOtpMessage message);

    /**
     * Gui 2 thong bao sau khi doi email thanh cong: email cu nhan tin bao da doi (kem email
     * moi rut gon), email moi nhan tin chuc mung. Khong nem exception - loi gui mail nao
     * cung chi duoc log, khong lam gian doan luong con lai.
     */
    void sendEmailChangedNotices(EmailChangedNotice notice);
}
