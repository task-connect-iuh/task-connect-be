package vn.taskconnect.task.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.task.entity.Task;

/**
 * Truy xuat du lieu bang task_tasks. Chi module Task duoc inject truc tiep repository nay -
 * module khac phai goi qua TaskFacade.
 */
public interface TaskRepository extends JpaRepository<Task, UUID> {

    /** Danh sach cong viec cua mot Poster, moi dang gan day nhat truoc - dung cho GET /tasks/mine. */
    List<Task> findByPosterIdOrderByCreatedAtDesc(UUID posterId);
}
