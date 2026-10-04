package vn.taskconnect.task.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.task.entity.TaskEditChange;

/** Truy xuat du lieu bang task_edit_event_changes (UC07). */
public interface TaskEditChangeRepository extends JpaRepository<TaskEditChange, UUID> {

    /** Toan bo truong da doi trong 1 su kien sua viec, dung thu tu hien thi - dung cho nut "Xem chi tiet thay doi". */
    List<TaskEditChange> findByEventIdOrderBySortOrderAsc(UUID eventId);
}
