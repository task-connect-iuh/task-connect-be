package vn.taskconnect.common.storage;

import java.time.Duration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Sinh presigned URL de client tu upload file truc tiep len S3, khong di qua backend (file
 * khong chay qua tang ung dung, tranh ton bang thong va bo nho cho upload anh). Chi dung
 * cho object cong khai (vd avatar) - key phai nam trong prefix da cau hinh public-read qua
 * bucket policy, xem docs/adr/ADR-003-avatar-storage-s3-presigned-upload.md. Object rieng tu
 * (anh KYC/chung chi, Buoc 4/6) can presigned GET rieng, chua lam o day.
 */
@Service
public class S3PresignedUploadService {

    private final S3Presigner presigner;
    private final S3Properties properties;

    /**
     * {@code @Lazy} tren tham so nay bat buoc: S3Presigner that su duoc dung
     * (AwsBasicCredentials.create ban trong S3Config nem NullPointerException neu access
     * key/secret rong) chi khi presign duoc goi lan dau, khong phai luc Spring khoi tao
     * S3PresignedUploadService. Neu khong co @Lazy, moi @SpringBootTest boot full context -
     * ke ca test khong dung gi den S3 nhu cac test module Auth - se sap ngay tu luc khoi
     * dong ung dung o moi may/CI chua co AWS_S3_ACCESS_KEY_ID/SECRET that (phat hien boi
     * code review, xem docs/PROGRESS-USER-MODULE.md).
     */
    public S3PresignedUploadService(@Lazy S3Presigner presigner, S3Properties properties) {
        this.presigner = presigner;
        this.properties = properties;
    }

    /**
     * Tao presigned PUT URL cho dung mot object key, het han sau {@code ttl}. contentType
     * duoc ky vao chu ky - client PUT phai gui dung header Content-Type nay, khac di S3 tra
     * 403 (co tac dung nhu mot lop kiem tra loai file o phia S3, ngoai whitelist o tang
     * service goi ham nay).
     */
    public PresignedUpload createPresignedPutUrl(String objectKey, String contentType, Duration ttl) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(putRequest)
                .build();
        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        String publicUrl = "https://%s.s3.%s.amazonaws.com/%s"
                .formatted(properties.bucket(), properties.region(), objectKey);
        return new PresignedUpload(presigned.url().toString(), publicUrl);
    }

    /**
     * Tao presigned GET URL cho object rieng tu (khong public-read), dung khi Admin can xem
     * anh CCCD/chung chi (Buoc 4/6) - khac avatar, cac object nay khong co publicUrl vinh
     * vien, moi lan xem phai ky URL moi, het han sau {@code ttl}.
     */
    public String createPresignedGetUrl(String objectKey, Duration ttl) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getRequest)
                .build();
        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
        return presigned.url().toString();
    }

    /** URL de PUT file len (dung mot lan, het han sau ttl) va URL cong khai doc lai sau khi upload xong. */
    public record PresignedUpload(String uploadUrl, String publicUrl) {
    }
}
