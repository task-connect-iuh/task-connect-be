package vn.taskconnect.task.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.common.storage.ImageContentTypes;
import vn.taskconnect.common.storage.S3PresignedUploadService;
import vn.taskconnect.common.storage.S3PresignedUploadService.PresignedUpload;
import vn.taskconnect.task.dto.request.ExtraCostImageUploadUrlRequest;
import vn.taskconnect.task.dto.response.ExtraCostImageUploadUrlResponse;

/**
 * Sinh presigned URL de Tasker tu tai 1 anh minh chung chi phi phat sinh len S3 (khong upload
 * qua backend) - cung co che voi TaskImageUploadService. Key object nam trong prefix
 * "extra-costs/" - can bucket policy public-read cho prefix nay (giong "tasks/*"), xem
 * docs/adr/ADR-003-avatar-storage-s3-presigned-upload.md.
 */
@Service
public class TaskExtraCostImageUploadService {

    /** URL upload chi co hieu luc ngan - du de client PUT ngay sau khi xin, khong de lo lau. */
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(5);

    private final S3PresignedUploadService s3Service;
    private final Clock clock;

    public TaskExtraCostImageUploadService(S3PresignedUploadService s3Service, Clock clock) {
        this.s3Service = s3Service;
        this.clock = clock;
    }

    /**
     * Kiem tra contentType nam trong whitelist, sinh object key rieng cho applicationId trong
     * prefix "extra-costs/", roi tra ve presigned PUT URL kem URL cong khai de client gan vao
     * photoUrl cua 1 ExtraCostItemInput sau khi upload thanh cong.
     */
    public ExtraCostImageUploadUrlResponse createUploadUrl(UUID applicationId, ExtraCostImageUploadUrlRequest request) {
        String contentType = ImageContentTypes.normalize(request.contentType());
        String extension = ImageContentTypes.extensionFor(contentType);
        if (extension == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_TASK_IMAGE_TYPE);
        }
        String objectKey = "extra-costs/%s/%s.%s".formatted(applicationId, UUID.randomUUID(), extension);
        PresignedUpload upload = s3Service.createPresignedPutUrl(objectKey, contentType, UPLOAD_URL_TTL);
        Instant expiresAt = clock.instant().plus(UPLOAD_URL_TTL);
        return new ExtraCostImageUploadUrlResponse(upload.uploadUrl(), upload.publicUrl(), expiresAt);
    }
}
