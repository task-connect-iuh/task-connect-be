package vn.taskconnect.booking.api.dto;

import java.util.UUID;
import vn.taskconnect.booking.api.PaymentMethod;
import vn.taskconnect.payment.api.EscrowHoldStatus;

/**
 * Ghep feeBaseAmount/paymentMethod cua Booking voi heldAmount/status tam giu that cua Payment
 * thanh 1 lat cat du de Task tinh "chi phi phat sinh" (TaskExtraCostService) - tra ve qua
 * BookingFacade.getEscrowBreakdown(), dung cho ca GET tong hop lan cac gate submit/topUp.
 */
public record BookingEscrowSummary(
        UUID bookingId,
        long feeBaseAmount,
        PaymentMethod paymentMethod,
        long heldAmount,
        EscrowHoldStatus status
) {
}
