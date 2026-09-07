package vn.taskconnect.task.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.task.entity.TaskImage;

/**
 * Truy xuat du lieu bang task_task_images. Chi module Task duoc inject truc tiep repository
 * nay - module khac phai goi qua TaskFacade.
 */
public interface TaskImageRepository extends JpaRepository<TaskImage, UUID> {

    /** Anh cua mot cong viec, dung dung thu tu hien thi - dung cho xem chi tiet 1 cong viec. */
    List<TaskImage> findByTaskIdOrderByDisplayOrderAsc(UUID taskId);

    /** Anh cua nhieu cong viec cung luc theo lo, tranh N+1 khi liet ke danh sach GET /tasks/mine. */
    List<TaskImage> findByTaskIdInOrderByDisplayOrderAsc(Collection<UUID> taskIds);
}
