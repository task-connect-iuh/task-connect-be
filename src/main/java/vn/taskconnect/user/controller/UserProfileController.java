package vn.taskconnect.user.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.taskconnect.auth.api.AuthFacade;
import vn.taskconnect.common.response.ApiResponse;
import vn.taskconnect.security.jwt.AuthenticatedPrincipal;
import vn.taskconnect.user.dto.request.AvatarUploadUrlRequest;
import vn.taskconnect.user.dto.request.UpdateProfileRequest;
import vn.taskconnect.user.dto.response.AvatarUploadUrlResponse;
import vn.taskconnect.user.dto.response.ProfileResponse;
import vn.taskconnect.user.dto.response.PublicProfileResponse;
import vn.taskconnect.user.service.AvatarUploadService;
import vn.taskconnect.user.service.UserProfileService;

/**
 * Endpoint ho so ca nhan: xem/sua ho so cua chinh minh, xem ho so toi thieu cong khai cua
 * tai khoan khac. Moi endpoint yeu cau da dang nhap (khong nam trong PUBLIC_ENDPOINTS cua
 * SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final UserProfileService profileService;
    private final AvatarUploadService avatarUploadService;
    private final AuthFacade authFacade;

    public UserProfileController(UserProfileService profileService, AvatarUploadService avatarUploadService,
            AuthFacade authFacade) {
        this.profileService = profileService;
        this.avatarUploadService = avatarUploadService;
        this.authFacade = authFacade;
    }

    /** Xem ho so cua chinh minh. Moi tai khoan da dang nhap deu duoc xem ho so cua chinh no. */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ProfileResponse> getMyProfile(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        ProfileResponse response = ProfileResponse.from(profileService.getMyProfile(principal.accountId()),
                authFacade.findAccount(principal.accountId()).orElse(null));
        return ApiResponse.ok(response);
    }

    /**
     * Tao moi (lan dau) hoac cap nhat mot phan ho so cua chinh minh (upsert) - xem quyet
     * dinh lazy-create trong docs/PROGRESS-USER-MODULE.md.
     */
    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ProfileResponse> updateMyProfile(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse response = ProfileResponse.from(profileService.upsertProfile(principal.accountId(), request),
                authFacade.findAccount(principal.accountId()).orElse(null));
        return ApiResponse.ok(response, "Cập nhật hồ sơ thành công.");
    }

    /** Xem ho so toi thieu cong khai cua mot tai khoan bat ky, chi can da dang nhap. */
    @GetMapping("/{accountId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PublicProfileResponse> getPublicProfile(@PathVariable UUID accountId) {
        return ApiResponse.ok(profileService.getPublicProfile(accountId));
    }

    /**
     * Xin presigned URL de tu tai anh dai dien len S3 truc tiep tu client, khong qua
     * backend. Sau khi PUT thanh cong len uploadUrl tra ve, client goi PATCH /users/me voi
     * avatarUrl = publicUrl de luu lai. Xem docs/adr/ADR-003-avatar-storage-s3-presigned-upload.md.
     */
    @PostMapping("/me/avatar-upload-url")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AvatarUploadUrlResponse> createAvatarUploadUrl(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody AvatarUploadUrlRequest request) {
        return ApiResponse.ok(avatarUploadService.createUploadUrl(principal.accountId(), request));
    }
}
