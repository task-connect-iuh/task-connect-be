package vn.taskconnect.task.dto.response;

import java.util.UUID;

/**
 * Ket qua UC11 "Chon nguoi nay" - dung cho POST .../confirm. platformFee/payoutEstimate chi
 * la SO LIEU XEM TRUOC (chua co module Payment/escrow that giu tien) - bookingId luon gan voi
 * booking status PENDING_ESCROW, khong phai da giai ngan. Xem dac ta muc 4 va 7.
 */
public record ConfirmApplicationResponse(
        TaskApplicationResponse application,
        UUID bookingId,
        long feeBaseAmount,
        long platformFee,
        long payoutEstimate
) {
}
