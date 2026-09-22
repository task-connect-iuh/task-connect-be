package vn.taskconnect.admin.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.admin.entity.AdminSystemParameter;

/**
 * Truy xuat du lieu bang admin_system_parameters. Chi module Admin duoc inject truc tiep
 * repository nay - module khac phai goi qua AdminFacade.
 */
public interface AdminSystemParameterRepository extends JpaRepository<AdminSystemParameter, UUID> {

    /** Doc 1 tham so theo ten khoa - dung boi AdminFacadeImpl cho moi getter. */
    Optional<AdminSystemParameter> findByParamKey(String paramKey);
}
