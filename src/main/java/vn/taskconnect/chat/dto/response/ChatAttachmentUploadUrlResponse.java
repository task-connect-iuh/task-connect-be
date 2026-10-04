package vn.taskconnect.chat.dto.response;

import java.time.Instant;

/**
 * Ket qua xin presigned PUT URL cho 1 file dinh kem chat - khong co publicUrl (prefix rieng tu,
 * giong mau ADR-004), client phai gui lai dung objectKey trong SendAttachmentMessageRequest sau
 * khi PUT thanh cong.
 */
public record ChatAttachmentUploadUrlResponse(
        String uploadUrl,
        String objectKey,
        Instant expiresAt
) {
}
