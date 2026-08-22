package vn.taskconnect.common.exception;

/**
 * Loi nghiep vu co ma. Moi module nem exception nay thay vi tu dung ResponseEntity loi.
 *
 * <p>{@link GlobalExceptionHandler} chuyen no thanh phan hoi REST thong nhat, lay ma HTTP
 * tu chinh {@link ErrorCode} nen ma HTTP that luon trung phan so trong chuoi ma loi.
 */
public class BusinessException extends RuntimeException {

    private final transient ErrorCode errorCode;
    private final transient Object details;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), null);
    }

    /**
     * Dung khi thong diep mac dinh cua ma loi chua du ro cho nguoi dung trong ngu canh nay.
     */
    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    /**
     * @param details du lieu bo sung tra kem trong truong {@code data} cua phan hoi.
     *                Khong duoc chua du lieu dinh danh ca nhan, token, hay thong tin noi bo.
     */
    public BusinessException(ErrorCode errorCode, String message, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Object details() {
        return details;
    }
}
