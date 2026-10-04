package vn.taskconnect.chat.infrastructure;

import java.util.Map;
import vn.taskconnect.chat.api.ChatMessageType;
import vn.taskconnect.common.storage.ImageContentTypes;

/**
 * Whitelist dinh dang file rieng cho tin nhan VIDEO/FILE/VOICE trong chat (IMAGE dung chung
 * ImageContentTypes voi avatar/KYC/chung chi) - dung boi ChatAttachmentUploadService khi xin
 * presigned URL. Chi anh huong den viec CO xin duoc URL hay khong (400 neu sai dinh dang), gioi
 * han dung luong/so luong nam o admin_system_parameters, doc qua AdminFacade trong ChatService.
 */
public final class ChatAttachmentContentTypes {

    /** Video: chi mp4 (H.264/AAC pho bien nhat, phat duoc truc tiep bang the <video> tren trinh duyet). */
    private static final Map<String, String> VIDEO_CONTENT_TYPES = Map.of("video/mp4", "mp4");

    /**
     * Voice (tin nhan thoai, them 2026-09-26): webm la dinh dang MediaRecorder API mac dinh cua
     * Chrome/Edge/Firefox, mp4 (AAC) danh cho Safari/iOS ghi ra dinh dang khac - ca hai deu phat
     * duoc truc tiep bang the <audio> tren trinh duyet.
     */
    private static final Map<String, String> VOICE_CONTENT_TYPES = Map.of(
            "audio/webm", "webm",
            "audio/mp4", "m4a");

    /** File tai lieu: cac dinh dang van phong/nen pho bien, khong bao gom anh (da co IMAGE rieng). */
    private static final Map<String, String> FILE_CONTENT_TYPES = Map.of(
            "application/pdf", "pdf",
            "application/msword", "doc",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx",
            "application/vnd.ms-excel", "xls",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx",
            "application/zip", "zip");

    private ChatAttachmentContentTypes() {
    }

    /**
     * Phan mo rong tuong ung voi (kind, contentType da chuan hoa qua ImageContentTypes.normalize),
     * null neu khong nam trong whitelist cua dung loai do. kind phai la IMAGE/FILE/VIDEO/VOICE -
     * goi voi TEXT/SYSTEM/PRICE_PROPOSAL/RESCHEDULE_PROPOSAL luon tra ve null (khong co whitelist).
     */
    public static String extensionFor(ChatMessageType kind, String normalizedContentType) {
        return switch (kind) {
            case IMAGE -> ImageContentTypes.extensionFor(normalizedContentType);
            case VIDEO -> VIDEO_CONTENT_TYPES.get(normalizedContentType);
            case FILE -> FILE_CONTENT_TYPES.get(normalizedContentType);
            case VOICE -> VOICE_CONTENT_TYPES.get(normalizedContentType);
            default -> null;
        };
    }
}
