package vn.taskconnect.user.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.taskconnect.common.response.ApiResponse;
import vn.taskconnect.security.jwt.AuthenticatedPrincipal;
import vn.taskconnect.user.dto.request.CreateAvailabilityRequest;
import vn.taskconnect.user.dto.request.UpdateAvailabilityRequest;
import vn.taskconnect.user.dto.response.AvailabilitySlotResponse;
import vn.taskconnect.user.service.TaskerAvailabilityService;

/**
 * Endpoint lich ranh Tasker (Buoc 7) - khai bao, xem, sua, xoa khung gio ranh trong tuan.
 * Chi danh cho Tasker (`hasRole('TASKER')`), cung muc gioi han nhu Buoc 6. Doc lap voi KYC
 * va ky nang, khong chan dieu kien gi.
 */
@RestController
@RequestMapping("/api/v1/users/me/tasker-availability")
public class TaskerAvailabilityController {

    private final TaskerAvailabilityService availabilityService;

    public TaskerAvailabilityController(TaskerAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /** Khai bao mot khung gio ranh moi. */
    @PostMapping
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<AvailabilitySlotResponse> addSlot(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody CreateAvailabilityRequest request) {
        AvailabilitySlotResponse response =
                AvailabilitySlotResponse.from(availabilityService.addSlot(principal.accountId(), request));
        return ApiResponse.ok(response, "Đã thêm khung giờ rảnh.");
    }

    /** Toan bo khung gio ranh cua chinh minh, sap theo thu roi gio bat dau. */
    @GetMapping
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<List<AvailabilitySlotResponse>> getMySlots(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        List<AvailabilitySlotResponse> response = availabilityService.getMySlots(principal.accountId()).stream()
                .map(AvailabilitySlotResponse::from)
                .toList();
        return ApiResponse.ok(response);
    }

    /** Sua mot phan khung gio da khai bao - truong nao khong gui (hoac null) giu nguyen gia tri cu. */
    @PatchMapping("/{slotId}")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<AvailabilitySlotResponse> updateSlot(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID slotId, @Valid @RequestBody UpdateAvailabilityRequest request) {
        AvailabilitySlotResponse response = AvailabilitySlotResponse.from(
                availabilityService.updateSlot(principal.accountId(), slotId, request));
        return ApiResponse.ok(response, "Đã cập nhật khung giờ rảnh.");
    }

    /** Xoa mot khung gio da khai bao. */
    @DeleteMapping("/{slotId}")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<Void> deleteSlot(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID slotId) {
        availabilityService.deleteSlot(principal.accountId(), slotId);
        return ApiResponse.ok(null, "Đã xoá khung giờ rảnh.");
    }
}
