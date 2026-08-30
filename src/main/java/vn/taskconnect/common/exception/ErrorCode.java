package vn.taskconnect.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Bang ma loi cua he thong.
 *
 * <p>Dinh dang {@code PREFIX-HTTPCODE-REASON}, {@code REASON} viet hoa va gach duoi.
 * Phan so trong ma phai trung {@link #status} — day la rang buoc bat buoc cua hop dong API,
 * duoc kiem tra bang unit test chu khong dua vao mat nguoi.
 *
 * <p>Prefix theo module: AUTH, USR, TSK, MATCH, BKG, PAY, RVW, CHT, NTF, ADM, AI.
 * Prefix {@code COMMON} danh cho loi ky thuat dung chung, khong thuoc module nao.
 *
 * <p>Them ma moi thi bo sung vao {@code docs/ERROR-CODES.md} cung luc, khong de ma song
 * le trong code.
 */
public enum ErrorCode {

    // --- COMMON: loi ky thuat dung chung ---
    VALIDATION_FAILED("COMMON-400-VALIDATION_FAILED", HttpStatus.BAD_REQUEST,
            "Dữ liệu gửi lên không hợp lệ."),
    MALFORMED_REQUEST("COMMON-400-MALFORMED_REQUEST", HttpStatus.BAD_REQUEST,
            "Nội dung yêu cầu không đọc được."),
    UNAUTHENTICATED("COMMON-401-UNAUTHENTICATED", HttpStatus.UNAUTHORIZED,
            "Bạn cần đăng nhập để tiếp tục."),
    FORBIDDEN("COMMON-403-FORBIDDEN", HttpStatus.FORBIDDEN,
            "Bạn không có quyền thực hiện thao tác này."),
    NOT_FOUND("COMMON-404-NOT_FOUND", HttpStatus.NOT_FOUND,
            "Không tìm thấy dữ liệu yêu cầu."),
    METHOD_NOT_ALLOWED("COMMON-405-METHOD_NOT_ALLOWED", HttpStatus.METHOD_NOT_ALLOWED,
            "Phương thức không được hỗ trợ cho đường dẫn này."),
    DATA_CONFLICT("COMMON-409-DATA_CONFLICT", HttpStatus.CONFLICT,
            "Dữ liệu bị xung đột, vui lòng tải lại và thử lại."),
    RATE_LIMIT_EXCEEDED("COMMON-429-RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS,
            "Bạn thao tác quá nhanh, vui lòng thử lại sau."),
    INTERNAL_ERROR("COMMON-500-INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR,
            "Hệ thống đang gặp sự cố, vui lòng thử lại sau."),

    // --- AUTH ---
    EMAIL_EXISTS("AUTH-409-EMAIL_EXISTS", HttpStatus.CONFLICT,
            "Email này đã được đăng ký."),
    PHONE_EXISTS("AUTH-409-PHONE_EXISTS", HttpStatus.CONFLICT,
            "Số điện thoại này đã được đăng ký."),
    ACCOUNT_LOCKED("AUTH-423-ACCOUNT_LOCKED", HttpStatus.LOCKED,
            "Tài khoản đang bị khoá tạm thời."),
    ACCOUNT_SUSPENDED("AUTH-403-ACCOUNT_SUSPENDED", HttpStatus.FORBIDDEN,
            "Tài khoản đã bị đình chỉ."),
    EMAIL_NOT_VERIFIED("AUTH-403-EMAIL_NOT_VERIFIED", HttpStatus.FORBIDDEN,
            "Tài khoản chưa xác thực email. Vui lòng xác minh email trước khi đăng nhập."),
    INVALID_CREDENTIALS("AUTH-401-INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED,
            "Email hoặc mật khẩu không đúng."),
    INVALID_REFRESH_TOKEN("AUTH-401-INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED,
            "Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại."),
    INVALID_VERIFICATION_OTP("AUTH-400-INVALID_VERIFICATION_OTP", HttpStatus.BAD_REQUEST,
            "Mã xác minh không đúng. Bạn có thể nhập lại hoặc yêu cầu mã mới."),
    EXPIRED_VERIFICATION_OTP("AUTH-410-EXPIRED_VERIFICATION_OTP", HttpStatus.GONE,
            "Mã xác minh đã hết hạn. Bạn có thể yêu cầu mã mới."),
    TOO_MANY_OTP_ATTEMPTS("AUTH-429-TOO_MANY_OTP_ATTEMPTS", HttpStatus.TOO_MANY_REQUESTS,
            "Bạn đã nhập sai mã quá nhiều lần. Mã hiện tại không còn hiệu lực, hãy yêu cầu mã mới."),
    INVALID_RESET_OTP("AUTH-400-INVALID_RESET_OTP", HttpStatus.BAD_REQUEST,
            "Mã đặt lại mật khẩu không đúng. Bạn có thể nhập lại hoặc yêu cầu mã mới."),
    EXPIRED_RESET_OTP("AUTH-410-EXPIRED_RESET_OTP", HttpStatus.GONE,
            "Mã đặt lại mật khẩu đã hết hạn. Bạn có thể yêu cầu mã mới."),
    TOO_MANY_RESET_OTP_ATTEMPTS("AUTH-429-TOO_MANY_RESET_OTP_ATTEMPTS", HttpStatus.TOO_MANY_REQUESTS,
            "Bạn đã nhập sai mã quá nhiều lần. Mã hiện tại không còn hiệu lực, hãy yêu cầu mã mới."),

    // --- USR ---
    MISSING_OPERATING_AREA("USR-400-MISSING_OPERATING_AREA", HttpStatus.BAD_REQUEST,
            "Bạn chưa khai báo khu vực hoạt động."),

    // --- TSK ---
    MISSING_LOCATION("TSK-400-MISSING_LOCATION", HttpStatus.BAD_REQUEST,
            "Công việc cần có địa điểm."),
    TASK_ALREADY_ASSIGNED("TSK-409-ALREADY_ASSIGNED", HttpStatus.CONFLICT,
            "Công việc đã được giao cho Tasker khác."),
    CANNOT_CANCEL_ASSIGNED("TSK-409-CANNOT_CANCEL_ASSIGNED", HttpStatus.CONFLICT,
            "Không thể huỷ công việc đã được giao."),
    FIELD_LOCKED_HAS_APPLICANTS("TSK-409-FIELD_LOCKED_HAS_APPLICANTS", HttpStatus.CONFLICT,
            "Không thể sửa thông tin này khi đã có người ứng tuyển."),

    // --- MATCH ---
    NO_TASKER_FOUND("MATCH-404-NO_TASKER_FOUND", HttpStatus.NOT_FOUND,
            "Chưa tìm được Tasker phù hợp."),

    // --- RVW ---
    TASK_NOT_COMPLETED("RVW-409-TASK_NOT_COMPLETED", HttpStatus.CONFLICT,
            "Chỉ đánh giá được khi công việc đã hoàn tất."),

    // --- PAY ---
    INSUFFICIENT_BALANCE("PAY-402-INSUFFICIENT_BALANCE", HttpStatus.PAYMENT_REQUIRED,
            "Số dư ví không đủ để thực hiện giao dịch.");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
