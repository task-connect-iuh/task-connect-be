package vn.taskconnect.task.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Yeu cau xin presigned URL de tai 1 anh minh chung chi phi phat sinh len S3, dung cho
 * POST /tasks/extra-cost-images/upload-url - cung co che voi TaskImageUploadUrlRequest.
 */
public record ExtraCostImageUploadUrlRequest(@NotBlank String contentType) {
}
