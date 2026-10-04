package vn.taskconnect.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Du lieu tha 1 emoji vao 1 tin nhan, dung cho POST
 * /api/v1/chat/applications/{applicationId}/messages/{messageId}/reactions. Tha lai cung emoji
 * da tha truoc do se BO tha (toggle) - xem ChatService.reactToMessage().
 */
public record AddReactionRequest(
        @NotBlank @Size(max = 32) String emoji
) {
}
