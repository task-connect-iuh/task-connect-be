package vn.taskconnect.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.user.entity.ServiceCategory;

/** Truy van danh muc nhom dich vu. Chi User module duoc inject truc tiep - module khac doc qua UserFacade. */
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, UUID> {

    /** Danh sach danh muc con hien hanh, sap theo ten de hien thi on dinh cho client. */
    List<ServiceCategory> findByActiveTrueOrderByNameAsc();

    /** Tra cuu theo ma nghiep vu on dinh, dung khi module khac can validate hoac seed du lieu lien quan. */
    Optional<ServiceCategory> findByCode(String code);
}
