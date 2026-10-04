package vn.taskconnect.chat.dto.response;

import java.util.UUID;

/** 1 emoji ma 1 tai khoan da tha vao 1 tin nhan - toi da 1 dong/tai khoan/tin nhan. */
public record ChatReactionResponse(
        UUID accountId,
        String accountName,
        String emoji
) {
}
