package vn.taskconnect.task.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.task.api.TaskStatus;
import vn.taskconnect.task.entity.Task;

/**
 * Truy xuat du lieu bang task_tasks. Chi module Task duoc inject truc tiep repository nay -
 * module khac phai goi qua TaskFacade.
 */
public interface TaskRepository extends JpaRepository<Task, UUID> {

    /** Danh sach cong viec cua mot Poster, moi dang gan day nhat truoc - dung cho GET /tasks/mine. */
    List<Task> findByPosterIdOrderByCreatedAtDesc(UUID posterId);

    /**
     * Feed cong viec dang mo cho Tasker duyet (UC10, muc don gian - chua Redis Geo/ban kinh,
     * xem docs/TASK-MODULE-SPLIT.md), khong loc category. Loc theo tu khoa lam o tang service
     * (in-memory) vi pham vi con nho, chua can FULLTEXT.
     */
    List<Task> findByStatusOrderByCreatedAtDesc(TaskStatus status);

    /** Nhu tren, co loc them theo danh muc dich vu. */
    List<Task> findByStatusAndCategoryIdOrderByCreatedAtDesc(TaskStatus status, UUID categoryId);

    /** Doc 1 cong viec dung trang thai - dung cho GET /tasks/{taskId}/browse (Tasker xem chi tiet 1 viec dang mo). */
    Optional<Task> findByIdAndStatus(UUID id, TaskStatus status);
}
