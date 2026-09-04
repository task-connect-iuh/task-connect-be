package vn.taskconnect.user.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.taskconnect.common.response.ApiResponse;
import vn.taskconnect.common.response.PageResponse;
import vn.taskconnect.security.jwt.AuthenticatedPrincipal;
import vn.taskconnect.user.api.KycStatus;
import vn.taskconnect.user.dto.request.KycUploadUrlRequest;
import vn.taskconnect.user.dto.request.RejectKycRequest;
import vn.taskconnect.user.dto.request.SubmitKycRequest;
import vn.taskconnect.user.dto.response.KycReviewDetailResponse;
import vn.taskconnect.user.dto.response.KycReviewSummaryResponse;
import vn.taskconnect.user.dto.response.KycStatusResponse;
import vn.taskconnect.user.dto.response.KycUploadUrlResponse;
import vn.taskconnect.user.service.KycUploadService;
import vn.taskconnect.user.service.KycVerificationService;

/**
 * Endpoint xac minh danh tinh (KYC) - Buoc 4. Chi Tasker can KYC (UC05 "Xac minh danh tinh
 * Tasker", Task Poster khong can) - 3 endpoint tu phuc vu (nop ho so, xin presigned URL tai
 * anh CCCD, xem trang thai cua chinh minh) yeu cau hasRole('TASKER'). Ba endpoint con lai
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
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<KycUploadUrlResponse> createUploadUrl(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody KycUploadUrlRequest request) {
        return ApiResponse.ok(kycUploadService.createUploadUrl(principal.accountId(), request));
    }

    /**
     * Nop ho so KYC moi. Chan bang loi neu lan nop gan nhat con dang cho duyet hoac da
     * duoc duyet - xem KycVerificationService.submitKyc.
     */
    @PostMapping("/me/kyc-verifications")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<KycStatusResponse> submitKyc(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody SubmitKycRequest request) {
        KycStatusResponse response = KycStatusResponse.from(kycService.submitKyc(principal.accountId(), request));
        return ApiResponse.ok(response, "Nộp hồ sơ xác minh danh tính thành công, đang chờ xét duyệt.");
    }

    /** Xem trang thai lan nop KYC gan nhat cua chinh minh. */
    @GetMapping("/me/kyc-verifications/latest")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<KycStatusResponse> getMyLatestKyc(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.ok(KycStatusResponse.from(kycService.getMyLatestKyc(principal.accountId())));
    }

    /**
     * Chinh chu tu huy lan nop KYC cua minh khi con dang cho duyet (VERIFYING) - khac
     * duyet/tu choi (danh cho Admin). Xem KycVerificationService.cancel.
     */
    @PatchMapping("/me/kyc-verifications/{kycVerificationId}/cancel")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<KycStatusResponse> cancelMyKyc(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID kycVerificationId) {
        KycStatusResponse response =
                KycStatusResponse.from(kycService.cancel(principal.accountId(), kycVerificationId));
        return ApiResponse.ok(response, "Đã huỷ hồ sơ xác minh danh tính.");
    }

    /**
     * Chi Admin: hang doi cac lan nop KYC theo status (mac dinh VERIFYING - dang cho duyet),
     * moi nhat truoc, co phan trang. Dung de biet accountId nao dang can xu ly - xem chi
     * tiet tung dong qua getLatestKycForReview(accountId) ben duoi.
     */
    @GetMapping("/kyc-verifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<KycReviewSummaryResponse>> listKycForReview(
            @RequestParam(defaultValue = "VERIFYING") KycStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.from(
                kycService.listForReview(status, PageRequest.of(page, Math.min(size, 100)))));
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
