package vn.taskconnect.user.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.user.entity.ServiceCategory;
import vn.taskconnect.user.repository.ServiceCategoryRepository;

/** Nghiep vu doc danh sach danh muc nhom dich vu (Master Data, chi doc qua API - sua bang migration). */
@Service
public class ServiceCategoryService {

    private final ServiceCategoryRepository categoryRepository;

    public ServiceCategoryService(ServiceCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /** Danh sach danh muc con hien hanh, sap theo ten, dung cho GET /users/service-categories. */
    @Transactional(readOnly = true)
    public List<ServiceCategory> listActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc();
    }
}
