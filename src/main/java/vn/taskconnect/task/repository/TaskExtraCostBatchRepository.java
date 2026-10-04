package vn.taskconnect.task.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.task.api.ExtraCostBatchStatus;
import vn.taskconnect.task.entity.TaskExtraCostBatch;

/**
 * Truy xuat du lieu bang task_extra_cost_batches. Chi module Task duoc inject truc tiep
 * repository nay - module khac khong duoc inject thang, chi doc qua
 * TaskFacade.findExtraCostBatch() (bo sung 2026-10-03, dung boi Chat de hien tin nhan
 * EXTRA_COST_BATCH).
 */
public interface TaskExtraCostBatchRepository extends JpaRepository<TaskExtraCostBatch, UUID> {

    /** Toan bo batch cua 1 application, cu nhat truoc - dung de ghep ExtraCostMoneySummaryResponse. */
    List<TaskExtraCostBatch> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);

    /** 1 batch theo id, kem rang buoc dung application - tranh 1 Tasker/Poster thao tac nham batch cua application khac. */
    Optional<TaskExtraCostBatch> findByIdAndApplicationId(UUID id, UUID applicationId);

    /** Dem tong so batch da tung dang cho 1 application (ke ca da xu ly) - dung tinh batchNo cho batch moi. */
    int countByApplicationId(UUID applicationId);

    /** Co dang batch nao o trang thai da cho truoc (PENDING) cho application nay khong - gate "chi 1 batch dang cho tai 1 thoi diem". */
    boolean existsByApplicationIdAndStatus(UUID applicationId, ExtraCostBatchStatus status);
}
