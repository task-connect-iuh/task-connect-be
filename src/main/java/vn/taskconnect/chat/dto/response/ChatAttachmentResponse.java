package vn.taskconnect.chat.dto.response;

/**
 * 1 file dinh kem cua 1 tin nhan IMAGE/FILE/VIDEO. url la presigned GET MOI KY LAI moi lan tra
 * ve (prefix rieng tu, khong public-read - xem ChatService.toAttachmentResponse), khong phai
 * URL vinh vien - client khong nen luu cache lau dai.
 */
public record ChatAttachmentResponse(
        String url,
        String fileName,
        String mimeType,
        long fileSizeBytes,
        int sortOrder
) {
}
