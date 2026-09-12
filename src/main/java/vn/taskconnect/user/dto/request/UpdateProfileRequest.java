package vn.taskconnect.user.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import vn.taskconnect.user.api.LocationType;

/**
 * Du lieu cap nhat ho so ca nhan, dung cho PATCH /users/me theo dung ngu nghia "sua mot
 * phan" cua 16-api-contract.md: field nao khong gui (hoac gui null) giu nguyen gia tri cu,
 * khong field nao bat buoc o muc DTO. fullName va operatingArea van bat buoc phai co gia
 * tri (cu hoac moi) sau khi ap dung, vi la NOT NULL trong user_profiles - UserProfileService
 * tu kiem tra dieu nay cho lan tao ho so dau tien, khong dung Bean Validation vi con phu
 * thuoc ho so da ton tai hay chua.
 *
 * <p>jobCategoryIds la ngoai le duy nhat cua quy tac "null = khong doi": null nghia la
 * khong doi (giu nguyen danh sach cu), con danh sach rong [] nghia la xoa het lua chon cu -
 * hai truong hop nay phan biet duoc vi JSON co the gui null hoac [] rieng biet.
 */
public record UpdateProfileRequest(
        @Size(max = 150) String fullName,
        @Size(max = 500) String avatarUrl,
        @Size(max = 500) String addressText,
        @Size(max = 1000) String bio,
        @Size(max = 255) String operatingArea,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal locationLat,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal locationLng,
        @Min(1) @Max(50) Integer preferredRadiusKm,
        LocationType locationType,
        @Size(max = 500) String arrivalNotes,
        List<UUID> jobCategoryIds
) {
}
