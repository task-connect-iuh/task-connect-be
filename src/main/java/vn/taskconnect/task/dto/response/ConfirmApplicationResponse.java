package vn.taskconnect.task.dto.response;

import java.util.UUID;
import vn.taskconnect.booking.api.PaymentMethod;

/**
 * Ket qua UC11 "Chon nguoi nay" - dung cho POST .../confirm. platformFee/payoutEstimate van chi
 * la SO LIEU XEM TRUOC cho moc giai ngan (ngoai pham vi dot nay, chua co luong Poster xac nhan
 * hoan thanh cong viec) - bookingId gan voi booking status CONFIRMED tu 2026-10-02 (da giu tien
 * that, gia lap, xem BookingStatus), khong phai da giai ngan that. paymentMethod/escrowHeldAmount
 * them 2026-10-02 de FE hien ngay so tien thuc te da tam giu theo phuong thuc Poster vua chon.
 */
public record ConfirmApplicationResponse(
        TaskApplicationResponse application,
        UUID bookingId,
        long feeBaseAmount,
        PaymentMethod paymentMethod,
        long escrowHeldAmount,
        long platformFee,
        long payoutEstimate
) {
}
