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
import vn.taskconnect.user.dto.request.CreateSavedAddressRequest;
import vn.taskconnect.user.dto.request.UpdateSavedAddressRequest;
import vn.taskconnect.user.dto.response.SavedAddressResponse;
import vn.taskconnect.user.service.SavedAddressService;

/**
 * Endpoint so dia chi tu luu de chon nhanh khi dang viec (vd "Nha", "Cong ty", giong so dia
 * chi giao hang Shopee) - them, xem, sua, xoa. Chi danh cho Task Poster
 * (`hasRole('TASK_POSTER')`), dung noi trong PostTaskPage.tsx.
 */
@RestController
@RequestMapping("/api/v1/users/me/saved-addresses")
public class SavedAddressController {

    private final SavedAddressService addressService;

    public SavedAddressController(SavedAddressService addressService) {
        this.addressService = addressService;
    }

    /** Luu mot dia chi moi. */
    @PostMapping
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<SavedAddressResponse> addAddress(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody CreateSavedAddressRequest request) {
        SavedAddressResponse response =
                SavedAddressResponse.from(addressService.addAddress(principal.accountId(), request));
        return ApiResponse.ok(response, "Đã lưu địa chỉ.");
    }

    /** Toan bo dia chi da luu cua chinh minh, moi luu gan day nhat truoc. */
    @GetMapping
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<List<SavedAddressResponse>> getMyAddresses(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        List<SavedAddressResponse> response = addressService.getMyAddresses(principal.accountId()).stream()
                .map(SavedAddressResponse::from)
                .toList();
        return ApiResponse.ok(response);
    }

    /** Sua mot phan dia chi da luu - truong nao khong gui (hoac null) giu nguyen gia tri cu. */
    @PatchMapping("/{addressId}")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<SavedAddressResponse> updateAddress(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID addressId, @Valid @RequestBody UpdateSavedAddressRequest request) {
        SavedAddressResponse response = SavedAddressResponse.from(
                addressService.updateAddress(principal.accountId(), addressId, request));
        return ApiResponse.ok(response, "Đã cập nhật địa chỉ.");
    }

    /** Xoa mot dia chi da luu. */
    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<Void> deleteAddress(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID addressId) {
        addressService.deleteAddress(principal.accountId(), addressId);
        return ApiResponse.ok(null, "Đã xoá địa chỉ.");
    }
}
