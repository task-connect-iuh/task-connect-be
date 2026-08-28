package vn.taskconnect.user.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.taskconnect.common.response.ApiResponse;
import vn.taskconnect.security.jwt.AuthenticatedPrincipal;
import vn.taskconnect.user.dto.request.CertificateUploadUrlRequest;
import vn.taskconnect.user.dto.request.RejectCertificationRequest;
import vn.taskconnect.user.dto.request.SubmitSkillRequest;
import vn.taskconnect.user.dto.response.CertificateUploadUrlResponse;
import vn.taskconnect.user.dto.response.CertificationReviewResponse;
import vn.taskconnect.user.dto.response.TaskerSkillResponse;
import vn.taskconnect.user.service.CertificateUploadService;
import vn.taskconnect.user.service.TaskerSkillService;

/**
 * Endpoint dang ky ky nang Tasker gop nop chung chi (Buoc 6). Cac endpoint tu phuc vu chi
 * danh cho Tasker (`hasRole('TASKER')`) - khac Bau 4 (KYC) dung chung Poster/Tasker. Hai
 * endpoint con lai (xem chi tiet, duyet, tu choi) chi danh cho Admin, dat trong module User
 * theo cung quy uoc tien to URL nhu KycVerificationController.
 */
@RestController
@RequestMapping("/api/v1/users")
public class TaskerSkillController {

    private final TaskerSkillService skillService;
    private final CertificateUploadService certificateUploadService;

    public TaskerSkillController(TaskerSkillService skillService, CertificateUploadService certificateUploadService) {
        this.skillService = skillService;
        this.certificateUploadService = certificateUploadService;
    }

    /** Xin presigned URL de tu tai file chung chi (anh hoac PDF) len S3, khong qua backend. */
    @PostMapping("/me/tasker-skills/{categoryId}/certificate-upload-url")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<CertificateUploadUrlResponse> createCertificateUploadUrl(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID categoryId,
            @Valid @RequestBody CertificateUploadUrlRequest request) {
        return ApiResponse.ok(certificateUploadService.createUploadUrl(principal.accountId(), categoryId, request));
    }

    /**
     * Dang ky (hoac nop lai sau khi bi tu choi) ky nang cho mot category kem nop chung chi
     * cung luc - xem TaskerSkillService.submitSkill.
     */
    @PostMapping("/me/tasker-skills")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<TaskerSkillResponse> submitSkill(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody SubmitSkillRequest request) {
        TaskerSkillResponse response = skillService.submitSkill(principal.accountId(), request);
        return ApiResponse.ok(response, "Nộp hồ sơ kỹ năng thành công, đang chờ xét duyệt.");
    }

    /** Danh sach moi category chinh chu tai khoan da khai bao, kem trang thai chung chi gan nhat. */
    @GetMapping("/me/tasker-skills")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<List<TaskerSkillResponse>> getMySkills(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.ok(skillService.getMySkills(principal.accountId()));
    }

    /**
     * Chi Admin: toan bo lich su nop chung chi cua mot tai khoan cho mot category, kem so
     * hieu chung chi da giai ma va presigned GET URL ngan han de xem file - phuc vu xet duyet.
     */
    @GetMapping("/{accountId}/tasker-skills/{categoryId}/certifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<CertificationReviewResponse>> getCertificationsForReview(
            @PathVariable UUID accountId, @PathVariable UUID categoryId) {
        return ApiResponse.ok(skillService.getCertificationsForReview(accountId, categoryId));
    }

    /**
     * Chi Admin: duyet mot lan nop chung chi dang cho xet duyet - chuyen ca chung chi lan
     * ho so ky nang cua category do sang VERIFIED (quan he OR, xem TaskerSkillService).
     */
    @PatchMapping("/tasker-certifications/{certificationId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TaskerSkillResponse> approve(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID certificationId) {
        TaskerSkillResponse response = skillService.approve(certificationId, principal.accountId());
        return ApiResponse.ok(response, "Đã duyệt hồ sơ chứng chỉ.");
    }

    /** Chi Admin: tu choi mot lan nop chung chi dang cho xet duyet, bat buoc kem ly do. */
    @PatchMapping("/tasker-certifications/{certificationId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TaskerSkillResponse> reject(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID certificationId, @Valid @RequestBody RejectCertificationRequest request) {
        TaskerSkillResponse response = skillService.reject(certificationId, principal.accountId(), request);
        return ApiResponse.ok(response, "Đã từ chối hồ sơ chứng chỉ.");
    }
}
