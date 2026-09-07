package vn.taskconnect.task.api;

import java.util.Optional;
import java.util.UUID;
import vn.taskconnect.task.api.dto.TaskSummary;

/**
 * Be mat cong khai duy nhat cua module Task. Module khac chi duoc goi qua day, cam import
 * entity trong {@code task.entity} hoac inject repository cua module Task. Dot 1 (dang viec
 * toi gian) chua module nao goi toi (Matching/Booking chua ton tai) - tao khung toi thieu
 * truoc theo dung convention da dung cho AuthFacade/UserFacade, mo rong dan khi can.
 */
public interface TaskFacade {

    /** Doc thong tin toi thieu mot cong viec theo id, rong neu khong ton tai. */
    Optional<TaskSummary> findTask(UUID taskId);
}
