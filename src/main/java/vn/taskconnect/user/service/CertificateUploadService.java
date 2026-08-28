package vn.taskconnect.user.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.common.storage.ImageContentTypes;
import vn.taskconnect.common.storage.S3PresignedUploadService;
import vn.taskconnect.common.storage.S3PresignedUploadService.PresignedUpload;
import vn.taskconnect.user.dto.request.CertificateUploadUrlRequest;
import vn.taskconnect.user.dto.response.CertificateUploadUrlResponse;

/**
 * Sinh presigned URL de Tasker tu tai file chung chi (anh hoac PDF) len S3, prefix rieng
 * "certificates/" - giong KYC (Buoc 4), khong public-read, can presigned GET rieng khi Admin
 * xem. Whitelist rong hon anh CCCD vi chung chi thuong la file scan PDF, khong chi anh chup.
 */
@Service
public class CertificateUploadService {

    /** Phan mo rong rieng cho PDF - ImageContentTypes (dung chung voi avatar/KYC) khong co dinh dang nay. */
    private static final Map<String, String> EXTRA_ALLOWED_CONTENT_TYPES = Map.of("application/pdf", "pdf");

    /** URL upload chi co hieu luc ngan - du de client PUT ngay sau khi xin, khong de lo lau. */
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(5);

    private final S3PresignedUploadService s3Service;
    private final Clock clock;

    public CertificateUploadService(S3PresignedUploadService s3Service, Clock clock) {
        this.s3Service = s3Service;
        this.clock = clock;
    }

    /**
     * Kiem tra contentType nam trong whitelist (anh dung chung voi KYC/avatar, cong them
     * PDF), sinh object key rieng cho tai khoan + category trong prefix
     * "certificates/{accountId}/{categoryId}/". Chi tra ve uploadUrl va objectKey, khong co
     * publicUrl - client phai gui lai objectKey nguyen ven trong SubmitSkillRequest.
     */
    public CertificateUploadUrlResponse createUploadUrl(UUID accountId, UUID categoryId,
            CertificateUploadUrlRequest request) {
        String contentType = ImageContentTypes.normalize(request.contentType());
        String extension = ImageContentTypes.extensionFor(contentType);
        if (extension == null) {
            extension = EXTRA_ALLOWED_CONTENT_TYPES.get(contentType);
        }
        if (extension == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_CERTIFICATE_FILE_TYPE);
        }
        String objectKey = "certificates/%s/%s/%s.%s".formatted(accountId, categoryId, UUID.randomUUID(), extension);
        PresignedUpload upload = s3Service.createPresignedPutUrl(objectKey, contentType, UPLOAD_URL_TTL);
        Instant expiresAt = clock.instant().plus(UPLOAD_URL_TTL);
        return new CertificateUploadUrlResponse(upload.uploadUrl(), objectKey, expiresAt);
    }
}
