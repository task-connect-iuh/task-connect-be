package vn.taskconnect.user.dto.response;

import java.math.BigDecimal;
import java.util.UUID;
import vn.taskconnect.user.api.LocationType;
import vn.taskconnect.user.entity.SavedAddress;

/** Mot dia chi da luu, tra ve cho client (POST/GET/PATCH /users/me/saved-addresses). */
public record SavedAddressResponse(
        UUID id,
        String label,
        String addressText,
        BigDecimal lat,
        BigDecimal lng,
        LocationType locationType,
        String arrivalNotes
) {

    /** Chuyen entity sang DTO tra ve qua API, tranh entity JPA lo ra ngoai lop controller. */
    public static SavedAddressResponse from(SavedAddress address) {
        return new SavedAddressResponse(address.getId(), address.getLabel(), address.getAddressText(),
                address.getLat(), address.getLng(), address.getLocationType(), address.getArrivalNotes());
    }
}
