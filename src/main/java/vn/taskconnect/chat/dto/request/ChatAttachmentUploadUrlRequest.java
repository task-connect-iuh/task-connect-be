package vn.taskconnect.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vn.taskconnect.chat.api.ChatMessageType;

/**
 * Du lieu xin 1 presigned PUT URL de tu tai 1 file dinh kem chat len S3, dung cho
 * POST /api/v1/chat/applications/{applicationId}/attachment-upload-url. kind phai la
 * IMAGE/FILE/VIDEO (400 neu truyen loai khac - xem ChatAttachmentUploadService).
 */
public record ChatAttachmentUploadUrlRequest(
        @NotBlank String contentType,
        @NotNull ChatMessageType kind
) {
}
