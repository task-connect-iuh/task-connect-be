package vn.taskconnect.user.controller;

import jakarta.validation.Valid;
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
import vn.taskconnect.user.dto.request.KycUploadUrlRequest;
import vn.taskconnect.user.dto.request.RejectKycRequest;
import vn.taskconnect.user.dto.request.SubmitKycRequest;
import vn.taskconnect.user.dto.response.KycReviewDetailResponse;
import vn.taskconnect.user.dto.response.KycStatusResponse;
import vn.taskconnect.user.dto.response.KycUploadUrlResponse;
import vn.taskconnect.user.service.KycUploadService;
import vn.taskconnect.user.service.KycVerificationService;

/**
 * Endpoint xac minh danh tinh (KYC) - Buoc 4. Ap dung chung Task Poster va Tasker: nop ho
 * so, xin presigned URL tai anh CCCD, xem trang thai cua chinh minh. Ba endpoint con lai
 * (xem chi tiet, duyet, tu choi) chi danh cho Admin - dat trong module User vi
 * user_kyc_verifications thuoc module nay, dung theo quy uoc "tien to URL theo module so
 * huu tai nguyen" cua 16-api-contract.md, khong phai theo vai tro nguoi goi.
 */
@RestController
@RequestMapping("/api/v1/users")
public class KycVerificationController {

    private final KycVerificationService kycService;
    private final KycUploadService kycUploadService;

    public KycVerificationController(KycVerificationService kycService, KycUploadService kycUploadService) {
        this.kycService = kycService;
        this.kycUploadService = kycUploadService;
    }

    /** Xin presigned URL de tu tai anh CCCD (mat truoc/sau) len S3, khong qua backend. */
    @PostMapping("/me/kyc-verifications/upload-url")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<KycUploadUrlResponse> createUploadUrl(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody KycUploadUrlRequest request) {
        return ApiResponse.ok(kycUploadService.createUploadUrl(principal.accountId(), request));
    }

    /**
     * Nop ho so KYC moi. Chan bang loi neu lan nop gan nhat con dang cho duyet hoac da
     * duoc duyet - xem KycVerificationService.submitKyc.
     */
    @PostMapping("/me/kyc-verifications")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<KycStatusResponse> submitKyc(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody SubmitKycRequest request) {
        KycStatusResponse response = KycStatusResponse.from(kycService.submitKyc(principal.accountId(), request));
        return ApiResponse.ok(response, "Nộp hồ sơ xác minh danh tính thành công, đang chờ xét duyệt.");
    }

    /** Xem trang thai lan nop KYC gan nhat cua chinh minh. */
    @GetMapping("/me/kyc-verifications/latest")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<KycStatusResponse> getMyLatestKyc(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.ok(KycStatusResponse.from(kycService.getMyLatestKyc(principal.accountId())));
    }

    /**
     * Chi Admin: xem chi tiet lan nop KYC gan nhat cua mot tai khoan bat ky, kem so CCCD da
     * giai ma va presigned GET URL ngan han de xem anh - phuc vu xet duyet.
     */
    @GetMapping("/{accountId}/kyc-verifications/latest")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<KycReviewDetailResponse> getLatestKycForReview(@PathVariable UUID accountId) {
        return ApiResponse.ok(kycService.getLatestKycForReview(accountId));
    }

    /** Chi Admin: duyet mot lan nop KYC dang cho xet duyet. */
    @PatchMapping("/kyc-verifications/{kycVerificationId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<KycStatusResponse> approve(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID kycVerificationId) {
        KycStatusResponse response =
                KycStatusResponse.from(kycService.approve(kycVerificationId, principal.accountId()));
        return ApiResponse.ok(response, "Đã duyệt hồ sơ xác minh danh tính.");
    }

    /** Chi Admin: tu choi mot lan nop KYC dang cho xet duyet, bat buoc kem ly do. */
    @PatchMapping("/kyc-verifications/{kycVerificationId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<KycStatusResponse> reject(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID kycVerificationId, @Valid @RequestBody RejectKycRequest request) {
        KycStatusResponse response =
                KycStatusResponse.from(kycService.reject(kycVerificationId, principal.accountId(), request));
        return ApiResponse.ok(response, "Đã từ chối hồ sơ xác minh danh tính.");
    }
}
