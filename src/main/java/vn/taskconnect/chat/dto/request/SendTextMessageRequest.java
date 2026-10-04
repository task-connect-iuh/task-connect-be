package vn.taskconnect.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Du lieu gui 1 tin nhan TEXT, dung cho POST /api/v1/chat/applications/{applicationId}/messages.
 * replyToMessageId tuy chon (them 2026-09-26) - tra loi (quote) 1 tin nhan khac cung kenh.
 */
public record SendTextMessageRequest(
        @NotBlank @Size(max = 2000) String text,
        UUID replyToMessageId
) {
}
