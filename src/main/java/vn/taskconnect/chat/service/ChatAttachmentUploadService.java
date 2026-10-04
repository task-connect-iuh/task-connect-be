package vn.taskconnect.chat.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.taskconnect.chat.api.ChatMessageType;
import vn.taskconnect.chat.dto.request.ChatAttachmentUploadUrlRequest;
import vn.taskconnect.chat.dto.response.ChatAttachmentUploadUrlResponse;
import vn.taskconnect.chat.infrastructure.ChatAttachmentContentTypes;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.common.storage.ImageContentTypes;
import vn.taskconnect.common.storage.S3PresignedUploadService;
import vn.taskconnect.common.storage.S3PresignedUploadService.PresignedUpload;

/**
 * Sinh presigned URL de mot ben trong 1 cuoc chat tu tai anh/video/file/tin nhan thoai len S3, prefix rieng tu
 * "chat-attachments/{applicationId}/" - CUNG mau voi certificates/kyc (ADR-004: khong
 * public-read, phai xem qua presigned GET ngan han, xem ChatService.toAttachmentResponse), KHAC
 * avatar (khong co publicUrl vinh vien) vi noi dung trao doi giua 2 ben khong nen cong khai.
 * IAM user backend hien tai (tao tu ADR-003, mo rong o ADR-004 cho kyc/certificates) can duoc
 * mo rong THEM quyen PutObject/GetObject/DeleteObject cho prefix "chat-attachments/*" tren AWS
 * Console truoc khi tinh nang nay chay duoc voi S3 that - viec nay ngoai repo, xem
 * docs/PROGRESS-CHAT-MODULE.md.
 */
@Service
public class ChatAttachmentUploadService {

    /** URL upload chi co hieu luc ngan - du de client PUT ngay sau khi xin, khong de lo lau. */
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(5);

    private final S3PresignedUploadService s3Service;
    private final Clock clock;

    public ChatAttachmentUploadService(S3PresignedUploadService s3Service, Clock clock) {
        this.s3Service = s3Service;
        this.clock = clock;
    }

    /**
     * Kiem tra kind phai la IMAGE/FILE/VIDEO va contentType nam trong whitelist tuong ung
     * (ChatAttachmentContentTypes), sinh object key rieng cho application trong prefix
     * "chat-attachments/{applicationId}/". Chi tra ve uploadUrl + objectKey (khong publicUrl) -
     * client phai gui lai objectKey nguyen ven trong SendAttachmentMessageRequest.
     */
    public ChatAttachmentUploadUrlResponse createUploadUrl(UUID applicationId, ChatAttachmentUploadUrlRequest request) {
        ChatMessageType kind = request.kind();
        if (kind != ChatMessageType.IMAGE && kind != ChatMessageType.FILE && kind != ChatMessageType.VIDEO
                && kind != ChatMessageType.VOICE) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_ATTACHMENT_TYPE);
        }
        String contentType = ImageContentTypes.normalize(request.contentType());
        String extension = ChatAttachmentContentTypes.extensionFor(kind, contentType);
        if (extension == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_ATTACHMENT_TYPE);
        }
        String objectKey = "chat-attachments/%s/%s.%s".formatted(applicationId, UUID.randomUUID(), extension);
        PresignedUpload upload = s3Service.createPresignedPutUrl(objectKey, contentType, UPLOAD_URL_TTL);
        Instant expiresAt = clock.instant().plus(UPLOAD_URL_TTL);
        return new ChatAttachmentUploadUrlResponse(upload.uploadUrl(), objectKey, expiresAt);
    }
}
