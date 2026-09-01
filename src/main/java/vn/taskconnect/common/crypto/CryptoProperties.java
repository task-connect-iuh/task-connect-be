package vn.taskconnect.common.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cau hinh khoa ma hoa AES-256 dung chung cho moi cot _enc trong CSDL (CCCD, so chung
 * chi, URL file KYC/chung chi...), doc tu {@code app.crypto.key} - anh xa tu bien moi
 * truong APP_CRYPTO_KEY qua .env. Buoc 4 (KYC) va Buoc 6 (chung chi) dung lai
 * {@link AesEncryptionService} duoc cau hinh boi properties nay.
 *
 * @param key chuoi Base64 giai ma ra dung 32 byte (AES-256). Sinh khoa moi:
 *            {@code openssl rand -base64 32}.
 */
@ConfigurationProperties(prefix = "app.crypto")
public record CryptoProperties(String key) {
}
