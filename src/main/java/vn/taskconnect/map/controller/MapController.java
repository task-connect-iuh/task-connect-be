package vn.taskconnect.map.controller;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.taskconnect.common.response.ApiResponse;
import vn.taskconnect.map.dto.response.AddressSuggestionResponse;
import vn.taskconnect.map.dto.response.GeocodeResultResponse;
import vn.taskconnect.map.dto.response.PlaceDetailResponse;
import vn.taskconnect.map.dto.response.RouteResponse;
import vn.taskconnect.map.service.MapService;

/**
 * Proxy cac API ban do (VietMap) cho frontend - khong lo VIETMAP_API_KEY qua trinh duyet
 * (khac tilemap-key, dung truc tiep tu frontend de tai tile/style, xem VietMapProperties).
 * Khong khai bao trong PUBLIC_ENDPOINTS cua SecurityConfig nen mac dinh yeu cau dang nhap
 * (anyRequest().authenticated()) - dung voi thuc te ca 3 man hinh dung API nay (dang viec,
 * ho so, xem viec da nhan) deu da o sau dang nhap.
 */
@RestController
@RequestMapping("/api/v1/map")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    /** Toa do -> dia chi gan nhat, dung khi bam/keo ghim tren ban do. */
    @GetMapping("/reverse-geocode")
    public ApiResponse<GeocodeResultResponse> reverseGeocode(@RequestParam BigDecimal lat, @RequestParam BigDecimal lng) {
        return ApiResponse.ok(mapService.reverseGeocode(lat, lng));
    }

    /** Goi y dia chi kieu GrabFood khi go tim (khong kem toa do, xem place()). */
    @GetMapping("/autocomplete")
    public ApiResponse<List<AddressSuggestionResponse>> autocomplete(@RequestParam String text,
            @RequestParam(required = false) BigDecimal focusLat, @RequestParam(required = false) BigDecimal focusLng) {
        return ApiResponse.ok(mapService.autocomplete(text, focusLat, focusLng));
    }

    /** Phan giai 1 goi y (ref_id) thanh dia chi day du + toa do - goi khi nguoi dung chon 1 dong trong dropdown. */
    @GetMapping("/place")
    public ApiResponse<PlaceDetailResponse> place(@RequestParam String refId) {
        return ApiResponse.ok(mapService.place(refId));
    }

    /** Chi duong giua 2 diem - dung cho man "Viec ban da nhan" cua Tasker. */
    @GetMapping("/route")
    public ApiResponse<RouteResponse> route(@RequestParam BigDecimal fromLat, @RequestParam BigDecimal fromLng,
            @RequestParam BigDecimal toLat, @RequestParam BigDecimal toLng,
            @RequestParam(required = false) String vehicle) {
        return ApiResponse.ok(mapService.route(fromLat, fromLng, toLat, toLng, vehicle));
    }
}
