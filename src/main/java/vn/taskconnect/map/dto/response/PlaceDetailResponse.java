package vn.taskconnect.map.dto.response;

/** Dia chi day du + toa do phan giai tu 1 ref_id, dung cho GET /api/v1/map/place. */
public record PlaceDetailResponse(String addressText, String operatingArea, double lat, double lng) {
}
