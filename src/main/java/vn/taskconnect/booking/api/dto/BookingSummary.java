package vn.taskconnect.booking.api.dto;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.booking.api.BookingStatus;
import vn.taskconnect.booking.api.PaymentMethod;

/**
 * Thong tin toi thieu 1 booking-lite, tra cho module khac qua BookingFacade (vd Chat can biet
 * bookingId de gan vao chat_channels.booking_id). paymentMethod them 2026-10-02 de Chat/Task biet
 * duoc phuong thuc thanh toan da chon luc UC11 (vd dieu kien hien nut "Chi phi phat sinh" chi khi
 * FULL_ESCROW, xem Javadoc PaymentMethod).
 */
public record BookingSummary(
        UUID id,
        UUID applicationId,
        UUID taskId,
        UUID posterId,
        UUID taskerId,
        long feeBaseAmount,
        PaymentMethod paymentMethod,
        BookingStatus status,
        Instant scheduledAt
) {
}
