package vn.taskconnect.matching.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.matching.entity.TaskerInvite;

/**
 * Truy xuat du lieu bang matching_tasker_invites. Chi module Matching duoc inject truc tiep
 * repository nay.
 */
public interface TaskerInviteRepository extends JpaRepository<TaskerInvite, UUID> {

    /** Loi moi cho dung mot cap Task+Tasker - dung kiem tra "da moi roi" truoc khi tao moi. */
    Optional<TaskerInvite> findByTaskIdAndTaskerId(UUID taskId, UUID taskerId);

    /** Toan bo loi moi cua mot Task - dung khi can doi trang thai cac loi moi PENDING con lai. */
    List<TaskerInvite> findByTaskId(UUID taskId);

    /** Mot loi moi cu the trong pham vi dung mot Task - dung cho accept/decline, tranh loi moi cua task khac lay nham id. */
    Optional<TaskerInvite> findByIdAndTaskId(UUID id, UUID taskId);

    /** Toan bo loi moi (moi trang thai) ma chinh Tasker dang goi da nhan duoc - dung cho man "Loi moi ban nhan duoc". */
    List<TaskerInvite> findByTaskerIdOrderByCreatedAtDesc(UUID taskerId);
}
