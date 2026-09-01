package vn.taskconnect.user.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Du lieu cap nhat ho so ca nhan, dung cho PATCH /users/me theo dung ngu nghia "sua mot
 * phan" cua 16-api-contract.md: field nao khong gui (hoac gui null) giu nguyen gia tri cu,
 * khong field nao bat buoc o muc DTO. fullName va operatingArea van bat buoc phai co gia
 * tri (cu hoac moi) sau khi ap dung, vi la NOT NULL trong user_profiles - UserProfileService
 * tu kiem tra dieu nay cho lan tao ho so dau tien, khong dung Bean Validation vi con phu
 * thuoc ho so da ton tai hay chua.
 */
public record UpdateProfileRequest(
        @Size(max = 150) String fullName,
        @Size(max = 500) String avatarUrl,
        @Size(max = 500) String addressText,
        @Size(max = 1000) String bio,
        @Size(max = 255) String operatingArea,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal locationLat,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal locationLng
) {
}
