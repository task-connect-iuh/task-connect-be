package vn.taskconnect.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.user.api.SkillVerificationStatus;
import vn.taskconnect.user.entity.TaskerSkillProfile;

/**
 * Truy xuat du lieu bang user_tasker_skill_profiles. Chi module User duoc inject truc tiep
 * repository nay - module khac phai goi qua UserFacade.
 */
public interface TaskerSkillProfileRepository extends JpaRepository<TaskerSkillProfile, UUID> {

    /** Danh sach moi category Tasker da khai bao, dung cho man hinh "ky nang cua toi". */
    List<TaskerSkillProfile> findByAccountIdOrderByCreatedAtAsc(UUID accountId);

    /** Ho so ky nang cua dung mot cap Tasker+category - UNIQUE trong V2 migration. */
    Optional<TaskerSkillProfile> findByAccountIdAndCategoryId(UUID accountId, UUID categoryId);

    /** Cac nhom dich vu da VERIFIED cua mot Tasker, dung de lo badge "Da xac minh" tren ho so cong khai (GET /users/{accountId}). */
    List<TaskerSkillProfile> findByAccountIdAndVerificationStatusOrderByVerifiedAtAsc(UUID accountId,
            SkillVerificationStatus verificationStatus);
}
