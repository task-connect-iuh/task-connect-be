package vn.taskconnect.map.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.map.dto.response.AddressSuggestionResponse;
import vn.taskconnect.map.dto.response.GeocodeResultResponse;
import vn.taskconnect.map.dto.response.LatLngResponse;
import vn.taskconnect.map.dto.response.PlaceDetailResponse;
import vn.taskconnect.map.dto.response.RouteResponse;
import vn.taskconnect.map.infrastructure.PhotonClient;
import vn.taskconnect.map.infrastructure.VietMapClient;
import vn.taskconnect.map.infrastructure.VietMapClient.VietMapPlaceDetail;
import vn.taskconnect.map.infrastructure.VietMapClient.VietMapPlaceItem;
import vn.taskconnect.map.infrastructure.VietMapClient.VietMapRoutePath;
import vn.taskconnect.map.infrastructure.VietMapClient.VietMapRouteResponse;

/**
 * Dieu phoi goi VietMapClient va chuyen du lieu tho cua VietMap thanh DTO on dinh cho
 * frontend (khong lo nguyen vocab cua VietMap ra ngoai module). Moi loi goi VietMap deu duoc
 * thu lai (retry) toi da 2 lan (backoff 300ms/700ms) truoc khi coi la that bai - chong lai
 * loi mang/timeout thoang qua, khong fallback oan chi vi 1 request bi gian doan tuc thoi.
 *
 * <p>reverseGeocode/autocomplete: neu VietMap van that bai sau khi retry (het quota, sap he
 * thong...), TU DONG chuyen sang Photon (mien phi, khong key, xem PhotonClient) - nguoi dung
 * khong thay gian doan, chi co the thay dia chi kem chi tiet hon/it hon chut it tuy provider
 * dang phuc vu. place/route KHONG co duong Photon tuong duong (Photon khong biet ref_id cua
 * VietMap, khong co API dinh tuyen mien phi dang tin cay) - that bai sau retry thi nem
 * BusinessException(MAP_PROVIDER_ERROR) nhu binh thuong; rieng route, frontend
 * (DirectionsModal.tsx) bat dung ma loi nay de tu mo Google Maps ngoai lam phuong an du phong.
 */
@Service
public class MapService {

    private static final Logger log = LoggerFactory.getLogger(MapService.class);
    private static final String DEFAULT_VEHICLE = "motorcycle";
    // So lan cho giua cac lan thu lai VietMap - KHONG tinh lan goi dau tien (vd mang [300,700]
    // nghia la: goi lan 1 -> that bai -> cho 300ms -> goi lan 2 -> that bai -> cho 700ms ->
    // goi lan 3 (cuoi) -> that bai het thi moi coi la VietMap khong kha dung.
    private static final long[] RETRY_BACKOFF_MS = {300, 700};

    private final VietMapClient client;
    private final PhotonClient photonClient;

    public MapService(VietMapClient client, PhotonClient photonClient) {
        this.client = client;
        this.photonClient = photonClient;
    }

    /** Toa do -> dia chi gan nhat. supported=false neu (VietMap hoac Photon fallback) khong tra ket qua nao. */
    public GeocodeResultResponse reverseGeocode(BigDecimal lat, BigDecimal lng) {
        try {
            List<VietMapPlaceItem> items = callVietMapWithRetry(() -> client.reverseGeocode(lat, lng));
            if (items.isEmpty()) {
                return new GeocodeResultResponse("", "", false, null, null);
            }
            VietMapPlaceItem first = items.get(0);
            String addressText = firstNonBlank(first.name(), first.display());
            Double resultLat = first.lat() != null ? first.lat().doubleValue() : null;
            Double resultLng = first.lng() != null ? first.lng().doubleValue() : null;
            return new GeocodeResultResponse(addressText, nullToEmpty(first.address()), true, resultLat, resultLng);
        } catch (VietMapUnavailableException ex) {
            log.warn("VietMap reverse-geocode khong kha dung sau retry, fallback Photon: {}", ex.getMessage());
            return callFallback(() -> photonClient.reverseGeocode(lat, lng));
        }
    }

    /** Van ban go tim -> danh sach goi y. Xem Javadoc lop nay ve fallback Photon khi VietMap loi. */
    public List<AddressSuggestionResponse> autocomplete(String text, BigDecimal focusLat, BigDecimal focusLng) {
        try {
            return callVietMapWithRetry(() -> client.autocomplete(text, focusLat, focusLng)).stream()
                    .map(item -> new AddressSuggestionResponse(
                            item.refId(),
                            firstNonBlank(item.display(), item.name()),
                            firstNonBlank(item.name(), item.display()),
                            nullToEmpty(item.address())))
                    .toList();
        } catch (VietMapUnavailableException ex) {
            log.warn("VietMap autocomplete khong kha dung sau retry, fallback Photon: {}", ex.getMessage());
            return callFallback(() -> photonClient.search(text));
        }
    }

    /**
     * Phan giai 1 ref_id (tu autocomplete) thanh dia chi day du + toa do that su. KHONG co
     * fallback Photon - ref_id la dinh danh noi bo cua VietMap, Photon khong the phan giai
     * duoc. Goi y tu Photon (refId dang "photon:...") da co san lat/lng ngay tu autocomplete()
     * nen frontend khong bao gio goi endpoint nay cho goi y kieu do (xem geocoding.ts
     * resolveSuggestion()).
     */
    public PlaceDetailResponse place(String refId) {
        VietMapPlaceDetail detail;
        try {
            detail = callVietMapWithRetry(() -> client.place(refId));
        } catch (VietMapUnavailableException ex) {
            log.warn("VietMap place khong kha dung sau retry: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.MAP_PROVIDER_ERROR);
        }
        if (detail == null || detail.lat() == null || detail.lng() == null) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_ERROR, "Không xác định được toạ độ cho địa chỉ đã chọn.");
        }
        String addressText = firstNonBlank(detail.name(), detail.display());
        return new PlaceDetailResponse(addressText, nullToEmpty(detail.address()), detail.lat().doubleValue(),
                detail.lng().doubleValue());
    }

    /**
     * Dinh tuyen giua 2 diem - mac dinh xe may. KHONG co fallback Photon (khong co API dinh
     * tuyen) - that bai sau retry thi nem MAP_PROVIDER_ERROR, frontend (DirectionsModal.tsx)
     * bat rieng ma loi nay de moi nguoi dung mo Google Maps ngoai thay the.
     */
    public RouteResponse route(BigDecimal fromLat, BigDecimal fromLng, BigDecimal toLat, BigDecimal toLng, String vehicle) {
        String usedVehicle = vehicle == null || vehicle.isBlank() ? DEFAULT_VEHICLE : vehicle;
        VietMapRouteResponse response;
        try {
            response = callVietMapWithRetry(() -> client.route(fromLat, fromLng, toLat, toLng, usedVehicle));
        } catch (VietMapUnavailableException ex) {
            log.warn("VietMap route khong kha dung sau retry: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.MAP_PROVIDER_ERROR);
        }
        if (response == null || response.paths() == null || response.paths().isEmpty()) {
            throw new BusinessException(ErrorCode.MAP_PROVIDER_ERROR, "Không tìm được tuyến đường phù hợp.");
        }
        VietMapRoutePath path = response.paths().get(0);
        List<List<BigDecimal>> coordinates = path.points() == null ? null : path.points().coordinates();
        // GeoJSON LineString.coordinates dung thu tu [lng, lat] (xem Javadoc VietMapGeometry) -
        // DAO LAI thanh (lat, lng) cho dung quy uoc chung cua RouteResponse/LatLngResponse.
        List<LatLngResponse> points = coordinates == null ? List.of() : coordinates.stream()
                .filter(p -> p.size() >= 2)
                .map(p -> new LatLngResponse(p.get(1).doubleValue(), p.get(0).doubleValue()))
                .toList();
        return new RouteResponse(path.distance(), path.time() / 1000, points);
    }

    /**
     * Goi VietMap, thu lai toi da RETRY_BACKOFF_MS.length lan (backoff tang dan) neu loi mang/
     * HTTP - chong loi thoang qua. Het luot thu ma van loi thi nem VietMapUnavailableException
     * (khong phai BusinessException) de noi goi (reverseGeocode/autocomplete) tu quyet dinh
     * fallback Photon hay bao loi thang (place/route khong co fallback).
     */
    private <T> T callVietMapWithRetry(Supplier<T> call) {
        RestClientException lastError = null;
        for (int attempt = 0; attempt <= RETRY_BACKOFF_MS.length; attempt++) {
            try {
                return call.get();
            } catch (RestClientException ex) {
                lastError = ex;
                if (attempt < RETRY_BACKOFF_MS.length) {
                    sleepQuietly(RETRY_BACKOFF_MS[attempt]);
                }
            }
        }
        throw new VietMapUnavailableException(lastError);
    }

    /** Goi provider fallback (Photon) - loi o day la loi cuoi cung, khong con duong lui nao khac. */
    private <T> T callFallback(Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientException ex) {
            log.warn("Fallback Photon cung that bai: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.MAP_PROVIDER_ERROR);
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return b == null ? "" : b;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** Tin hieu noi bo: VietMap that bai sau khi da thu lai het so lan cho phep. */
    private static final class VietMapUnavailableException extends RuntimeException {
        VietMapUnavailableException(Throwable cause) {
            super(cause);
        }
    }
}
