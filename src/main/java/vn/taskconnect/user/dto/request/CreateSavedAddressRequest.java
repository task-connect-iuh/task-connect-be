package vn.taskconnect.user.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import vn.taskconnect.user.api.LocationType;

/**
 * Luu mot dia chi moi, dung cho POST /users/me/saved-addresses. Ca label/addressText/lat/lng
 * bat buoc (khac UpdateSavedAddressRequest dung cho PATCH, cho phep null tung phan).
 * locationType/arrivalNotes tuy chon.
 */
public record CreateSavedAddressRequest(
        @NotBlank @Size(max = 100) String label,
        @NotBlank @Size(max = 500) String addressText,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal lat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal lng,
        LocationType locationType,
        @Size(max = 500) String arrivalNotes
) {
}
