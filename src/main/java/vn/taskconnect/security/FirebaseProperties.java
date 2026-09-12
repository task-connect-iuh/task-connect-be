package vn.taskconnect.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cau hinh xac minh so dien thoai qua Firebase Phone Auth, doc tu {@code app.firebase.*}
 * trong application.yml (anh xa tu FIREBASE_PROJECT_ID/FIREBASE_SERVICE_ACCOUNT_JSON_BASE64
 * qua .env).
 *
 * @param projectId               Project ID cua Firebase project - dung de kiem tra ID token
 *                                 duoc phat cho dung project nay, xem
 *                                 security/firebase/FirebaseTokenVerifierService.java.
 * @param serviceAccountJsonBase64 Noi dung file JSON service-account (Firebase Console >
 *                                 Project Settings > Service accounts > Generate new private
 *                                 key), encode base64 tren mot dong - dung de khoi tao
 *                                 FirebaseApp voi quyen Admin SDK verify ID token server-side.
 */
@ConfigurationProperties(prefix = "app.firebase")
public record FirebaseProperties(String projectId, String serviceAccountJsonBase64) {
}
