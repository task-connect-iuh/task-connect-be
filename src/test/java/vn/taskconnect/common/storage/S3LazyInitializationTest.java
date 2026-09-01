package vn.taskconnect.common.storage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Xac nhan fix cua code review: neu thieu AWS_S3_ACCESS_KEY_ID/SECRET (rong, mo phong may
 * dev/CI chua setup AWS), context Spring van khoi dong duoc binh thuong (khong sap ca ung
 * dung chi vi mot bean khong lien quan), loi chi xay ra dung luc thuc su goi presign. Dung
 * AnnotationConfigApplicationContext thuan tuy (khong Spring Boot, khong can DB/Docker) vi
 * chi can kiem tra thoi diem AwsBasicCredentials.create() thuc thi.
 */
class S3LazyInitializationTest {

    @Configuration
    static class TestConfig {
        @Bean
        S3Properties s3Properties() {
            return new S3Properties("ap-southeast-1", "some-bucket", "", "");
        }
    }

    @Test
    void should_startContext_when_credentialsAreBlank_and_onlyFailOnActualPresignCall() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(TestConfig.class, S3Config.class, S3PresignedUploadService.class);

            assertThatCode(context::refresh).doesNotThrowAnyException();

            S3PresignedUploadService service = context.getBean(S3PresignedUploadService.class);
            assertThatThrownBy(() -> service.createPresignedPutUrl("avatars/x/y.jpg", "image/jpeg", Duration.ofMinutes(5)))
                    .hasRootCauseInstanceOf(NullPointerException.class)
                    .hasRootCauseMessage("Access key ID cannot be blank.");
        }
    }
}
