package vn.taskconnect.user.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.taskconnect.common.response.ApiResponse;
import vn.taskconnect.security.jwt.AuthenticatedPrincipal;
import vn.taskconnect.user.dto.request.UpdateProfileRequest;
import vn.taskconnect.user.dto.response.ProfileResponse;
import vn.taskconnect.user.dto.response.PublicProfileResponse;
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

    public UserProfileController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    /** Xem ho so cua chinh minh. Moi tai khoan da dang nhap deu duoc xem ho so cua chinh no. */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ProfileResponse> getMyProfile(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.ok(ProfileResponse.from(profileService.getMyProfile(principal.accountId())));
    }

    /**
     * Tao moi (lan dau) hoac cap nhat mot phan ho so cua chinh minh (upsert) - xem quyet
     * dinh lazy-create trong docs/PROGRESS-USER-MODULE.md.
     */
    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ProfileResponse> updateMyProfile(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse response = ProfileResponse.from(profileService.upsertProfile(principal.accountId(), request));
        return ApiResponse.ok(response, "Cập nhật hồ sơ thành công.");
    }

    /** Xem ho so toi thieu cong khai cua mot tai khoan bat ky, chi can da dang nhap. */
    @GetMapping("/{accountId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PublicProfileResponse> getPublicProfile(@PathVariable UUID accountId) {
        return ApiResponse.ok(PublicProfileResponse.from(profileService.getPublicProfile(accountId)));
    }
}
