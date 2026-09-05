package vn.taskconnect.user.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.user.entity.KycIdNumberLock;

/**
 * Truy xuat du lieu bang user_kyc_id_number_locks. Chi module User duoc inject truc tiep
 * repository nay - module khac phai goi qua UserFacade. findById(byte[]) da co san tu
 * JpaRepository, du dung de tra cuu theo id_number_hash (khoa chinh).
 */
public interface KycIdNumberLockRepository extends JpaRepository<KycIdNumberLock, byte[]> {

    void deleteByIdNumberHashAndKycVerificationId(byte[] idNumberHash, UUID kycVerificationId);
}
