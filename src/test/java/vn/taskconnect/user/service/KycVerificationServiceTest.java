package vn.taskconnect.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.taskconnect.common.crypto.AesEncryptionService;
import vn.taskconnect.common.crypto.CryptoProperties;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.common.storage.S3PresignedUploadService;
import vn.taskconnect.user.api.KycStatus;
import vn.taskconnect.user.dto.request.RejectKycRequest;
import vn.taskconnect.user.dto.request.SubmitKycRequest;
import vn.taskconnect.user.dto.response.KycReviewDetailResponse;
import vn.taskconnect.user.dto.response.KycReviewSummaryResponse;
import vn.taskconnect.user.entity.KycVerification;
import vn.taskconnect.user.entity.UserProfile;
import vn.taskconnect.user.repository.KycIdNumberLockRepository;
import vn.taskconnect.user.repository.KycVerificationRepository;
import vn.taskconnect.user.repository.UserProfileRepository;

/**
 * Unit test thuan tuy (khong DB, khong Spring context) cho KycVerificationService. Dung
 * AesEncryptionService that (khong mock) vi la thanh phan re, thuan tuy va can round-trip
 * that de kiem tra giai ma dung o luong Admin xem chi tiet.
 */
class KycVerificationServiceTest {

    private static final String VALID_KEY = "gvuR3TBLZYe2nwVikB8pQpal7zsbVP9y1EXDSrVhWCk=";
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final Instant FIXED_NOW = Instant.parse("2026-08-27T10:00:00Z");

    private final KycVerificationRepository kycRepository = mock(KycVerificationRepository.class);
    private final KycIdNumberLockRepository idNumberLockRepository = mock(KycIdNumberLockRepository.class);
    private final UserProfileRepository profileRepository = mock(UserProfileRepository.class);
    private final AesEncryptionService encryptionService = new AesEncryptionService(new CryptoProperties(VALID_KEY));
    private final S3PresignedUploadService s3Service = mock(S3PresignedUploadService.class);
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final KycVerificationService service = new KycVerificationService(
            kycRepository, idNumberLockRepository, profileRepository, encryptionService, s3Service, clock);

    private static SubmitKycRequest requestFor(UUID accountId) {
        return new SubmitKycRequest("Nguyen Van A", "079203001234",
                "kyc/" + accountId + "/front.jpg", "kyc/" + accountId + "/back.jpg");
    }

    @Test
    void should_createNewVerifyingRow_when_neverSubmittedBefore() {
        when(kycRepository.findFirstByAccountIdOrderBySubmittedAtDesc(ACCOUNT_ID)).thenReturn(Optional.empty());
        when(kycRepository.save(any(KycVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        KycVerification result = service.submitKyc(ACCOUNT_ID, requestFor(ACCOUNT_ID));

        assertThat(result.getStatus()).isEqualTo(KycStatus.VERIFYING);
        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(encryptionService.decrypt(result.getIdNumberEnc())).isEqualTo("079203001234");
        verify(profileRepository, never()).save(any());
    }

    @Test
    void should_syncProfileKycStatus_when_profileExists() {
        when(kycRepository.findFirstByAccountIdOrderBySubmittedAtDesc(ACCOUNT_ID)).thenReturn(Optional.empty());
        when(kycRepository.save(any(KycVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserProfile profile = new UserProfile(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A", "Quan 7",
                FIXED_NOW.minusSeconds(3600));
        when(profileRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.submitKyc(ACCOUNT_ID, requestFor(ACCOUNT_ID));

        assertThat(profile.getKycStatus()).isEqualTo(KycStatus.VERIFYING);
        verify(profileRepository, times(1)).save(profile);
    }

    @Test
    void should_throwKycAlreadyVerifying_when_latestSubmissionStillPending() {
        KycVerification pending = new KycVerification(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A",
                new byte[0], new byte[0], new byte[0], new byte[0], FIXED_NOW.minusSeconds(60));
        when(kycRepository.findFirstByAccountIdOrderBySubmittedAtDesc(ACCOUNT_ID))
                .thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.submitKyc(ACCOUNT_ID, requestFor(ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.KYC_ALREADY_VERIFYING);
        verify(kycRepository, never()).save(any());
    }

    @Test
    void should_throwKycAlreadyVerified_when_latestSubmissionAlreadyVerified() {
        KycVerification verified = new KycVerification(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A",
                new byte[0], new byte[0], new byte[0], new byte[0], FIXED_NOW.minusSeconds(60));
        verified.approve(UUID.randomUUID(), FIXED_NOW.minusSeconds(30));
        when(kycRepository.findFirstByAccountIdOrderBySubmittedAtDesc(ACCOUNT_ID))
                .thenReturn(Optional.of(verified));

        assertThatThrownBy(() -> service.submitKyc(ACCOUNT_ID, requestFor(ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.KYC_ALREADY_VERIFIED);
    }

    @Test
    void should_allowResubmission_when_latestSubmissionWasRejected() {
        KycVerification rejected = new KycVerification(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A",
                new byte[0], new byte[0], new byte[0], new byte[0], FIXED_NOW.minusSeconds(60));
        rejected.reject(UUID.randomUUID(), "Anh mo, khong doc duoc", FIXED_NOW.minusSeconds(30));
        when(kycRepository.findFirstByAccountIdOrderBySubmittedAtDesc(ACCOUNT_ID))
                .thenReturn(Optional.of(rejected));
        when(kycRepository.save(any(KycVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        KycVerification result = service.submitKyc(ACCOUNT_ID, requestFor(ACCOUNT_ID));

        assertThat(result.getStatus()).isEqualTo(KycStatus.VERIFYING);
    }

    @Test
    void should_throwValidationFailed_when_imageKeyDoesNotBelongToAccount() {
        when(kycRepository.findFirstByAccountIdOrderBySubmittedAtDesc(ACCOUNT_ID)).thenReturn(Optional.empty());
        SubmitKycRequest request = new SubmitKycRequest("Nguyen Van A", "079203001234",
                "kyc/" + UUID.randomUUID() + "/front.jpg", "kyc/" + ACCOUNT_ID + "/back.jpg");

        assertThatThrownBy(() -> service.submitKyc(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
        verify(kycRepository, never()).save(any());
    }

    @Test
    void should_throwKycNotFound_when_gettingLatestBeforeAnySubmission() {
        when(kycRepository.findFirstByAccountIdOrderBySubmittedAtDesc(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyLatestKyc(ACCOUNT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.KYC_NOT_FOUND);
    }

    @Test
    void should_approveAndSyncProfile_when_pendingReviewFound() {
        UUID adminId = UUID.randomUUID();
        KycVerification pending = new KycVerification(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A",
                new byte[0], new byte[0], new byte[0], new byte[0], FIXED_NOW.minusSeconds(60));
        when(kycRepository.findByIdForUpdate(pending.getId())).thenReturn(Optional.of(pending));
        UserProfile profile = new UserProfile(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A", "Quan 7", FIXED_NOW);
        when(profileRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KycVerification result = service.approve(pending.getId(), adminId);

        assertThat(result.getStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(result.getReviewedByAdminId()).isEqualTo(adminId);
        assertThat(profile.getKycStatus()).isEqualTo(KycStatus.VERIFIED);
    }

    @Test
    void should_throwKycNotPendingReview_when_approvingAlreadyReviewedSubmission() {
        KycVerification alreadyVerified = new KycVerification(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A",
                new byte[0], new byte[0], new byte[0], new byte[0], FIXED_NOW.minusSeconds(60));
        alreadyVerified.approve(UUID.randomUUID(), FIXED_NOW.minusSeconds(30));
        when(kycRepository.findByIdForUpdate(alreadyVerified.getId())).thenReturn(Optional.of(alreadyVerified));

        assertThatThrownBy(() -> service.approve(alreadyVerified.getId(), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.KYC_NOT_PENDING_REVIEW);
    }

    @Test
    void should_reject_when_pendingReviewFound() {
        UUID adminId = UUID.randomUUID();
        KycVerification pending = new KycVerification(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A",
                new byte[0], new byte[0], new byte[0], new byte[0], FIXED_NOW.minusSeconds(60));
        when(kycRepository.findByIdForUpdate(pending.getId())).thenReturn(Optional.of(pending));
        when(profileRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        KycVerification result = service.reject(pending.getId(), adminId, new RejectKycRequest("Anh mo"));

        assertThat(result.getStatus()).isEqualTo(KycStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Anh mo");
    }

    @Test
    void should_cancelAndSyncProfile_when_ownerCancelsPendingSubmission() {
        KycVerification pending = new KycVerification(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A",
                new byte[0], new byte[0], new byte[0], new byte[0], FIXED_NOW.minusSeconds(60));
        when(kycRepository.findByIdForUpdate(pending.getId())).thenReturn(Optional.of(pending));
        UserProfile profile = new UserProfile(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A", "Quan 7", FIXED_NOW);
        when(profileRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KycVerification result = service.cancel(ACCOUNT_ID, pending.getId());

        assertThat(result.getStatus()).isEqualTo(KycStatus.CANCELLED);
        assertThat(profile.getKycStatus()).isEqualTo(KycStatus.CANCELLED);
    }

    @Test
    void should_throwKycNotFound_when_cancelingSomeoneElsesSubmission() {
        KycVerification pending = new KycVerification(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A",
                new byte[0], new byte[0], new byte[0], new byte[0], FIXED_NOW.minusSeconds(60));
        when(kycRepository.findByIdForUpdate(pending.getId())).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.cancel(UUID.randomUUID(), pending.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.KYC_NOT_FOUND);
    }

    @Test
    void should_throwKycNotPendingReview_when_cancelingAlreadyReviewedSubmission() {
        KycVerification approved = new KycVerification(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A",
                new byte[0], new byte[0], new byte[0], new byte[0], FIXED_NOW.minusSeconds(60));
        approved.approve(UUID.randomUUID(), FIXED_NOW.minusSeconds(30));
        when(kycRepository.findByIdForUpdate(approved.getId())).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.cancel(ACCOUNT_ID, approved.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.KYC_NOT_PENDING_REVIEW);
    }

    @Test
    void should_returnDecryptedDetailWithPresignedViewUrls_when_adminReviewsLatestSubmission() {
        KycVerification verification = new KycVerification(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A",
                encryptionService.encrypt("079203001234"),
                new byte[32],
                encryptionService.encrypt("kyc/" + ACCOUNT_ID + "/front.jpg"),
                encryptionService.encrypt("kyc/" + ACCOUNT_ID + "/back.jpg"),
                FIXED_NOW);
        when(kycRepository.findFirstByAccountIdOrderBySubmittedAtDesc(ACCOUNT_ID))
                .thenReturn(Optional.of(verification));
        when(s3Service.createPresignedGetUrl(eq("kyc/" + ACCOUNT_ID + "/front.jpg"), any(Duration.class)))
                .thenReturn("https://s3.example/front-signed");
        when(s3Service.createPresignedGetUrl(eq("kyc/" + ACCOUNT_ID + "/back.jpg"), any(Duration.class)))
                .thenReturn("https://s3.example/back-signed");

        KycReviewDetailResponse response = service.getLatestKycForReview(ACCOUNT_ID);

        assertThat(response.idNumber()).isEqualTo("079203001234");
        assertThat(response.idCardFrontViewUrl()).isEqualTo("https://s3.example/front-signed");
        assertThat(response.idCardBackViewUrl()).isEqualTo("https://s3.example/back-signed");
    }

    @Test
    void should_returnSummaryPage_when_listingForReview() {
        KycVerification pending = new KycVerification(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A",
                encryptionService.encrypt("079203001234"), new byte[32], encryptionService.encrypt("front-key"),
                encryptionService.encrypt("back-key"), FIXED_NOW);
        PageRequest pageable = PageRequest.of(0, 20);
        when(kycRepository.findByStatus(KycStatus.VERIFYING, pageable))
                .thenReturn(new PageImpl<>(List.of(pending), pageable, 1));

        Page<KycReviewSummaryResponse> result = service.listForReview(KycStatus.VERIFYING, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(pending.getId());
        assertThat(result.getContent().get(0).fullNameOnId()).isEqualTo("Nguyen Van A");
        // Khong duoc lo so CCCD tho/da giai ma trong DTO tom tat - chi KycReviewDetailResponse moi co truong nay.
    }
}
