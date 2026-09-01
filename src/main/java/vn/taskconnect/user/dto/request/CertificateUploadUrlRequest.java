package vn.taskconnect.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Yeu cau xin presigned URL de tai file chung chi len S3, dung cho
 * POST /users/me/tasker-skills/{categoryId}/certificate-upload-url. contentType kiem tra
 * whitelist trong CertificateUploadService (anh + PDF), khong dung Bean Validation - cung
 * ly do nhu AvatarUploadUrlRequest/KycUploadUrlRequest.
 */
public record CertificateUploadUrlRequest(@NotBlank String contentType) {
}
