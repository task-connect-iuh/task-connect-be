package vn.taskconnect.task.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.taskconnect.task.api.ExtraCostBatchStatus;

/**
 * Chi tiet 1 batch "Chi phi phat sinh" (quyet dinh nguoi dung 2026-10-02/03), doc qua
 * {@link vn.taskconnect.task.api.TaskFacade#findExtraCostBatch(UUID)}. Dung boi Chat de hien tin
 * nhan EXTRA_COST_BATCH ngay trong khung chat (giong PRICE_PROPOSAL/refPriceHistoryId) - status/
 * items/totalAmount LUON doc TUOI tai day moi lan hien, khong luu lai trong chat_messages de
 * tranh 2 nguon su that (xem Javadoc ChatMessageResponse).
 */
public record ExtraCostBatchSummary(
        UUID id,
        int batchNo,
        ExtraCostBatchStatus status,
        String note,
        List<Item> items,
        long totalAmount,
        Instant submittedAt,
        Instant reviewedAt
) {

    public record Item(String name, long amount, String photoUrl) {
    }
}
