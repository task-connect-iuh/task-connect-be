package vn.taskconnect.task.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.task.entity.TaskApplication;

/**
 * Truy xuat du lieu bang task_applications. Chi module Task duoc inject truc tiep repository
 * nay - module khac phai goi qua TaskFacade (hien chua co method nao lo du lieu ung tuyen ra
 * ngoai module).
 */
public interface TaskApplicationRepository extends JpaRepository<TaskApplication, UUID> {

    /** Toan bo don ung tuyen cua mot cong viec - dung cho Poster xem danh sach ung vien. */
    List<TaskApplication> findByTaskId(UUID taskId);

    /** Tra ve don ung tuyen dung task+tasker neu co - dung de chan ung tuyen trung. */
    Optional<TaskApplication> findByTaskIdAndTaskerId(UUID taskId, UUID taskerId);

    /** Toan bo don ung tuyen cua mot Tasker, moi gui gan day nhat truoc - dung cho "Viec da nhan". */
    List<TaskApplication> findByTaskerIdOrderByCreatedAtDesc(UUID taskerId);

    /** Lay dung 1 don theo id, kem rang buoc thuoc ve dung task - dung cho confirm/reject. */
    Optional<TaskApplication> findByIdAndTaskId(UUID id, UUID taskId);
}
