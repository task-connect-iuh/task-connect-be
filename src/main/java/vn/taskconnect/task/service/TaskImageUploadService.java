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
import vn.taskconnect.task.dto.request.TaskImageUploadUrlRequest;
import vn.taskconnect.task.dto.response.TaskImageUploadUrlResponse;

/**
 * Sinh presigned URL de Task Poster tu tai mot anh minh hoa cong viec len S3 (khong upload
 * qua backend) - cung co che voi AvatarUploadService (user module). Key object do server
 * sinh, nam trong prefix "tasks/" - can bucket policy public-read cho prefix nay (giong
 * "avatars/*" da cau hinh o Buoc 1 module User), xem docs/PROGRESS-TASK-POSTER-MODULE.md.
 */
@Service
public class TaskImageUploadService {

    /** URL upload chi co hieu luc ngan - du de client PUT ngay sau khi xin, khong de lo lau. */
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(5);

    private final S3PresignedUploadService s3Service;
    private final Clock clock;

    public TaskImageUploadService(S3PresignedUploadService s3Service, Clock clock) {
        this.s3Service = s3Service;
        this.clock = clock;
    }

    /**
     * Kiem tra contentType nam trong whitelist, sinh object key rieng cho Poster trong
     * prefix "tasks/", roi tra ve presigned PUT URL kem URL cong khai de client gom vao
     * imageUrls cua CreateTaskRequest sau khi upload thanh cong.
     */
    public TaskImageUploadUrlResponse createUploadUrl(UUID posterId, TaskImageUploadUrlRequest request) {
        String contentType = ImageContentTypes.normalize(request.contentType());
        String extension = ImageContentTypes.extensionFor(contentType);
        if (extension == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_TASK_IMAGE_TYPE);
        }
        String objectKey = "tasks/%s/%s.%s".formatted(posterId, UUID.randomUUID(), extension);
        PresignedUpload upload = s3Service.createPresignedPutUrl(objectKey, contentType, UPLOAD_URL_TTL);
        Instant expiresAt = clock.instant().plus(UPLOAD_URL_TTL);
        return new TaskImageUploadUrlResponse(upload.uploadUrl(), upload.publicUrl(), expiresAt);
    }
}
