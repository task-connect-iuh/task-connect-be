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
import org.mockito.ArgumentCaptor;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.common.storage.S3PresignedUploadService;
import vn.taskconnect.common.storage.S3PresignedUploadService.PresignedUpload;
import vn.taskconnect.user.dto.request.AvatarUploadUrlRequest;
import vn.taskconnect.user.dto.response.AvatarUploadUrlResponse;

/**
 * Unit test thuan tuy (khong goi S3 that) cho AvatarUploadService, mock
 * S3PresignedUploadService. Kiem tra whitelist dinh dang anh va cach sinh object key -
 * xem ADR-003. Khong bao phu duoc phan "S3 that co tra ve URL dung khong", can smoke test
 * thu cong voi bucket that (xem docs/PROGRESS-USER-MODULE.md).
 */
class AvatarUploadServiceTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final Instant FIXED_NOW = Instant.parse("2026-08-27T10:00:00Z");

    private final S3PresignedUploadService s3Service = mock(S3PresignedUploadService.class);
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final AvatarUploadService service = new AvatarUploadService(s3Service, clock);

    @Test
    void should_throwUnsupportedAvatarType_when_contentTypeNotWhitelisted() {
        AvatarUploadUrlRequest request = new AvatarUploadUrlRequest("application/pdf");

        assertThatThrownBy(() -> service.createUploadUrl(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_AVATAR_TYPE);
        verify(s3Service, never()).createPresignedPutUrl(any(), any(), any());
    }

    @Test
    void should_createUploadUrl_when_contentTypeIsJpeg() {
        when(s3Service.createPresignedPutUrl(any(), eq("image/jpeg"), eq(Duration.ofMinutes(5))))
                .thenReturn(new PresignedUpload("https://upload.example/signed", "https://public.example/avatar.jpg"));

        AvatarUploadUrlResponse response = service.createUploadUrl(ACCOUNT_ID, new AvatarUploadUrlRequest("image/jpeg"));

        assertThat(response.uploadUrl()).isEqualTo("https://upload.example/signed");
        assertThat(response.publicUrl()).isEqualTo("https://public.example/avatar.jpg");
        assertThat(response.expiresAt()).isEqualTo(FIXED_NOW.plus(Duration.ofMinutes(5)));
    }

    // Phat hien boi code review: contentType khac hoa/thuong hoac kem tham so (vd charset)
    // van phai duoc chap nhan, khong bi tu choi nham la UNSUPPORTED_AVATAR_TYPE.
    @Test
    void should_normalizeContentType_when_caseDiffersOrHasParameters() {
        when(s3Service.createPresignedPutUrl(any(), eq("image/jpeg"), any()))
                .thenReturn(new PresignedUpload("upload", "public"));

        AvatarUploadUrlResponse response =
                service.createUploadUrl(ACCOUNT_ID, new AvatarUploadUrlRequest("IMAGE/JPEG; charset=UTF-8"));

        assertThat(response.uploadUrl()).isEqualTo("upload");
        verify(s3Service).createPresignedPutUrl(any(), eq("image/jpeg"), any());
    }

    @Test
    void should_generateKeyUnderAccountAvatarsPrefix_withMatchingExtension() {
        when(s3Service.createPresignedPutUrl(any(), any(), any()))
                .thenReturn(new PresignedUpload("upload", "public"));
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        service.createUploadUrl(ACCOUNT_ID, new AvatarUploadUrlRequest("image/png"));

        verify(s3Service).createPresignedPutUrl(keyCaptor.capture(), eq("image/png"), any());
        assertThat(keyCaptor.getValue()).matches("avatars/" + ACCOUNT_ID + "/[0-9a-f-]{36}\\.png");
    }
}
