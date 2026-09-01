package vn.taskconnect.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Yeu cau xin presigned URL de tai anh dai dien len S3, dung cho
 * POST /users/me/avatar-upload-url. contentType duoc kiem tra theo whitelist trong
 * AvatarUploadService (khong dung Bean Validation vi danh sach cho phep la nghiep vu,
 * co the doi khac voi @Pattern tinh).
 */
public record AvatarUploadUrlRequest(@NotBlank String contentType) {
}
