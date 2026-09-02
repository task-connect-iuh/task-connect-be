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
import vn.taskconnect.user.dto.request.KycImageSide;
import vn.taskconnect.user.dto.request.KycUploadUrlRequest;
import vn.taskconnect.user.dto.response.KycUploadUrlResponse;

/**
 * Unit test thuan tuy (khong goi S3 that) cho KycUploadService, mock S3PresignedUploadService.
 * Kiem tra whitelist dinh dang anh (dung chung voi avatar qua ImageContentTypes) va cach
 * sinh object key rieng cho tung mat CCCD.
 */
class KycUploadServiceTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final Instant FIXED_NOW = Instant.parse("2026-08-27T10:00:00Z");

    private final S3PresignedUploadService s3Service = mock(S3PresignedUploadService.class);
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final KycUploadService service = new KycUploadService(s3Service, clock);

    @Test
    void should_throwUnsupportedKycImageType_when_contentTypeNotWhitelisted() {
        KycUploadUrlRequest request = new KycUploadUrlRequest(KycImageSide.FRONT, "application/pdf");

        assertThatThrownBy(() -> service.createUploadUrl(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_KYC_IMAGE_TYPE);
        verify(s3Service, never()).createPresignedPutUrl(any(), any(), any());
    }

    @Test
    void should_generateKeyUnderAccountKycPrefix_withSideAndMatchingExtension() {
        when(s3Service.createPresignedPutUrl(any(), eq("image/jpeg"), eq(Duration.ofMinutes(5))))
                .thenReturn(new PresignedUpload("https://upload.example/signed", "https://public.example/x.jpg"));

        KycUploadUrlResponse response =
                service.createUploadUrl(ACCOUNT_ID, new KycUploadUrlRequest(KycImageSide.FRONT, "image/jpeg"));

        assertThat(response.uploadUrl()).isEqualTo("https://upload.example/signed");
        assertThat(response.objectKey()).matches("kyc/" + ACCOUNT_ID + "/[0-9a-f-]{36}-front\\.jpg");
        assertThat(response.expiresAt()).isEqualTo(FIXED_NOW.plus(Duration.ofMinutes(5)));
    }

    @Test
    void should_useBackInKey_when_sideIsBack() {
        when(s3Service.createPresignedPutUrl(any(), eq("image/png"), any()))
                .thenReturn(new PresignedUpload("upload", "public"));

        KycUploadUrlResponse response =
                service.createUploadUrl(ACCOUNT_ID, new KycUploadUrlRequest(KycImageSide.BACK, "image/png"));

        assertThat(response.objectKey()).matches("kyc/" + ACCOUNT_ID + "/[0-9a-f-]{36}-back\\.png");
    }
}
