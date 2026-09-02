package vn.taskconnect.user.dto.response;

import java.time.Instant;

/**
 * Phan hoi cua POST /users/me/avatar-upload-url: uploadUrl de client PUT truc tiep file
 * len S3 (dung mot lan, het han o expiresAt), publicUrl la dia chi doc lai sau khi upload
 * xong - client tu gui publicUrl nay vao PATCH /users/me de luu avatarUrl.
 */
public record AvatarUploadUrlResponse(String uploadUrl, String publicUrl, Instant expiresAt) {
}
