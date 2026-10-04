package vn.taskconnect.payment.api.dto;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.payment.api.EscrowHoldStatus;

/**
 * Thong tin toi thieu 1 ban ghi tam giu tien, tra cho module khac (Booking) qua PaymentFacade -
 * Booking tu ghep them feeBaseAmount/paymentMethod cua chinh no de tra ve BookingEscrowSummary
 * day du cho Task/Chat dung.
 */
public record EscrowHoldSummary(
        UUID id,
        UUID bookingId,
        long heldAmount,
        EscrowHoldStatus status,
        Instant updatedAt
) {
}
