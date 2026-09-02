package vn.taskconnect.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cau hinh AWS S3 dung chung cho moi module can luu tru file, doc tu {@code aws.s3.*}
 * trong application.yml. Hien tai chi module User dung (avatar, Buoc 1); Buoc 4/6 se
 * dung lai cho anh KYC va chung chi. Xem docs/adr/ADR-003-avatar-storage-s3-presigned-upload.md.
 */
@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(String region, String bucket, String accessKeyId, String secretAccessKey) {
}
