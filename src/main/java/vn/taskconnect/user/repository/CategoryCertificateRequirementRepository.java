package vn.taskconnect.user.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.user.entity.CategoryCertificateRequirement;

/**
 * Truy xuat du lieu bang user_category_certificate_requirements. Chi module User duoc
 * inject truc tiep repository nay - module khac phai goi qua UserFacade.
 */
public interface CategoryCertificateRequirementRepository extends JpaRepository<CategoryCertificateRequirement, UUID> {

    /** Danh sach chung chi duoc chap nhan cho mot category - dung o Buoc 6 va man hinh chon chung chi. */
    List<CategoryCertificateRequirement> findByCategoryId(UUID categoryId);
}
