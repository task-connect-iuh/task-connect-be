package vn.taskconnect.user.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import vn.taskconnect.user.api.LocationType;

/**
 * Sua mot dia chi da luu, dung cho PATCH /users/me/saved-addresses/{id} theo dung ngu nghia
 * "sua mot phan" cua 16-api-contract.md: truong nao null giu nguyen gia tri cu, khong truong
 * nao bat buoc o muc DTO - khac CreateSavedAddressRequest (POST).
 */
public record UpdateSavedAddressRequest(
        @Size(max = 100) String label,
        @Size(max = 500) String addressText,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal lat,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal lng,
        LocationType locationType,
        @Size(max = 500) String arrivalNotes
) {
}
