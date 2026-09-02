package vn.taskconnect.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.common.storage.S3PresignedUploadService;
import vn.taskconnect.common.storage.S3PresignedUploadService.PresignedUpload;
import vn.taskconnect.user.dto.request.CertificateUploadUrlRequest;
import vn.taskconnect.user.dto.response.CertificateUploadUrlResponse;

/**
 * Unit test thuan tuy (khong goi S3 that) cho CertificateUploadService. Khac
 * AvatarUploadServiceTest/KycUploadServiceTest - whitelist rong hon (cong them PDF) va key
 * co them phan category.
 */
class CertificateUploadServiceTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final Instant FIXED_NOW = Instant.parse("2026-08-27T10:00:00Z");

    private final S3PresignedUploadService s3Service = mock(S3PresignedUploadService.class);
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final CertificateUploadService service = new CertificateUploadService(s3Service, clock);

    @Test
    void should_throwUnsupportedFileType_when_contentTypeNotWhitelisted() {
        CertificateUploadUrlRequest request = new CertificateUploadUrlRequest("application/zip");

        assertThatThrownBy(() -> service.createUploadUrl(ACCOUNT_ID, CATEGORY_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_CERTIFICATE_FILE_TYPE);
        verify(s3Service, never()).createPresignedPutUrl(any(), any(), any());
    }

    @Test
    void should_acceptPdf_unlikeAvatarOrKycUpload() {
        when(s3Service.createPresignedPutUrl(any(), eq("application/pdf"), eq(Duration.ofMinutes(5))))
                .thenReturn(new PresignedUpload("https://upload.example/signed", "https://public.example/x.pdf"));

        CertificateUploadUrlResponse response =
                service.createUploadUrl(ACCOUNT_ID, CATEGORY_ID, new CertificateUploadUrlRequest("application/pdf"));

        assertThat(response.uploadUrl()).isEqualTo("https://upload.example/signed");
        assertThat(response.objectKey())
                .matches("certificates/" + ACCOUNT_ID + "/" + CATEGORY_ID + "/[0-9a-f-]{36}\\.pdf");
        assertThat(response.expiresAt()).isEqualTo(FIXED_NOW.plus(Duration.ofMinutes(5)));
    }

    @Test
    void should_acceptImage_sameWhitelistAsAvatarAndKyc() {
        when(s3Service.createPresignedPutUrl(any(), eq("image/jpeg"), any()))
                .thenReturn(new PresignedUpload("upload", "public"));

        CertificateUploadUrlResponse response =
                service.createUploadUrl(ACCOUNT_ID, CATEGORY_ID, new CertificateUploadUrlRequest("image/jpeg"));

        assertThat(response.objectKey())
                .matches("certificates/" + ACCOUNT_ID + "/" + CATEGORY_ID + "/[0-9a-f-]{36}\\.jpg");
    }
}
