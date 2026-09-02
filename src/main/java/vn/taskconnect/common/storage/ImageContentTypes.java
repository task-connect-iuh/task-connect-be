package vn.taskconnect.common.storage;

import java.util.Locale;
import java.util.Map;

/**
 * Whitelist dinh dang anh dung chung cho moi luong xin presigned URL upload anh qua S3
 * (avatar - Buoc 1, CCCD KYC - Buoc 4, chung chi hanh nghe - Buoc 6) - tranh dinh nghia lai
 * cung mot whitelist o nhieu noi.
 */
public final class ImageContentTypes {

    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private ImageContentTypes() {
    }

    /**
     * Chuan hoa ve dang "type/subtype" thuong, bo tham so di kem (vd "; charset=UTF-8") -
     * tranh tu choi nham mot content type hop le chi khac hoa/thuong hoac co tham so.
     */
    public static String normalize(String contentType) {
        int paramIndex = contentType.indexOf(';');
        String base = paramIndex >= 0 ? contentType.substring(0, paramIndex) : contentType;
        return base.trim().toLowerCase(Locale.ROOT);
    }

    /** Phan mo rong file tuong ung voi content type da chuan hoa, null neu khong nam trong whitelist. */
    public static String extensionFor(String normalizedContentType) {
        return ALLOWED_CONTENT_TYPES.get(normalizedContentType);
    }
}
