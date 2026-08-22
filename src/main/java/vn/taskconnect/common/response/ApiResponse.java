package vn.taskconnect.common.response;

import java.time.Instant;

/**
 * Vo boc thong nhat cho moi phan hoi REST cua he thong.
 *
 * <p>Truong {@code null} bi loai khoi JSON nho cau hinh
 * {@code spring.jackson.default-property-inclusion=non_null}, nen phan hoi thanh cong
 * khong kem {@code errorCode}, va phan hoi loi khong kem {@code data} neu khong co gi de kem.
 *
 * <p>{@code errorCode} luon theo dinh dang {@code PREFIX-HTTPCODE-REASON}, lay tu
 * {@link vn.taskconnect.common.exception.ErrorCode}. Phan so trong ma phai trung ma HTTP
 * that cua phan hoi.
 *
 * @param success  true khi nghiep vu thanh cong
 * @param message  thong diep tieng Viet huong toi nguoi dung, co the null
 * @param data     du lieu tra ve, hoac chi tiet bo sung cua loi
 * @param errorCode ma loi, chi co mat khi {@code success} la false
 * @param timestamp thoi diem tao phan hoi, theo UTC
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String errorCode,
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, message, null, errorCode, Instant.now());
    }

    /**
     * Dung khi loi can kem chi tiet, vi du danh sach loi validation theo tung field.
     * Chi tiet dat trong {@code data}, khong duoc chua du lieu nhay cam.
     */
    public static <T> ApiResponse<T> error(String errorCode, String message, T data) {
        return new ApiResponse<>(false, message, data, errorCode, Instant.now());
    }
}
