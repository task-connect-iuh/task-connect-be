package vn.taskconnect.task.api;

import java.util.Optional;
import java.util.UUID;
import vn.taskconnect.task.api.dto.TaskSummary;

/**
 * Be mat cong khai duy nhat cua module Task. Module khac chi duoc goi qua day, cam import
 * entity trong {@code task.entity} hoac inject repository cua module Task. Mo rong tu ban
 * toi gian ban dau khi module Matching (goi y Tasker + moi lam viec) can doc/doi trang thai
 * Task ma khong duoc inject TaskRepository truc tiep.
 */
public interface TaskFacade {

    /** Doc thong tin toi thieu mot cong viec theo id, rong neu khong ton tai. */
    Optional<TaskSummary> findTask(UUID taskId);

    /**
     * Poster chap nhan mot Tasker cho cong viec nay (dung boi Matching khi Tasker accept
     * mot loi moi, tuong duong hanh vi TaskApplicationService.confirm() da co cho luong ung
     * tuyen) - chuyen Task tu OPEN sang ASSIGNED. Nem BusinessException(TASK_NOT_FOUND) neu
     * task khong ton tai hoac khong thuoc ve posterId, TASK_ALREADY_ASSIGNED neu task khong
     * con OPEN. Goi lai dung logic da co trong module Task (khong tao nhanh doi trang thai
     * song song), xem TaskFacadeImpl.
     */
    void assignTask(UUID taskId, UUID posterId);
}
