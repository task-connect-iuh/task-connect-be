package vn.taskconnect.map.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Goi truc tiep REST API cua VietMap (maps.vietmap.vn) - Reverse Geocoding v4, Autocomplete
 * v4, Place v4, Route v1.1 (xem docs/map-api tren maps.vietmap.vn). Dung {@link RestClient} co
 * san trong spring-web, khong them dependency Maven moi - cung cach GroqChatClient (module AI)
 * dang lam. Nem RuntimeException khi goi HTTP that bai; {@link vn.taskconnect.map.service.MapService}
 * la noi bat va chuyen thanh BusinessException(MAP_PROVIDER_ERROR), client nay khong tu quyet
 * dinh fallback.
 */
@Component
public class VietMapClient {

    private final VietMapProperties properties;
    private final RestClient restClient;

    VietMapClient(VietMapProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    /** Reverse Geocoding v4: 1 toa do -> danh sach ket qua gan nhat (thuong 1-3 phan tu, co the rong). */
    public List<VietMapPlaceItem> reverseGeocode(BigDecimal lat, BigDecimal lng) {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.baseUrl() + "/api/reverse/v4")
                .queryParam("apikey", properties.apiKey())
                .queryParam("lat", lat)
                .queryParam("lng", lng)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
        VietMapPlaceItem[] result = restClient.get().uri(uri).retrieve().body(VietMapPlaceItem[].class);
        return result == null ? List.of() : List.of(result);
    }

    /**
     * Autocomplete v4: chuoi go tim -> danh sach goi y KHONG kem toa do (VietMap chi tra
     * toa do qua Place v4 rieng, xem {@link #place}) - tiet kiem quota vi 1 lan go co the
     * ra 8+ goi y nhung nguoi dung chi chon 1.
     */
    public List<VietMapAutocompleteItem> autocomplete(String text, BigDecimal focusLat, BigDecimal focusLng) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.baseUrl() + "/api/autocomplete/v4")
                .queryParam("apikey", properties.apiKey())
                .queryParam("text", text);
        if (focusLat != null && focusLng != null) {
            builder.queryParam("focus", focusLat + "," + focusLng);
        }
        // BAT BUOC .encode(UTF_8).build().toUri() - dung String url roi truyen vao
        // restClient.uri(String) se bi UriBuilderFactory ma hoa THEM MOT LAN NUA (vd "%" cua
        // "%E1%BB%91" bi doi thanh "%25E1%25BB%2591"), lam sai lech hoan toan chuoi tim kiem
        // (VietMap van tra 200 OK nhung khong khop ket qua nao, vd "Quốc lộ 1A" ra mang rong).
        // Truyen thang java.net.URI (khong phai String) qua restClient.uri(URI) de bo qua
        // buoc encode lai cua factory - day la URI CUOI CUNG, dung nguyen ven.
        URI uri = builder.encode(StandardCharsets.UTF_8).build().toUri();
        VietMapAutocompleteItem[] result = restClient.get().uri(uri).retrieve().body(VietMapAutocompleteItem[].class);
        return result == null ? List.of() : List.of(result);
    }

    /** Place v4: phan giai 1 ref_id (tu autocomplete/geocode) thanh dia chi day du + toa do. */
    public VietMapPlaceDetail place(String refId) {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.baseUrl() + "/api/place/v4")
                .queryParam("apikey", properties.apiKey())
                .queryParam("refid", refId)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
        return restClient.get().uri(uri).retrieve().body(VietMapPlaceDetail.class);
    }

    /**
     * Route v1.1: dinh tuyen giua 2 diem. Dung ban v1.1 (khong phai v4) vi v1.1 la ban on dinh,
     * du dung cho o to/xe may - v4 them cac tham so logistics (truck/container) khong can thiet
     * o day. points_encoded=false de VietMap tra toa do tho thay vi Google Polyline ma hoa,
     * tranh phai giai ma o ca backend lan frontend.
     */
    public VietMapRouteResponse route(BigDecimal fromLat, BigDecimal fromLng, BigDecimal toLat, BigDecimal toLng,
            String vehicle) {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.baseUrl() + "/api/route")
                .queryParam("api-version", "1.1")
                .queryParam("apikey", properties.apiKey())
                .queryParam("point", fromLat + "," + fromLng)
                .queryParam("point", toLat + "," + toLng)
                .queryParam("points_encoded", "false")
                .queryParam("vehicle", vehicle)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
        return restClient.get().uri(uri).retrieve().body(VietMapRouteResponse.class);
    }

    /** Mot phan tu ket qua Reverse Geocoding v4. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VietMapPlaceItem(
            BigDecimal lat,
            BigDecimal lng,
            @JsonProperty("ref_id") String refId,
            String address,
            String name,
            String display) {
    }

    /** Mot phan tu ket qua Autocomplete v4 - KHONG co lat/lng, phai goi Place v4 rieng. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VietMapAutocompleteItem(
            @JsonProperty("ref_id") String refId,
            String address,
            String name,
            String display) {
    }

    /** Ket qua Place v4 - phan giai ref_id thanh dia chi day du + toa do. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VietMapPlaceDetail(
            String display,
            String name,
            String address,
            BigDecimal lat,
            BigDecimal lng) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VietMapRouteResponse(String code, List<VietMapRoutePath> paths) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VietMapRoutePath(double distance, long time, VietMapGeometry points) {
    }

    /**
     * Voi points_encoded=false, VietMap KHONG tra "points" la mang toa do tho ma la 1 object
     * GeoJSON LineString ({@code {"type":"LineString","coordinates":[[lng,lat],...]}}) - da
     * xac nhan bang curl thuc te voi key that (tai lieu VietMap mo ta chua chinh xac diem
     * nay). coordinates dung thu tu GeoJSON chuan [lng, lat], NGUOC voi thu tu [lat, lng] o
     * hau het cho khac trong API nay - xem MapService.route() noi dao lai thu tu.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VietMapGeometry(String type, List<List<BigDecimal>> coordinates) {
    }
}
