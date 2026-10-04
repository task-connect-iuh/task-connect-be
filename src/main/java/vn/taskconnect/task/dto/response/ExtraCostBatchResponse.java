package vn.taskconnect.task.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.taskconnect.task.api.ExtraCostBatchStatus;

/** 1 lan Tasker dang chi phi phat sinh, kem danh sach khoan va ai dang/ai duyet - dung trong ExtraCostMoneySummaryResponse. */
public record ExtraCostBatchResponse(
        UUID id,
        int batchNo,
        ExtraCostBatchStatus status,
        String note,
        List<ExtraCostItemResponse> items,
        long totalAmount,
        UUID submittedByAccountId,
        String submittedByName,
        Instant submittedAt,
        UUID reviewedByAccountId,
        String reviewedByName,
        Instant reviewedAt
) {
}
