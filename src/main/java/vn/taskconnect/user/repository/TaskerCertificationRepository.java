package vn.taskconnect.user.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.taskconnect.user.api.CertificationStatus;
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

    /**
     * Hang doi duyet cho Admin, xuyen suot moi tai khoan/category - moi lan nop la 1 dong
     * rieng nen loc thang theo status la du, giong KycVerificationRepository.findByStatus.
     */
    Page<TaskerCertification> findByStatus(CertificationStatus status, Pageable pageable);

    /**
     * Dung rieng cho requirePendingReview() trong TaskerSkillService: khoa dong ngay luc doc
     * (SELECT ... FOR UPDATE) de chan race condition khi Admin duyet/tu choi va chinh chu tu
     * huy dung luc gan nhu dong thoi - cung pattern voi
     * AuthRefreshTokenRepository.findByTokenHashForUpdate() va
     * KycVerificationRepository.findByIdForUpdate().
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from TaskerCertification c where c.id = :id")
    Optional<TaskerCertification> findByIdForUpdate(@Param("id") UUID id);
}
