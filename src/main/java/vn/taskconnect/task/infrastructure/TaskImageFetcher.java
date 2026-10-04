package vn.taskconnect.task.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.taskconnect.common.storage.ImageContentTypes;
import vn.taskconnect.common.storage.S3Properties;

/**
 * Tai ve byte cua mot anh minh hoa cong viec da duoc chinh Poster tai len S3 truoc do (qua
 * TaskImageUploadService), dung cho tinh nang "Dien tu dong tu anh" (goi AiFacade.suggestTaskFromImage()).
 *
 * <p>BAT BUOC kiem tra imageUrl NAM DUNG TRONG prefix object key rieng cua chinh posterId dang
 * goi ("tasks/{posterId}/") truoc khi fetch - day la lop chan SSRF va chan xem anh cua Poster
 * khac: khong tin bat ky URL nao FE gui len, chi cho phep doc lai chinh bucket/prefix cua minh,
 * KHONG fetch duoc URL bat ky ngoai Internet hay object cua tai khoan khac.
 */
@Component
public class TaskImageFetcher {

    /** Tran kich thuoc anh cho phep phan tich - Gemini inline_data gioi han ~20MB, dat thap hon nhieu vi day chi la anh dien thoai chup. */
    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;

    private final S3Properties s3Properties;
    private final RestClient restClient;

    public TaskImageFetcher(S3Properties s3Properties) {
        this.s3Properties = s3Properties;
        this.restClient = RestClient.create();
    }

    /**
     * Tra ve byte + content type cua anh neu imageUrl hop le (dung bucket/region cau hinh, dung
     * prefix "tasks/{posterId}/" cua chinh nguoi goi, tai ve thanh cong, khong vuot MAX_IMAGE_BYTES).
     * Rong trong moi truong hop con lai - nguoi goi (TaskService) tra loi INVALID_TASK_IMAGE_URL
     * cho URL sai chu so huu/dinh dang, hoac coi nhu AI khong phan tich duoc neu fetch that bai.
     */
    public Optional<FetchedImage> fetchOwnedTaskImage(UUID posterId, String imageUrl) {
        String expectedPrefix = "https://%s.s3.%s.amazonaws.com/tasks/%s/"
                .formatted(s3Properties.bucket(), s3Properties.region(), posterId);
        if (!imageUrl.startsWith(expectedPrefix)) {
            return Optional.empty();
        }
        try {
            ResponseEntity<byte[]> response = restClient.get()
                    .uri(imageUrl)
                    .retrieve()
                    .toEntity(byte[].class);
            byte[] bytes = response.getBody();
            if (bytes == null || bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
                return Optional.empty();
            }
            String contentType = ImageContentTypes.normalize(
                    response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE) != null
                            ? response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE) : "");
            if (ImageContentTypes.extensionFor(contentType) == null) {
                return Optional.empty();
            }
            return Optional.of(new FetchedImage(bytes, contentType));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /** Byte anh da tai ve + content type da xac minh nam trong whitelist - dung lam input cho AiFacade. */
    public record FetchedImage(byte[] bytes, String mimeType) {
    }
}
