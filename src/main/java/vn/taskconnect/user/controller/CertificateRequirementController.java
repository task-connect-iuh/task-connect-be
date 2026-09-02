package vn.taskconnect.user.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.taskconnect.common.response.ApiResponse;
import vn.taskconnect.user.dto.response.CategoryCertificateRequirementResponse;
import vn.taskconnect.user.dto.response.CertificateTypeResponse;
import vn.taskconnect.user.service.CertificateRequirementService;

/**
 * Endpoint loai chung chi va yeu cau chung chi theo category (Master Data, Buoc 5). Cung
 * yeu cau da dang nhap nhu cac endpoint Master Data khac cua module User (service-categories
 * o Buoc 2) de nhat quan, khong phai vi du lieu nhay cam.
 */
@RestController
@RequestMapping("/api/v1/users")
public class CertificateRequirementController {

    private final CertificateRequirementService certificateRequirementService;

    public CertificateRequirementController(CertificateRequirementService certificateRequirementService) {
        this.certificateRequirementService = certificateRequirementService;
    }

    /** Danh sach loai chung chi con hien hanh, dung cho FE hien danh sach chon chung chi. */
    @GetMapping("/certificate-types")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<CertificateTypeResponse>> listActiveCertificateTypes() {
        List<CertificateTypeResponse> response = certificateRequirementService.listActiveCertificateTypes().stream()
                .map(CertificateTypeResponse::from)
                .toList();
        return ApiResponse.ok(response);
    }

    /**
     * Danh sach chung chi duoc chap nhan cho mot category cu the - dung o man hinh khai
     * bao ky nang (Buoc 6) de biet upload loai chung chi nao. Quan he OR giua cac dong
     * mandatory=true, xem CategoryCertificateRequirementResponse.
     */
    @GetMapping("/service-categories/{categoryId}/certificate-requirements")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<CategoryCertificateRequirementResponse>> listRequirementsForCategory(
            @PathVariable UUID categoryId) {
        return ApiResponse.ok(certificateRequirementService.listRequirementsForCategory(categoryId));
    }
}
