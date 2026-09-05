package vn.taskconnect.notification.infrastructure;

import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Toan bo noi dung tieng Viet cua email OTP gom ve mot cho (rule 22 -
 * "Chuoi van ban hardcode rai rac - gom vao mot cho de de soat"). Sentence case,
 * ngoi thu hai, khong dau cham than, khong emoji.
 *
 * <p>Public: duoc goi tu package {@code notification.service}.
 */
@Component
public class EmailOtpTemplate {

    public String verificationSubject() {
        return "Mã xác minh TaskConnect của bạn";
    }

    public String verificationBody(String otp, Duration validFor) {
        long minutes = validFor.toMinutes();
        return "Mã xác minh của bạn là: " + otp + "\n\n"
                + "Mã có hiệu lực trong " + minutes + " phút. Nếu bạn không yêu cầu mã này, "
                + "hãy bỏ qua email này.\n\n"
                + "TaskConnect";
    }

    public String passwordResetSubject() {
        return "Mã đặt lại mật khẩu TaskConnect của bạn";
    }

    public String passwordResetBody(String otp, Duration validFor) {
        long minutes = validFor.toMinutes();
        return "Mã đặt lại mật khẩu của bạn là: " + otp + "\n\n"
                + "Mã có hiệu lực trong " + minutes + " phút. Không chia sẻ mã này với bất kỳ ai. "
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.\n\n"
                + "TaskConnect";
    }

    public String emailChangeOldOtpSubject() {
        return "Mã xác minh email hiện tại của bạn";
    }

    public String emailChangeOldOtpBody(String otp, Duration validFor) {
        long minutes = validFor.toMinutes();
        return "Bạn vừa yêu cầu đổi email cho tài khoản TaskConnect. Mã xác minh email hiện tại "
                + "của bạn là: " + otp + "\n\n"
                + "Mã có hiệu lực trong " + minutes + " phút. Nếu bạn không yêu cầu đổi email, "
                + "hãy bỏ qua email này.\n\n"
                + "TaskConnect";
    }

    public String emailChangeNewOtpSubject() {
        return "Mã xác minh email mới của bạn";
    }

    public String emailChangeNewOtpBody(String otp, Duration validFor) {
        long minutes = validFor.toMinutes();
        return "Mã xác minh cho email mới của bạn là: " + otp + "\n\n"
                + "Mã có hiệu lực trong " + minutes + " phút. Nếu bạn không yêu cầu đổi email, "
                + "hãy bỏ qua email này.\n\n"
                + "TaskConnect";
    }

    public String emailChangedOldNoticeSubject() {
        return "Email tài khoản TaskConnect của bạn đã được thay đổi";
    }

    public String emailChangedOldNoticeBody(String maskedNewEmail) {
        return "Bạn đã thay đổi email của mình sang " + maskedNewEmail + ".\n\n"
                + "Nếu không phải bạn thực hiện thay đổi này, hãy liên hệ hỗ trợ ngay.\n\n"
                + "TaskConnect";
    }

    public String emailChangedWelcomeSubject() {
        return "Đổi email thành công";
    }

    public String emailChangedWelcomeBody() {
        return "Chúc mừng bạn đã đổi email thành công. Từ nay bạn đăng nhập TaskConnect bằng "
                + "địa chỉ email này.\n\n"
                + "TaskConnect";
    }
}
