package vn.taskconnect.map.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cau hinh goi VietMap (maps.vietmap.vn), doc tu prefix {@code vietmap.*} trong
 * application.yml - anh xa tu bien moi truong VIETMAP_API_KEY qua .env. apiKey khong co gia
 * tri default (fail-fast giong GEMINI_API_KEY/GROQ_API_KEY o module AI) vi day la khoa goi
 * API co quota that ra ngoai.
 *
 * @param apiKey khoa "Key API" cua VietMap, BAT BUOC set qua bien moi truong VIETMAP_API_KEY.
 *               Rieng biet voi "Key tilemap" (VITE_VIETMAP_TILEMAP_KEY ben task-connect-fe) -
 *               key do dung de frontend tai tile/style ban do truc tiep, khong di qua module nay.
 * @param baseUrl goc URL cua VietMap API, mac dinh https://maps.vietmap.vn.
 */
@ConfigurationProperties(prefix = "vietmap")
public record VietMapProperties(String apiKey, String baseUrl) {
}
