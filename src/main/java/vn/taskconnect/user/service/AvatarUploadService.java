package vn.taskconnect.user.service;

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
import vn.taskconnect.user.dto.request.AvatarUploadUrlRequest;
import vn.taskconnect.user.dto.response.AvatarUploadUrlResponse;

/**
 * Sinh presigned URL de chinh chu tai khoan tu tai anh dai dien len S3 (khong upload qua
 * backend). Key object luon do server sinh (khong nhan tu client) de tranh path traversal
 * hoac ghi de file cua tai khoan khac.
 */
@Service
public class AvatarUploadService {

    /** URL upload chi co hieu luc ngan - du de client PUT ngay sau khi xin, khong de lo lau. */
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(5);

    private final S3PresignedUploadService s3Service;
    private final Clock clock;

    public AvatarUploadService(S3PresignedUploadService s3Service, Clock clock) {
        this.s3Service = s3Service;
        this.clock = clock;
    }

    /**
     * Kiem tra contentType nam trong whitelist, sinh object key rieng cho tai khoan trong
     * prefix "avatars/" (public-read theo bucket policy, xem ADR-003), roi tra ve presigned
     * PUT URL kem URL cong khai de client luu lai sau khi upload thanh cong.
     */
    public AvatarUploadUrlResponse createUploadUrl(UUID accountId, AvatarUploadUrlRequest request) {
        String contentType = ImageContentTypes.normalize(request.contentType());
        String extension = ImageContentTypes.extensionFor(contentType);
        if (extension == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_AVATAR_TYPE);
        }
        String objectKey = "avatars/%s/%s.%s".formatted(accountId, UUID.randomUUID(), extension);
        PresignedUpload upload = s3Service.createPresignedPutUrl(objectKey, contentType, UPLOAD_URL_TTL);
        Instant expiresAt = clock.instant().plus(UPLOAD_URL_TTL);
        return new AvatarUploadUrlResponse(upload.uploadUrl(), upload.publicUrl(), expiresAt);
    }
}
