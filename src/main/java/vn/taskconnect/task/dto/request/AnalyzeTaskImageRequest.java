package vn.taskconnect.task.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Yeu cau phan tich mot anh minh hoa cong viec DA TAI LEN S3 truoc do (qua
 * POST /tasks/images-upload-url) de goi y tieu de/mo ta/danh muc, dung cho
 * POST /api/v1/tasks/analyze-image. imageUrl phai la publicUrl vua nhan duoc tu buoc upload -
 * TaskImageFetcher se tu choi moi URL khong dung prefix cua chinh Poster dang goi.
 */
public record AnalyzeTaskImageRequest(@NotBlank String imageUrl) {
}
