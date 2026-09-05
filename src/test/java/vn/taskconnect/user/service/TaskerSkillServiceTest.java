package vn.taskconnect.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
import vn.taskconnect.user.api.CertificationStatus;
import vn.taskconnect.user.api.SkillVerificationStatus;
import vn.taskconnect.user.dto.request.RejectCertificationRequest;
import vn.taskconnect.user.dto.request.SubmitSkillRequest;
import vn.taskconnect.user.dto.response.CertificationReviewResponse;
import vn.taskconnect.user.dto.response.CertificationReviewSummaryResponse;
import vn.taskconnect.user.dto.response.TaskerSkillResponse;
import vn.taskconnect.user.entity.CategoryCertificateRequirement;
import vn.taskconnect.user.entity.KycVerification;
import vn.taskconnect.user.entity.ServiceCategory;
import vn.taskconnect.user.entity.TaskerCertification;
import vn.taskconnect.user.entity.TaskerSkillProfile;
import vn.taskconnect.user.repository.CategoryCertificateRequirementRepository;
import vn.taskconnect.user.repository.KycVerificationRepository;
import vn.taskconnect.user.repository.ServiceCategoryRepository;
import vn.taskconnect.user.repository.TaskerCertificationRepository;
import vn.taskconnect.user.repository.TaskerSkillProfileRepository;
import vn.taskconnect.user.repository.UserProfileRepository;

/**
 * Unit test thuan tuy (khong DB, khong Spring context) cho TaskerSkillService. Dung
 * AesEncryptionService that (khong mock, re va can round-trip that cho luong Admin xem chi
 * tiet), cac repository con lai mock bang Mockito. Entity Master Data (ServiceCategory,
 * CategoryCertificateRequirement) chi co constructor rong nen dung reflection, cung mau da
 * dung o ServiceCategoryServiceTest/CertificateRequirementServiceTest.
 */
class TaskerSkillServiceTest {

    private static final String VALID_KEY = "gvuR3TBLZYe2nwVikB8pQpal7zsbVP9y1EXDSrVhWCk=";
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID CERTIFICATE_TYPE_ID = UUID.randomUUID();
    private static final Instant FIXED_NOW = Instant.parse("2026-08-27T10:00:00Z");

    private final TaskerSkillProfileRepository skillRepository = mock(TaskerSkillProfileRepository.class);
    private final TaskerCertificationRepository certificationRepository = mock(TaskerCertificationRepository.class);
    private final CategoryCertificateRequirementRepository requirementRepository =
            mock(CategoryCertificateRequirementRepository.class);
    private final ServiceCategoryRepository categoryRepository = mock(ServiceCategoryRepository.class);
    private final KycVerificationRepository kycRepository = mock(KycVerificationRepository.class);
    private final UserProfileRepository profileRepository = mock(UserProfileRepository.class);
    private final AesEncryptionService encryptionService = new AesEncryptionService(new CryptoProperties(VALID_KEY));
    private final S3PresignedUploadService s3Service = mock(S3PresignedUploadService.class);
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final TaskerSkillService service = new TaskerSkillService(skillRepository, certificationRepository,
            requirementRepository, categoryRepository, kycRepository, profileRepository, encryptionService,
            s3Service, clock);

    private static ServiceCategory activeCategoryOf(UUID id) {
        try {
            Constructor<ServiceCategory> constructor = ServiceCategory.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            ServiceCategory category = constructor.newInstance();
            setField(ServiceCategory.class, category, "id", id);
            setField(ServiceCategory.class, category, "active", true);
            return category;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CategoryCertificateRequirement requirementOf(UUID categoryId, UUID certificateTypeId) {
        try {
            Constructor<CategoryCertificateRequirement> constructor =
                    CategoryCertificateRequirement.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            CategoryCertificateRequirement requirement = constructor.newInstance();
            setField(CategoryCertificateRequirement.class, requirement, "id", UUID.randomUUID());
            setField(CategoryCertificateRequirement.class, requirement, "categoryId", categoryId);
            setField(CategoryCertificateRequirement.class, requirement, "certificateTypeId", certificateTypeId);
            setField(CategoryCertificateRequirement.class, requirement, "mandatory", true);
            return requirement;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setField(Class<?> type, Object target, String fieldName, Object value)
            throws ReflectiveOperationException {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static KycVerification verifiedKycOf(UUID accountId) {
        KycVerification kyc = new KycVerification(UUID.randomUUID(), accountId, "Nguyen Van A",
                new byte[0], new byte[0], new byte[0], new byte[0], FIXED_NOW.minusSeconds(3600));
        kyc.approve(UUID.randomUUID(), FIXED_NOW.minusSeconds(1800));
        return kyc;
    }

    private static SubmitSkillRequest requestFor(UUID accountId) {
        return new SubmitSkillRequest(CATEGORY_ID, 3, 100_000L, 300_000L, CERTIFICATE_TYPE_ID,
                "CC-001", "So Cong Thuong", null, null, "certificates/" + accountId + "/" + CATEGORY_ID + "/x.jpg",
                null, null);
    }

    private void givenValidCategoryKycAndRequirement() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(activeCategoryOf(CATEGORY_ID)));
        when(kycRepository.findFirstByAccountIdOrderBySubmittedAtDesc(ACCOUNT_ID))
                .thenReturn(Optional.of(verifiedKycOf(ACCOUNT_ID)));
        when(requirementRepository.findByCategoryId(CATEGORY_ID))
                .thenReturn(List.of(requirementOf(CATEGORY_ID, CERTIFICATE_TYPE_ID)));
    }

    @Test
    void should_createPendingSkillAndCertification_when_firstSubmissionForCategory() {
        givenValidCategoryKycAndRequirement();
        when(skillRepository.findByAccountIdAndCategoryId(ACCOUNT_ID, CATEGORY_ID)).thenReturn(Optional.empty());
        when(skillRepository.save(any(TaskerSkillProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(certificationRepository.save(any(TaskerCertification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskerSkillResponse response = service.submitSkill(ACCOUNT_ID, requestFor(ACCOUNT_ID));

        assertThat(response.verificationStatus()).isEqualTo(SkillVerificationStatus.PENDING);
        assertThat(response.latestCertificationStatus()).isEqualTo(CertificationStatus.PENDING_REVIEW);
        assertThat(response.yearsExperience()).isEqualTo(3);
    }

    @Test
    void should_throwCategoryNotFound_when_categoryInactive() {
        ServiceCategory inactive = activeCategoryOf(CATEGORY_ID);
        try {
            setField(ServiceCategory.class, inactive, "active", false);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.submitSkill(ACCOUNT_ID, requestFor(ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void should_throwKycNotVerified_when_kycNeverSubmitted() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(activeCategoryOf(CATEGORY_ID)));
        when(kycRepository.findFirstByAccountIdOrderBySubmittedAtDesc(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitSkill(ACCOUNT_ID, requestFor(ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.KYC_NOT_VERIFIED);
    }

    @Test
    void should_throwInvalidCertificateTypeForCategory_when_certificateTypeNotAccepted() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(activeCategoryOf(CATEGORY_ID)));
        when(kycRepository.findFirstByAccountIdOrderBySubmittedAtDesc(ACCOUNT_ID))
                .thenReturn(Optional.of(verifiedKycOf(ACCOUNT_ID)));
        when(requirementRepository.findByCategoryId(CATEGORY_ID))
                .thenReturn(List.of(requirementOf(CATEGORY_ID, UUID.randomUUID())));

        assertThatThrownBy(() -> service.submitSkill(ACCOUNT_ID, requestFor(ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.INVALID_CERTIFICATE_TYPE_FOR_CATEGORY);
    }

    @Test
    void should_throwValidationFailed_when_fileKeyDoesNotBelongToAccountAndCategory() {
        givenValidCategoryKycAndRequirement();
        when(skillRepository.findByAccountIdAndCategoryId(ACCOUNT_ID, CATEGORY_ID)).thenReturn(Optional.empty());
        SubmitSkillRequest request = new SubmitSkillRequest(CATEGORY_ID, 3, null, null, CERTIFICATE_TYPE_ID,
                null, null, null, null, "certificates/" + UUID.randomUUID() + "/" + CATEGORY_ID + "/x.jpg",
                null, null);

        assertThatThrownBy(() -> service.submitSkill(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void should_throwSkillAlreadyVerified_when_profileAlreadyVerifiedForCategory() {
        givenValidCategoryKycAndRequirement();
        TaskerSkillProfile verified = new TaskerSkillProfile(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID, 2, null,
                null, FIXED_NOW.minusSeconds(3600));
        verified.markVerified(FIXED_NOW.minusSeconds(1800));
        when(skillRepository.findByAccountIdAndCategoryId(ACCOUNT_ID, CATEGORY_ID))
                .thenReturn(Optional.of(verified));

        assertThatThrownBy(() -> service.submitSkill(ACCOUNT_ID, requestFor(ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.SKILL_ALREADY_VERIFIED);
    }

    @Test
    void should_throwSkillPendingReview_when_profileAlreadyPendingForCategory() {
        givenValidCategoryKycAndRequirement();
        TaskerSkillProfile pending = new TaskerSkillProfile(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID, 2, null,
                null, FIXED_NOW.minusSeconds(3600));
        when(skillRepository.findByAccountIdAndCategoryId(ACCOUNT_ID, CATEGORY_ID))
                .thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.submitSkill(ACCOUNT_ID, requestFor(ACCOUNT_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.SKILL_PENDING_REVIEW);
    }

    @Test
    void should_allowResubmissionAndResetToPending_when_profileWasRejected() {
        givenValidCategoryKycAndRequirement();
        TaskerSkillProfile rejected = new TaskerSkillProfile(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID, 1, null,
                null, FIXED_NOW.minusSeconds(3600));
        rejected.markRejected(FIXED_NOW.minusSeconds(1800));
        when(skillRepository.findByAccountIdAndCategoryId(ACCOUNT_ID, CATEGORY_ID))
                .thenReturn(Optional.of(rejected));
        when(skillRepository.save(any(TaskerSkillProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(certificationRepository.save(any(TaskerCertification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskerSkillResponse response = service.submitSkill(ACCOUNT_ID, requestFor(ACCOUNT_ID));

        assertThat(response.verificationStatus()).isEqualTo(SkillVerificationStatus.PENDING);
        assertThat(response.yearsExperience()).isEqualTo(3);
    }

    @Test
    void should_approveCertificationAndVerifySkillProfile_when_pendingReviewFound() {
        UUID adminId = UUID.randomUUID();
        TaskerCertification pending = new TaskerCertification(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID,
                CERTIFICATE_TYPE_ID, null, null, null, null, new byte[0], null, null,
                FIXED_NOW.minusSeconds(60));
        when(certificationRepository.findByIdForUpdate(pending.getId())).thenReturn(Optional.of(pending));
        TaskerSkillProfile profile = new TaskerSkillProfile(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID, 2, null,
                null, FIXED_NOW.minusSeconds(60));
        when(skillRepository.findByAccountIdAndCategoryId(ACCOUNT_ID, CATEGORY_ID))
                .thenReturn(Optional.of(profile));

        TaskerSkillResponse response = service.approve(pending.getId(), adminId);

        assertThat(response.verificationStatus()).isEqualTo(SkillVerificationStatus.VERIFIED);
        assertThat(pending.getStatus()).isEqualTo(CertificationStatus.APPROVED);
        assertThat(pending.getReviewedByAdminId()).isEqualTo(adminId);
    }

    @Test
    void should_throwCertificationNotPendingReview_when_approvingAlreadyReviewedCertification() {
        TaskerCertification approved = new TaskerCertification(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID,
                CERTIFICATE_TYPE_ID, null, null, null, null, new byte[0], null, null,
                FIXED_NOW.minusSeconds(60));
        approved.approve(UUID.randomUUID(), FIXED_NOW.minusSeconds(30));
        when(certificationRepository.findByIdForUpdate(approved.getId())).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.approve(approved.getId(), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.CERTIFICATION_NOT_PENDING_REVIEW);
    }

    @Test
    void should_rejectCertificationAndRejectSkillProfile_when_pendingReviewFound() {
        UUID adminId = UUID.randomUUID();
        TaskerCertification pending = new TaskerCertification(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID,
                CERTIFICATE_TYPE_ID, null, null, null, null, new byte[0], null, null,
                FIXED_NOW.minusSeconds(60));
        when(certificationRepository.findByIdForUpdate(pending.getId())).thenReturn(Optional.of(pending));
        TaskerSkillProfile profile = new TaskerSkillProfile(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID, 2, null,
                null, FIXED_NOW.minusSeconds(60));
        when(skillRepository.findByAccountIdAndCategoryId(ACCOUNT_ID, CATEGORY_ID))
                .thenReturn(Optional.of(profile));

        TaskerSkillResponse response =
                service.reject(pending.getId(), adminId, new RejectCertificationRequest("Anh mo, khong doc duoc"));

        assertThat(response.verificationStatus()).isEqualTo(SkillVerificationStatus.REJECTED);
        assertThat(pending.getRejectionReason()).isEqualTo("Anh mo, khong doc duoc");
    }

    @Test
    void should_cancelCertificationAndCancelSkillProfile_when_ownerCancelsPendingSubmission() {
        TaskerCertification pending = new TaskerCertification(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID,
                CERTIFICATE_TYPE_ID, null, null, null, null, new byte[0], null, null,
                FIXED_NOW.minusSeconds(60));
        when(certificationRepository.findByIdForUpdate(pending.getId())).thenReturn(Optional.of(pending));
        TaskerSkillProfile profile = new TaskerSkillProfile(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID, 2, null,
                null, FIXED_NOW.minusSeconds(60));
        when(skillRepository.findByAccountIdAndCategoryId(ACCOUNT_ID, CATEGORY_ID))
                .thenReturn(Optional.of(profile));

        TaskerSkillResponse response = service.cancel(ACCOUNT_ID, pending.getId());

        assertThat(response.verificationStatus()).isEqualTo(SkillVerificationStatus.CANCELLED);
        assertThat(pending.getStatus()).isEqualTo(CertificationStatus.CANCELLED);
    }

    @Test
    void should_throwCertificationNotFound_when_cancelingSomeoneElsesCertification() {
        TaskerCertification pending = new TaskerCertification(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID,
                CERTIFICATE_TYPE_ID, null, null, null, null, new byte[0], null, null,
                FIXED_NOW.minusSeconds(60));
        when(certificationRepository.findByIdForUpdate(pending.getId())).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.cancel(UUID.randomUUID(), pending.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.CERTIFICATION_NOT_FOUND);
    }

    @Test
    void should_throwCertificationNotPendingReview_when_cancelingAlreadyReviewedCertification() {
        TaskerCertification approved = new TaskerCertification(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID,
                CERTIFICATE_TYPE_ID, null, null, null, null, new byte[0], null, null,
                FIXED_NOW.minusSeconds(60));
        approved.approve(UUID.randomUUID(), FIXED_NOW.minusSeconds(30));
        when(certificationRepository.findByIdForUpdate(approved.getId())).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.cancel(ACCOUNT_ID, approved.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.CERTIFICATION_NOT_PENDING_REVIEW);
    }

    @Test
    void should_returnDecryptedDetailWithPresignedViewUrl_when_adminReviewsCertifications() {
        TaskerCertification certification = new TaskerCertification(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID,
                CERTIFICATE_TYPE_ID, encryptionService.encrypt("CC-12345"), "So Cong Thuong", null, null,
                encryptionService.encrypt("certificates/" + ACCOUNT_ID + "/" + CATEGORY_ID + "/file.pdf"),
                null, null, FIXED_NOW);
        when(certificationRepository.findByAccountIdAndCategoryIdOrderBySubmittedAtDesc(ACCOUNT_ID, CATEGORY_ID))
                .thenReturn(List.of(certification));
        when(s3Service.createPresignedGetUrl(
                eq("certificates/" + ACCOUNT_ID + "/" + CATEGORY_ID + "/file.pdf"), any(Duration.class)))
                .thenReturn("https://s3.example/cert-signed");

        List<CertificationReviewResponse> result = service.getCertificationsForReview(ACCOUNT_ID, CATEGORY_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).certificateNumber()).isEqualTo("CC-12345");
        assertThat(result.get(0).fileViewUrl()).isEqualTo("https://s3.example/cert-signed");
    }

    @Test
    void should_returnAllCategories_when_gettingMySkills() {
        TaskerSkillProfile profile = new TaskerSkillProfile(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID, 4, 50_000L,
                150_000L, FIXED_NOW);
        when(skillRepository.findByAccountIdOrderByCreatedAtAsc(ACCOUNT_ID)).thenReturn(List.of(profile));
        when(certificationRepository.findFirstByAccountIdAndCategoryIdOrderBySubmittedAtDesc(ACCOUNT_ID, CATEGORY_ID))
                .thenReturn(Optional.empty());

        List<TaskerSkillResponse> result = service.getMySkills(ACCOUNT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoryId()).isEqualTo(CATEGORY_ID);
        assertThat(result.get(0).latestCertificationStatus()).isNull();
    }

    @Test
    void should_returnSummaryPage_when_listingCertificationsForReview() {
        TaskerCertification pending = new TaskerCertification(UUID.randomUUID(), ACCOUNT_ID, CATEGORY_ID,
                CERTIFICATE_TYPE_ID, null, null, null, null, new byte[0], null, null, FIXED_NOW);
        PageRequest pageable = PageRequest.of(0, 20);
        when(certificationRepository.findByStatus(CertificationStatus.PENDING_REVIEW, pageable))
                .thenReturn(new PageImpl<>(List.of(pending), pageable, 1));

        Page<CertificationReviewSummaryResponse> result =
                service.listCertificationsForReview(CertificationStatus.PENDING_REVIEW, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(pending.getId());
        assertThat(result.getContent().get(0).accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.getContent().get(0).categoryId()).isEqualTo(CATEGORY_ID);
    }
}
