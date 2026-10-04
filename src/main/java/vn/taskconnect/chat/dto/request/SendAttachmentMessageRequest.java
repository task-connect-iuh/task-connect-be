package vn.taskconnect.chat.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import vn.taskconnect.chat.api.ChatMessageType;

/**
 * Du lieu tao 1 tin nhan IMAGE/FILE/VIDEO SAU KHI client da PUT xong tung file len S3 (dung
 * objectKey da xin qua ChatAttachmentUploadUrlRequest) - dung cho POST
 * /api/v1/chat/applications/{applicationId}/attachment-messages. Gioi han so luong/dung luong
 * cu the (vd toi da 10 anh) doc dong tu admin_system_parameters o ChatService, KHONG hardcode o
 * day (xem 02-source-of-truth.md) - @NotEmpty chi dam bao co it nhat 1 file, khong dien ta gioi
 * han nghiep vu.
 */
public record SendAttachmentMessageRequest(
        @NotNull ChatMessageType messageType,
        @Size(max = 2000) String caption,
        UUID replyToMessageId,
        @NotEmpty @Valid List<AttachmentInput> attachments
) {

    /** 1 file da upload xong len S3, client gui lai dung nhung gi ChatAttachmentUploadUrlResponse da tra ve truoc do. */
    public record AttachmentInput(
            @NotBlank String objectKey,
            @NotBlank @Size(max = 255) String fileName,
            @NotBlank String mimeType,
            @Positive long fileSizeBytes
    ) {
    }
}
