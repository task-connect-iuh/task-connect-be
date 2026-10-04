package vn.taskconnect.task.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.task.entity.TaskExtraCostItem;

/**
 * Truy xuat du lieu bang task_extra_cost_items. Chi module Task duoc inject truc tiep repository
 * nay.
 */
public interface TaskExtraCostItemRepository extends JpaRepository<TaskExtraCostItem, UUID> {

    /** Toan bo khoan cua 1 batch, dung thu tu Tasker da nhap - dung khi hien chi tiet 1 batch. */
    List<TaskExtraCostItem> findByBatchIdOrderBySortOrderAsc(UUID batchId);

    /** Toan bo khoan cua nhieu batch cung luc (batch fetch) - dung khi ghep ExtraCostMoneySummaryResponse, tranh N+1 query. */
    List<TaskExtraCostItem> findByBatchIdInOrderBySortOrderAsc(List<UUID> batchIds);
}
