package vn.taskconnect.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Yeu cau xin presigned URL de tai anh CCCD (mat truoc hoac mat sau) len S3, dung cho
 * POST /users/me/kyc-verifications/upload-url. contentType kiem tra whitelist trong
 * KycUploadService, khong dung Bean Validation - cung ly do nhu AvatarUploadUrlRequest.
 */
public record KycUploadUrlRequest(@NotNull KycImageSide side, @NotBlank String contentType) {
}
