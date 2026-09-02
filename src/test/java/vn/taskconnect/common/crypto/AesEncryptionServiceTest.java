package vn.taskconnect.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * Unit test thuan tuy (khong can Spring context/DB) cho AesEncryptionService - kiem tra
 * round-trip ma hoa/giai ma, tinh ngau nhien cua IV, phat hien du lieu bi sua doi, va
 * fail-fast khi khoa cau hinh sai do dai.
 */
class AesEncryptionServiceTest {

    // Base64 cua dung 32 byte ngau nhien - chi dung cho test, khong lien quan khoa dev
    // trong .env.example.
    private static final String VALID_KEY = "gvuR3TBLZYe2nwVikB8pQpal7zsbVP9y1EXDSrVhWCk=";

    private final AesEncryptionService service = new AesEncryptionService(new CryptoProperties(VALID_KEY));

    @Test
    void should_decryptToOriginalPlaintext_when_roundTrip() {
        byte[] encrypted = service.encrypt("012345678901");

        assertThat(service.decrypt(encrypted)).isEqualTo("012345678901");
    }

    @Test
    void should_produceDifferentCiphertext_when_encryptingSamePlaintextTwice() {
        String plaintext = "so cccd giong nhau";

        byte[] first = service.encrypt(plaintext);
        byte[] second = service.encrypt(plaintext);

        assertThat(first).isNotEqualTo(second);
        assertThat(service.decrypt(first)).isEqualTo(plaintext);
        assertThat(service.decrypt(second)).isEqualTo(plaintext);
    }

    @Test
    void should_throw_when_ciphertextTamperedWith() {
        byte[] encrypted = service.encrypt("du lieu can bao ve");
        encrypted[encrypted.length - 1] ^= 0x01;

        assertThatThrownBy(() -> service.decrypt(encrypted)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_throw_when_configuredKeyIsNot32BytesAfterDecoding() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new AesEncryptionService(new CryptoProperties(shortKey)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 byte");
    }
}
