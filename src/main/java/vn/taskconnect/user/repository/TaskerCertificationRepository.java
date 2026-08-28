package vn.taskconnect.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.user.entity.TaskerCertification;

/**
 * Truy xuat du lieu bang user_tasker_certifications. Chi module User duoc inject truc tiep
 * repository nay - module khac phai goi qua UserFacade.
 */
public interface TaskerCertificationRepository extends JpaRepository<TaskerCertification, UUID> {

    /**
     * Toan bo lich su nop chung chi cua mot cap Tasker+category, moi nhat truoc - khong co
     * UNIQUE tren (account_id, category_id) trong V2 migration, cho phep giu lai cac lan bi
     * tu choi truoc do. Dung cho man hinh Admin xem lich su xet duyet.
     */
    List<TaskerCertification> findByAccountIdAndCategoryIdOrderBySubmittedAtDesc(UUID accountId, UUID categoryId);

    /** Lan nop gan nhat cua mot cap Tasker+category - la lan dang/ vua duoc xet duyet. */
    Optional<TaskerCertification> findFirstByAccountIdAndCategoryIdOrderBySubmittedAtDesc(UUID accountId,
            UUID categoryId);
}
