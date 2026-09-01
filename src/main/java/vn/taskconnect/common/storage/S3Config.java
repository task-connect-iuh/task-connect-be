package vn.taskconnect.common.storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Tao bean {@link S3Presigner} dung chung, dung access key/secret rieng cua IAM user
 * backend (khong dung role EC2/ECS vi ha tang dev chay tren may thuong, khong o AWS).
 * Sinh presigned URL la tinh toan cuc bo (ky chu ky, khong goi mang), nen khong can bean
 * S3Client day du cho luong upload truc tiep tu client len S3.
 */
@Configuration
public class S3Config {

    /**
     * {@code @Lazy} bat buoc: neu khong, Spring se tu khoi tao bean nay trong luot
     * pre-instantiate singleton luc khoi dong context - du @Lazy da dat o tham so
     * constructor cua S3PresignedUploadService, container van tao rieng bean nay theo
     * dinh nghia cua no (khong lien quan gi den viec ai dang inject no). Phai co ca hai
     * ben (o day va o S3PresignedUploadService) thi bean nay moi thuc su chi duoc tao luc
     * goi presign lan dau, khong sap context ung dung khi thieu AWS_S3_ACCESS_KEY_ID/SECRET
     * (phat hien boi code review, xem docs/PROGRESS-USER-MODULE.md).
     */
    @Bean
    @Lazy
    public S3Presigner s3Presigner(S3Properties properties) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.accessKeyId(), properties.secretAccessKey());
        return S3Presigner.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
