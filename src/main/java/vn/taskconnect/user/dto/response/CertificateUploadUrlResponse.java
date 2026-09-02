package vn.taskconnect.user.dto.response;

import java.time.Instant;

/**
 * Phan hoi cua POST /users/me/tasker-skills/{categoryId}/certificate-upload-url. Khong co
 * publicUrl vi file chung chi la du lieu rieng tu, khong public-read - objectKey phai duoc
 * client gui lai nguyen ven trong SubmitSkillRequest.
 */
public record CertificateUploadUrlResponse(String uploadUrl, String objectKey, Instant expiresAt) {
}
