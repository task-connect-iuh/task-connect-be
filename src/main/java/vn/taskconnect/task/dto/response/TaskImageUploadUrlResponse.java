package vn.taskconnect.task.dto.response;

import java.time.Instant;

/**
 * Phan hoi cua POST /api/v1/tasks/images-upload-url: uploadUrl de client PUT truc tiep file
 * len S3 (dung mot lan, het han o expiresAt), publicUrl la dia chi doc lai sau khi upload
 * xong - client gom cac publicUrl nay vao imageUrls cua CreateTaskRequest khi dang cong viec.
 */
public record TaskImageUploadUrlResponse(String uploadUrl, String publicUrl, Instant expiresAt) {
}
