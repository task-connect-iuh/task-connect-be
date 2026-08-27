package vn.taskconnect.user.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.common.storage.ImageContentTypes;
import vn.taskconnect.common.storage.S3PresignedUploadService;
import vn.taskconnect.common.storage.S3PresignedUploadService.PresignedUpload;
import vn.taskconnect.user.dto.request.KycUploadUrlRequest;
import vn.taskconnect.user.dto.response.KycUploadUrlResponse;

/**
 * Sinh presigned URL de chinh chu tai khoan tu tai anh CCCD (mat truoc/sau) len S3, dung
 * prefix rieng "kyc/" - khac "avatars/", prefix nay KHONG duoc cau hinh public-read trong
 * bucket policy (xem docs/adr/ADR-004-kyc-certificate-private-image-storage.md), can xin
 * presigned GET rieng khi Admin muon xem (S3PresignedUploadService.createPresignedGetUrl).
 */
@Service
public class KycUploadService {

    /** URL upload chi co hieu luc ngan - du de client PUT ngay sau khi xin, khong de lo lau. */
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(5);

    private final S3PresignedUploadService s3Service;
    private final Clock clock;

    public KycUploadService(S3PresignedUploadService s3Service, Clock clock) {
        this.s3Service = s3Service;
        this.clock = clock;
    }

    /**
     * Kiem tra contentType nam trong whitelist, sinh object key rieng cho tai khoan trong
     * prefix "kyc/{accountId}/", kem mat truoc/sau trong ten file de de phan biet. Chi tra
     * ve uploadUrl va objectKey (khong co publicUrl nhu avatar) - client phai gui lai
     * objectKey nguyen ven trong SubmitKycRequest.
     */
    public KycUploadUrlResponse createUploadUrl(UUID accountId, KycUploadUrlRequest request) {
        String contentType = ImageContentTypes.normalize(request.contentType());
        String extension = ImageContentTypes.extensionFor(contentType);
        if (extension == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_KYC_IMAGE_TYPE);
        }
        String side = request.side().name().toLowerCase(Locale.ROOT);
        String objectKey = "kyc/%s/%s-%s.%s".formatted(accountId, UUID.randomUUID(), side, extension);
        PresignedUpload upload = s3Service.createPresignedPutUrl(objectKey, contentType, UPLOAD_URL_TTL);
        Instant expiresAt = clock.instant().plus(UPLOAD_URL_TTL);
        return new KycUploadUrlResponse(upload.uploadUrl(), objectKey, expiresAt);
    }
}
