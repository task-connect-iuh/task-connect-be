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
}
