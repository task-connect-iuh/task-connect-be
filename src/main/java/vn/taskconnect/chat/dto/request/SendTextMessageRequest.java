package vn.taskconnect.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Du lieu gui 1 tin nhan TEXT, dung cho POST /api/v1/chat/applications/{applicationId}/messages. */
public record SendTextMessageRequest(
        @NotBlank @Size(max = 2000) String text
) {
}
