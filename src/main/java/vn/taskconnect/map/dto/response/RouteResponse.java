package vn.taskconnect.map.dto.response;

import java.util.List;

/** Ket qua dinh tuyen giua 2 diem, dung cho GET /api/v1/map/route. */
public record RouteResponse(double distanceMeters, long durationSeconds, List<LatLngResponse> path) {
}
