package vn.taskconnect.map.dto.response;

/**
 * Ket qua reverse geocode (toa do -> dia chi) tra ve cho frontend, dung cho
 * GET /api/v1/map/reverse-geocode. addressText/operatingArea rong va supported=false khi
 * VietMap khong tra ket qua nao cho toa do nay (vd giua bien, ngoai vung phu du lieu).
 */
public record GeocodeResultResponse(String addressText, String operatingArea, boolean supported, Double lat, Double lng) {
}
