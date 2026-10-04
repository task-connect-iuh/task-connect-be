package vn.taskconnect.task.dto.request;

import jakarta.validation.constraints.NotNull;
import vn.taskconnect.booking.api.PaymentMethod;

/**
 * Yeu cau UC11 "Chon nguoi nay" - Poster bat buoc chon 1 trong 2 phuong thuc thanh toan truoc
 * khi xac nhan (quyet dinh nguoi dung 2026-10-02), xem Javadoc PaymentMethod.
 */
public record ConfirmApplicationRequest(@NotNull PaymentMethod paymentMethod) {
}
