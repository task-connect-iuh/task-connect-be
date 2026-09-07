package vn.taskconnect.task.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Yeu cau xin presigned URL de tai 1 anh minh hoa cong viec len S3, dung cho
 * POST /api/v1/tasks/images-upload-url. contentType duoc kiem tra theo whitelist trong
 * TaskImageUploadService (khong dung Bean Validation vi danh sach cho phep la nghiep vu),
 * cung mau AvatarUploadUrlRequest (user module).
 */
public record TaskImageUploadUrlRequest(@NotBlank String contentType) {
}
