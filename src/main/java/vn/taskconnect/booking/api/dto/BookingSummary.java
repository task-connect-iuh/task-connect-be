package vn.taskconnect.booking.api.dto;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.booking.api.BookingStatus;

/**
 * Thong tin toi thieu 1 booking-lite, tra cho module khac qua BookingFacade (vd Chat can biet
 * bookingId de gan vao chat_channels.booking_id).
 */
public record BookingSummary(
        UUID id,
        UUID applicationId,
        UUID taskId,
        UUID posterId,
        UUID taskerId,
        long feeBaseAmount,
        BookingStatus status,
        Instant scheduledAt
) {
}
