package vn.taskconnect.common.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * Ma hoa/giai ma AES-256-GCM dung chung cho cac cot _enc (VARBINARY) trong CSDL - CCCD
 * (Buoc 4), so chung chi va URL file chung chi (Buoc 6). GCM la ma hoa co xac thuc
 * (authenticated encryption): tu phat hien du lieu bi sua doi luc giai ma (nem
 * IllegalStateException), khac voi ma hoa don thuan kieu CBC khong xac thuc duoc.
 *
 * <p>Dinh dang blob luu trong cot _enc: 12 byte IV (sinh ngau nhien, khac nhau moi lan
 * ma hoa) noi truc tiep voi ciphertext+tag phia sau - tu chua du du lieu de giai ma lai,
 * khong can them cot rieng luu IV.
 */
@Service
public class AesEncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    // 12 byte la do dai IV khuyen nghi chuan cho GCM (NIST SP 800-38D) - do dai khac van
    // chay duoc ve mat ky thuat nhung lam giam bao dam an toan cua che do nay.
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int AES_256_KEY_LENGTH_BYTES = 32;

    private final SecretKeySpec secretKey;

    /** Doc khoa AES-256 tu CryptoProperties, fail-fast ngay luc khoi dong neu khoa sai do dai. */
    public AesEncryptionService(CryptoProperties properties) {
        byte[] keyBytes = Base64.getDecoder().decode(properties.key());
        if (keyBytes.length != AES_256_KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "APP_CRYPTO_KEY phai la chuoi Base64 giai ma ra dung 32 byte (AES-256), "
                            + "hien tai la " + keyBytes.length + " byte. Sinh khoa moi bang: "
                            + "openssl rand -base64 32");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /** Ma hoa chuoi ro thanh blob nhi phan (IV + ciphertext + tag) de luu vao cot VARBINARY _enc. */
    public byte[] encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(iv.length + cipherText.length)
                    .put(iv)
                    .put(cipherText)
                    .array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Ma hoa AES-256-GCM that bai", e);
        }
    }

    /**
     * Giai ma blob doc tu cot _enc ve lai chuoi ro. Nem IllegalStateException neu du lieu
     * bi sua doi (sai tag xac thuc) hoac khong dung dinh dang IV+ciphertext.
     */
    public String decrypt(byte[] encrypted) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(encrypted);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Giai ma AES-256-GCM that bai, du lieu co the da bi sua doi", e);
        }
    }
}
