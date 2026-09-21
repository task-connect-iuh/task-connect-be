package vn.taskconnect.map.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import vn.taskconnect.map.dto.response.AddressSuggestionResponse;
import vn.taskconnect.map.dto.response.GeocodeResultResponse;

/**
 * Goi Photon (photon.komoot.io) - mien phi, khong can API key, du lieu nen la OpenStreetMap.
 * CHI dung khi VietMap khong kha dung sau khi da retry (het quota/sap he thong, xem
 * MapService.callVietMapWithFallback) - day la duong du phong, KHONG phai duong chinh. Port
 * lai logic tung nam o task-connect-fe/src/utils/geocoding.ts truoc khi doi sang VietMap (xem
 * docs/PROGRESS-FE-USER-MODULE.md), gio chuyen vao backend vi frontend khong con tu goi
 * Photon truc tiep nua.
 */
@Component
public class PhotonClient {

    private static final String REVERSE_URL = "https://photon.komoot.io/reverse";
    private static final String SEARCH_URL = "https://photon.komoot.io/api";

    // Photon doi khi tra ten hanh chinh Viet Nam kem tien to/hau to tieng Phap (du lieu OSM
    // quoc te gan them ten kieu thuoc dia cu cho vai tinh/thanh lon) thay vi thuan Viet, vd
    // "Province de Vinh Long" hay "Hô Chi Minh-Ville". Lam sach tot nhat co the bang regex, va
    // doi rieng ten 2 thanh pho hay gap nhat bang bang tra thu cong (regex khong sua duoc phan
    // dau/thanh dieu thieu cua chinh ten, vd "Hô Chi Minh" van thieu dau so voi "Hồ Chí Minh").
    private static final Pattern FRENCH_ADMIN_AFFIXES =
            Pattern.compile("^(Province|Département|Ville) (de |du |des |d')|-Ville$", Pattern.CASE_INSENSITIVE);
    private static final Map<String, String> KNOWN_NAME_FIXES = Map.of(
            "hô chi minh", "Hồ Chí Minh",
            "ho chi minh", "Hồ Chí Minh",
            "hanoi", "Hà Nội",
            "hanoï", "Hà Nội");

    private final RestClient restClient = RestClient.create();

    /** Toa do -> dia chi gan nhat qua Photon /reverse. supported=false neu ngoai Viet Nam/khong co du lieu. */
    public GeocodeResultResponse reverseGeocode(BigDecimal lat, BigDecimal lng) {
        URI uri = UriComponentsBuilder.fromHttpUrl(REVERSE_URL)
                .queryParam("lon", lng)
                .queryParam("lat", lat)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
        PhotonResponse response = restClient.get().uri(uri).retrieve().body(PhotonResponse.class);
        PhotonFeature first = response == null || response.features() == null || response.features().isEmpty()
                ? null : response.features().get(0);
        PhotonProperties properties = first == null ? null : first.properties();
        if (properties == null || !"VN".equals(properties.countrycode())) {
            return new GeocodeResultResponse("", "", false, null, null);
        }
        AddressParts parts = toAddressParts(properties);
        List<Double> coordinates = first.geometry() == null ? null : first.geometry().coordinates();
        Double resultLat = coordinates != null && coordinates.size() >= 2 ? coordinates.get(1) : lat.doubleValue();
        Double resultLng = coordinates != null && coordinates.size() >= 2 ? coordinates.get(0) : lng.doubleValue();
        return new GeocodeResultResponse(parts.addressText(), parts.operatingArea(), true, resultLat, resultLng);
    }

    /**
     * Goi y dia chi qua Photon /api (khac /reverse) - CO KEM toa do truc tiep trong ket qua
     * (khac VietMap Autocomplete v4), nen tra lat/lng ngay trong AddressSuggestionResponse va
     * refId null - frontend nhan dien fallback qua lat/lng khac null, bo qua buoc goi
     * GET /map/place (xem geocoding.ts resolveSuggestion()).
     */
    public List<AddressSuggestionResponse> search(String text) {
        URI uri = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                .queryParam("q", text)
                .queryParam("limit", 8)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
        PhotonResponse response = restClient.get().uri(uri).retrieve().body(PhotonResponse.class);
        if (response == null || response.features() == null) return List.of();

        return response.features().stream()
                .filter(f -> f.properties() != null && "VN".equals(f.properties().countrycode())
                        && f.geometry() != null && f.geometry().coordinates() != null
                        && f.geometry().coordinates().size() >= 2)
                .map(f -> {
                    AddressParts parts = toAddressParts(f.properties());
                    List<Double> coordinates = f.geometry().coordinates();
                    double lng = coordinates.get(0);
                    double lat = coordinates.get(1);
                    String label = String.join(", ", nonBlank(parts.addressText(), parts.operatingArea()));
                    if (label.isBlank()) label = nullToEmpty(f.properties().name());
                    return new AddressSuggestionResponse("photon:" + lat + "," + lng, label, parts.addressText(),
                            parts.operatingArea(), lat, lng);
                })
                .filter(s -> !s.label().isBlank())
                .toList();
    }

    private record AddressParts(String addressText, String operatingArea) {
    }

    /**
     * Ghep addressText/operatingArea tu properties tho cua Photon - dung chung cho
     * reverseGeocode va search, de 2 luong cho ra cung dinh dang du lieu.
     */
    private AddressParts toAddressParts(PhotonProperties properties) {
        String streetLine = String.join(" ",
                nonBlank(properties.housenumber(), properties.street() != null ? properties.street() : properties.name()));
        String district = cleanAdminName(properties.district());
        String province = cleanAdminName(properties.city() != null ? properties.city() : properties.state());
        String operatingArea = String.join(", ", nonBlank(district, province));
        String addressText = String.join(", ", nonBlank(streetLine, properties.locality()));
        return new AddressParts(addressText, operatingArea);
    }

    private static String cleanAdminName(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        String stripped = FRENCH_ADMIN_AFFIXES.matcher(raw).replaceAll("").trim();
        String known = KNOWN_NAME_FIXES.get(stripped.toLowerCase());
        return known != null ? known : stripped;
    }

    /** Loc bo phan tu null/rong - KHONG dung List.of(values) truc tiep, ham do nem NPE ngay
     *  neu bat ky phan tu nao null (housenumber cua Photon thuong xuyen null). */
    private static List<String> nonBlank(String... values) {
        return java.util.Arrays.stream(values).filter(v -> v != null && !v.isBlank()).toList();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PhotonResponse(List<PhotonFeature> features) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PhotonFeature(PhotonProperties properties, PhotonGeometry geometry) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PhotonProperties(String name, String street, String housenumber, String locality, String district,
            String city, String state, String countrycode) {
    }

    /** GeoJSON Point: coordinates la [lon, lat] (khac thu tu [lat, lon] thong thuong). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PhotonGeometry(List<Double> coordinates) {
    }
}
