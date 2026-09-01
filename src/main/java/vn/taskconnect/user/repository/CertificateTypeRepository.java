package vn.taskconnect.user.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.user.entity.CertificateType;

/**
 * Truy xuat du lieu bang user_certificate_types. Chi module User duoc inject truc tiep
 * repository nay - module khac phai goi qua UserFacade.
 */
public interface CertificateTypeRepository extends JpaRepository<CertificateType, UUID> {

    /** Danh sach loai chung chi con hien hanh, sap theo ten - dung cho man hinh chon chung chi. */
    List<CertificateType> findByActiveTrueOrderByNameAsc();
}
