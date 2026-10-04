package vn.taskconnect.task.dto.response;

import java.util.List;
import vn.taskconnect.booking.api.PaymentMethod;

/**
 * Toan bo buc tranh tien cua 1 application sau khi ASSIGNED: chi phi chot ban dau + cac batch
 * phat sinh da duyet + batch dang cho (neu co) + so sanh voi so dang tam giu that - dung cho man
 * hinh "Tien cua viec nay" trong chat va TaskDetailDialog. CHI co y nghia day du khi
 * paymentMethod = FULL_ESCROW (xem TaskExtraCostService) - FE an han nut "Chi phi phat sinh" khi
 * FEE_ONLY_ESCROW, nhung API nay van tra ve duoc (approvedBatches/pendingBatch luon rong) de FE
 * co the hien thi phan "Khi giai ngan" don gian cho PA2 neu can.
 */
public record ExtraCostMoneySummaryResponse(
        long feeBaseAmount,
        PaymentMethod paymentMethod,
        long heldAmount,
        List<ExtraCostBatchResponse> approvedBatches,
        ExtraCostBatchResponse pendingBatch,
        long approvedExtraTotal,
        long requiredTotal,
        long deltaNeeded
) {
}
