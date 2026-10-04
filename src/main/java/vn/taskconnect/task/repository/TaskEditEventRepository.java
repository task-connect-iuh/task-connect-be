package vn.taskconnect.task.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.task.entity.TaskEditEvent;

/** Truy xuat du lieu bang task_edit_events (UC07). Tra cuu chi theo id, khong can query rieng. */
public interface TaskEditEventRepository extends JpaRepository<TaskEditEvent, UUID> {
}
