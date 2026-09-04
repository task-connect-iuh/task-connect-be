package vn.taskconnect.user.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.taskconnect.user.api.KycStatus;
import vn.taskconnect.user.entity.KycVerification;

/**
 * Truy xuat du lieu bang user_kyc_verifications. Chi module User duoc inject truc tiep
 * repository nay - module khac phai goi qua UserFacade.
 */
public interface KycVerificationRepository extends JpaRepository<KycVerification, UUID> {

    /**
     * Lan nop gan nhat cua mot tai khoan - khong co UNIQUE tren account_id (cho phep nop
     * lai sau khi bi tu choi), nen ban ghi nay la trang thai xac minh hien hanh cua tai khoan.
     */
    Optional<KycVerification> findFirstByAccountIdOrderBySubmittedAtDesc(UUID accountId);

    /**
     * Hang doi duyet cho Admin - moi lan nop la 1 dong rieng (khong UNIQUE tren account_id,
     * xem KycVerification javadoc) nen loc thang theo status la du, khong can gom nhom theo
     * tai khoan. Cu chua sap xep truoc, xem KycVerificationService.listForReview.
     */
    Page<KycVerification> findByStatus(KycStatus status, Pageable pageable);

    /**
     * Dung rieng cho requirePendingReview() trong KycVerificationService: khoa dong ngay
     * luc doc (SELECT ... FOR UPDATE) de chan race condition khi Admin duyet/tu choi va
     * chinh chu tu huy dung luc gan nhu dong thoi - dung pattern voi
     * AuthRefreshTokenRepository.findByTokenHashForUpdate(). Khong co khoa nay, ca hai giao
     * dich co the cung doc duoc status dang VERIFYING truoc khi ben kia kip commit, dan den
     * ca hai cung ghi thanh cong thay vi dung 1 ben thang.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from KycVerification k where k.id = :id")
    Optional<KycVerification> findByIdForUpdate(@Param("id") UUID id);
}
