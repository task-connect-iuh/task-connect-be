package vn.taskconnect.task.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.taskconnect.task.api.TaskApplicationStatus;
import vn.taskconnect.task.entity.TaskApplication;

/**
 * Truy xuat du lieu bang task_applications. Chi module Task duoc inject truc tiep repository
 * nay - module khac phai goi qua TaskFacade.
 */
public interface TaskApplicationRepository extends JpaRepository<TaskApplication, UUID> {

    /** Toan bo don ung tuyen cua mot cong viec - dung cho Poster xem danh sach ung vien. */
    List<TaskApplication> findByTaskId(UUID taskId);

    /**
     * Toan bo don (moi trang thai, co the nhieu dong theo thoi gian tu Round B5 sua lai - xem
     * V33__drop_unique_task_applications_task_tasker.sql) cho 1 cap task+tasker, dung 1 trong
     * cac trang thai da cho - dung de kiem tra "co dong nao dang chan ung tuyen/moi lai khong"
     * (ACTIVE hoac DECLINED/REJECTED, xem TaskApplicationService.requireNoBlockingApplication).
     */
    List<TaskApplication> findByTaskIdAndTaskerIdAndStatusIn(UUID taskId, UUID taskerId,
            Collection<TaskApplicationStatus> statuses);

    /** Toan bo don ung tuyen cua mot Tasker, moi gui gan day nhat truoc - dung cho "Viec da nhan". */
    List<TaskApplication> findByTaskerIdOrderByCreatedAtDesc(UUID taskerId);

    /**
     * Toan bo don cua 1 Tasker dang dung 1 trong cac trang thai da cho - dung de suy ra tap
     * taskId can loai khoi feed tim viec (TaskApplicationService.browseOpenTasks), tai su dung
     * cung dinh nghia BLOCKING_APPLICATION_STATUSES voi requireNoBlockingApplication (da ung
     * tuyen/dang cho hoac tung bi tu choi thi khong con thay lai trong feed).
     */
    List<TaskApplication> findByTaskerIdAndStatusIn(UUID taskerId, Collection<TaskApplicationStatus> statuses);

    /** Lay dung 1 don theo id, kem rang buoc thuoc ve dung task - dung cho confirm/reject. */
    Optional<TaskApplication> findByIdAndTaskId(UUID id, UUID taskId);

    /** Lay dung 1 don theo id, kem rang buoc thuoc ve dung task va dung Tasker - dung cho withdraw. */
    Optional<TaskApplication> findByIdAndTaskIdAndTaskerId(UUID id, UUID taskId, UUID taskerId);

    /**
     * Toan bo applicationId ma accountId la Poster (chu task, qua JOIN noi bo trong cung
     * module Task) hoac Tasker (nguoi gui don) - dung cho TaskFacade.listApplicationIdsForAccount,
     * de Chat liet ke Inbox ma khong phai tu query bang task_tasks (cam JOIN xuyen module).
     */
    @Query("SELECT a.id FROM TaskApplication a JOIN Task t ON t.id = a.taskId "
            + "WHERE a.taskerId = :accountId OR t.posterId = :accountId")
    List<UUID> findApplicationIdsInvolvingAccount(@Param("accountId") UUID accountId);

    /** So don dang dung 1 trang thai cho 1 task - dung de dem so loi moi INVITED dang cho (TSK-409-INVITE_LIMIT_REACHED). */
    long countByTaskIdAndStatus(UUID taskId, TaskApplicationStatus status);

    /**
     * Toan bo don dang dung 1 trang thai cho nhieu task cung luc - dung de tinh pendingApplicantCount
     * hang loat cho GET /tasks/mine (tab "Can xu ly"), tranh N+1 query so voi goi countByTaskIdAndStatus
     * rieng cho tung task.
     */
    List<TaskApplication> findByTaskIdInAndStatus(Collection<UUID> taskIds, TaskApplicationStatus status);

    /** Toan bo don dang dung 1 trang thai va da qua han - dung boi InviteExpirySweeperJob de quet loi moi INVITED qua expires_at. */
    List<TaskApplication> findByStatusAndExpiresAtBefore(TaskApplicationStatus status, Instant instant);
}
