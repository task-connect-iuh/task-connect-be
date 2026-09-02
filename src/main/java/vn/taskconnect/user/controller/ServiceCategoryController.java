package vn.taskconnect.user.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.taskconnect.common.response.ApiResponse;
import vn.taskconnect.user.dto.response.ServiceCategoryResponse;
import vn.taskconnect.user.service.ServiceCategoryService;

/**
 * Endpoint danh muc nhom dich vu (Master Data). Cung yeu cau da dang nhap nhu cac endpoint
 * User khac o Buoc 1 (xem docs/PROGRESS-USER-MODULE.md) de nhat quan trong module, khong
 * phai vi du lieu nhay cam - co the mo public sau neu FE can hien thi truoc dang nhap.
 */
@RestController
@RequestMapping("/api/v1/users/service-categories")
public class ServiceCategoryController {

    private final ServiceCategoryService categoryService;

    public ServiceCategoryController(ServiceCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** Danh sach danh muc con hien hanh, dung cho FE hien danh sach chon nhom dich vu. */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ServiceCategoryResponse>> listActiveCategories() {
        List<ServiceCategoryResponse> categories = categoryService.listActiveCategories().stream()
                .map(ServiceCategoryResponse::from)
                .toList();
        return ApiResponse.ok(categories);
    }
}
