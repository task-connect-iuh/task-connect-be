package vn.taskconnect.user.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.common.crypto.AesEncryptionService;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.common.storage.S3PresignedUploadService;
import vn.taskconnect.user.api.CertificationStatus;
import vn.taskconnect.user.api.KycStatus;
import vn.taskconnect.user.api.SkillVerificationStatus;
import vn.taskconnect.user.dto.request.RejectCertificationRequest;
import vn.taskconnect.user.dto.request.SubmitSkillRequest;
import vn.taskconnect.user.dto.response.CertificationReviewResponse;
import vn.taskconnect.user.dto.response.CertificationReviewSummaryResponse;
import vn.taskconnect.user.dto.response.TaskerSkillResponse;
import vn.taskconnect.user.entity.CategoryCertificateRequirement;
import vn.taskconnect.user.entity.TaskerCertification;
import vn.taskconnect.user.entity.TaskerSkillProfile;
import vn.taskconnect.user.repository.CategoryCertificateRequirementRepository;
import vn.taskconnect.user.repository.KycVerificationRepository;
import vn.taskconnect.user.repository.ServiceCategoryRepository;
import vn.taskconnect.user.repository.TaskerCertificationRepository;
import vn.taskconnect.user.repository.TaskerSkillProfileRepository;

/**
 * Nghiep vu dang ky ky nang Tasker gop nop chung chi (Buoc 6) - mot giao dien, mot lan
 * submit cho moi category: kinh nghiem, gia, va chung chi cung luc. Chan cung neu KYC (Buoc
 * 4) chua VERIFIED. Quan he OR trong tung category (xem V5__seed_user_certificate_types.sql):
 * chi can MOT chung chi hop le duoc Admin duyet la ho so ky nang cua category do chuyen
 * VERIFIED - khong phai duyet het tat ca chung chi liet ke cho category.
 */
@Service
public class TaskerSkillService {

    /** URL xem file chung chi chi ngan han, sinh moi lan Admin mo chi tiet, khong luu lai. */
    private static final Duration VIEW_URL_TTL = Duration.ofMinutes(10);

    private final TaskerSkillProfileRepository skillRepository;
    private final TaskerCertificationRepository certificationRepository;
    private final CategoryCertificateRequirementRepository requirementRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final KycVerificationRepository kycRepository;
    private final AesEncryptionService encryptionService;
    private final S3PresignedUploadService s3Service;
    private final Clock clock;

    public TaskerSkillService(TaskerSkillProfileRepository skillRepository,
            TaskerCertificationRepository certificationRepository,
            CategoryCertificateRequirementRepository requirementRepository,
            ServiceCategoryRepository categoryRepository, KycVerificationRepository kycRepository,
            AesEncryptionService encryptionService, S3PresignedUploadService s3Service, Clock clock) {
        this.skillRepository = skillRepository;
        this.certificationRepository = certificationRepository;
        this.requirementRepository = requirementRepository;
        this.categoryRepository = categoryRepository;
        this.kycRepository = kycRepository;
        this.encryptionService = encryptionService;
        this.s3Service = s3Service;
        this.clock = clock;
    }

    /**
     * Dang ky (hoac nop lai sau khi bi tu choi) ky nang cho mot category, kem nop chung chi
     * cung luc. Ho so ky nang la UPDATE-in-place khi nop lai (UNIQUE account_id+category_id
     * trong V2), con chung chi luon la mot dong moi (giu lich su cac lan bi tu choi).
     */
    @Transactional
    public TaskerSkillResponse submitSkill(UUID accountId, SubmitSkillRequest request) {
        requireCategoryExists(request.categoryId());
        requireKycVerified(accountId);
        requireValidCertificateType(request.categoryId(), request.certificateTypeId());
        requireDateOrder(request.issuedDate(), request.expiryDate());
        requireOwnCertificatePrefix(accountId, request.categoryId(), request.fileKey());

        Instant now = clock.instant();
        TaskerSkillProfile profile = skillRepository.findByAccountIdAndCategoryId(accountId, request.categoryId())
                .orElse(null);
        if (profile == null) {
            profile = new TaskerSkillProfile(UUID.randomUUID(), accountId, request.categoryId(),
                    request.yearsExperience(), request.priceMin(), request.priceMax(), now);
        } else if (profile.getVerificationStatus() == SkillVerificationStatus.VERIFIED) {
            throw new BusinessException(ErrorCode.SKILL_ALREADY_VERIFIED);
        } else if (profile.getVerificationStatus() == SkillVerificationStatus.PENDING) {
            throw new BusinessException(ErrorCode.SKILL_PENDING_REVIEW);
        } else {
            profile.resubmit(request.yearsExperience(), request.priceMin(), request.priceMax(), now);
        }
        profile = skillRepository.save(profile);

        TaskerCertification certification = new TaskerCertification(
                UUID.randomUUID(), accountId, request.categoryId(), request.certificateTypeId(),
                request.certificateNumber() != null ? encryptionService.encrypt(request.certificateNumber()) : null,
                request.issuingAuthority(), request.issuedDate(), request.expiryDate(),
                encryptionService.encrypt(request.fileKey()), request.experienceProofUrl(),
                request.claimedExperienceYears(), now);
        certification = certificationRepository.save(certification);

        return toResponse(profile, certification);
    }

    /** Danh sach moi category chinh chu tai khoan da khai bao, kem trang thai chung chi gan nhat. */
    @Transactional(readOnly = true)
    public List<TaskerSkillResponse> getMySkills(UUID accountId) {
        return skillRepository.findByAccountIdOrderByCreatedAtAsc(accountId).stream()
                .map(profile -> toResponse(profile, latestCertificationOrNull(accountId, profile.getCategoryId())))
                .toList();
    }

    /**
     * Toan bo lich su nop chung chi cua mot cap Tasker+category, kem giai ma so hieu chung
     * chi va presigned GET URL ngan han de xem file. Dung chung cho hai noi goi: Admin xet
     * duyet (accountId lay tu path, method public rieng ben Controller) va chinh chu Tasker
     * tu xem lai qua nut "Xem chi tiet" (accountId lay tu principal dang dang nhap) - vi
     * cung mot format DTO va cung logic giai ma/ky URL, khong tach rieng de tranh trung lap.
     */
    @Transactional(readOnly = true)
    public List<CertificationReviewResponse> getCertificationsForReview(UUID accountId, UUID categoryId) {
        return certificationRepository.findByAccountIdAndCategoryIdOrderBySubmittedAtDesc(accountId, categoryId)
                .stream()
                .map(this::toReviewResponse)
                .toList();
    }

    /**
     * Chi danh cho Admin: hang doi cac lan nop chung chi theo status (mac dinh
     * PENDING_REVIEW), xuyen suot moi tai khoan/category, moi nhat truoc. Tra ve DTO nhe
     * (khong giai ma so hieu chung chi, khong ky presigned URL) - xem chi tiet that su goi
     * getCertificationsForReview(accountId, categoryId) rieng.
     */
    @Transactional(readOnly = true)
    public Page<CertificationReviewSummaryResponse> listCertificationsForReview(CertificationStatus status,
            Pageable pageable) {
        return certificationRepository.findByStatus(status, pageable).map(CertificationReviewSummaryResponse::from);
    }

    /** Admin duyet mot lan nop chung chi - chuyen ca chung chi lan ho so ky nang cua category do sang VERIFIED. */
    @Transactional
    public TaskerSkillResponse approve(UUID certificationId, UUID adminAccountId) {
        TaskerCertification certification = requirePendingReview(certificationId);
        Instant now = clock.instant();
        certification.approve(adminAccountId, now);
        TaskerSkillProfile profile = requireSkillProfile(certification.getAccountId(), certification.getCategoryId());
        profile.markVerified(now);
        return toResponse(profile, certification);
    }

    /** Admin tu choi mot lan nop chung chi kem ly do - ho so ky nang cua category do quay lai REJECTED. */
    @Transactional
    public TaskerSkillResponse reject(UUID certificationId, UUID adminAccountId, RejectCertificationRequest request) {
        TaskerCertification certification = requirePendingReview(certificationId);
        Instant now = clock.instant();
        certification.reject(adminAccountId, request.rejectionReason(), now);
        TaskerSkillProfile profile = requireSkillProfile(certification.getAccountId(), certification.getCategoryId());
        profile.markRejected(now);
        return toResponse(profile, certification);
    }

    private TaskerCertification requirePendingReview(UUID certificationId) {
        TaskerCertification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CERTIFICATION_NOT_FOUND));
        if (certification.getStatus() != CertificationStatus.PENDING_REVIEW) {
            throw new BusinessException(ErrorCode.CERTIFICATION_NOT_PENDING_REVIEW);
        }
        return certification;
    }

    private TaskerSkillProfile requireSkillProfile(UUID accountId, UUID categoryId) {
        return skillRepository.findByAccountIdAndCategoryId(accountId, categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKILL_NOT_FOUND));
    }

    private TaskerCertification latestCertificationOrNull(UUID accountId, UUID categoryId) {
        return certificationRepository.findFirstByAccountIdAndCategoryIdOrderBySubmittedAtDesc(accountId, categoryId)
                .orElse(null);
    }

    private TaskerSkillResponse toResponse(TaskerSkillProfile profile, TaskerCertification latestCertification) {
        CertificationStatus latestStatus = latestCertification != null ? latestCertification.getStatus() : null;
        String latestRejectionReason = latestCertification != null ? latestCertification.getRejectionReason() : null;
        return new TaskerSkillResponse(profile.getCategoryId(), profile.getYearsExperience(), profile.getPriceMin(),
                profile.getPriceMax(), profile.getVerificationStatus(), profile.getVerifiedAt(), latestStatus,
                latestRejectionReason);
    }

    private CertificationReviewResponse toReviewResponse(TaskerCertification certification) {
        String certificateNumber = certification.getCertificateNumberEnc() != null
                ? encryptionService.decrypt(certification.getCertificateNumberEnc())
                : null;
        String fileKey = encryptionService.decrypt(certification.getFileUrlEnc());
        return new CertificationReviewResponse(
                certification.getId(),
                certification.getCertificateTypeId(),
                certificateNumber,
                certification.getIssuingAuthority(),
                certification.getIssuedDate(),
                certification.getExpiryDate(),
                s3Service.createPresignedGetUrl(fileKey, VIEW_URL_TTL),
                certification.getExperienceProofUrl(),
                certification.getClaimedExperienceYears(),
                certification.getStatus(),
                certification.getRejectionReason(),
                certification.getSubmittedAt(),
                certification.getReviewedAt());
    }

    /** Category phai ton tai va con hien hanh - khong cho khai bao ky nang cho danh muc bi vo hieu hoa. */
    private void requireCategoryExists(UUID categoryId) {
        if (categoryRepository.findById(categoryId).filter(category -> category.isActive()).isEmpty()) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    /** Chan cung: phai co lan nop KYC gan nhat va dang VERIFIED - xem quyet dinh o docs/PROGRESS-USER-MODULE.md. */
    private void requireKycVerified(UUID accountId) {
        boolean verified = kycRepository.findFirstByAccountIdOrderBySubmittedAtDesc(accountId)
                .map(kyc -> kyc.getStatus() == KycStatus.VERIFIED)
                .orElse(false);
        if (!verified) {
            throw new BusinessException(ErrorCode.KYC_NOT_VERIFIED);
        }
    }

    /** certificateTypeId phai nam trong danh sach chung chi duoc chap nhan cho category nay (Buoc 5). */
    private void requireValidCertificateType(UUID categoryId, UUID certificateTypeId) {
        List<CategoryCertificateRequirement> requirements = requirementRepository.findByCategoryId(categoryId);
        boolean valid = requirements.stream()
                .anyMatch(requirement -> requirement.getCertificateTypeId().equals(certificateTypeId));
        if (!valid) {
            throw new BusinessException(ErrorCode.INVALID_CERTIFICATE_TYPE_FOR_CATEGORY);
        }
    }

    /** Ngay het han (neu co) khong duoc truoc ngay cap (neu co). */
    private void requireDateOrder(LocalDate issuedDate, LocalDate expiryDate) {
        if (issuedDate != null && expiryDate != null && expiryDate.isBefore(issuedDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Ngày hết hạn không được trước ngày cấp.");
        }
    }

    /**
     * Chan client gui object key khong thuoc prefix "certificates/{accountId}/{categoryId}/"
     * cua chinh minh - cung ly do nhu requireOwnKycPrefix o KycVerificationService (Buoc 4).
     */
    private void requireOwnCertificatePrefix(UUID accountId, UUID categoryId, String fileKey) {
        String expectedPrefix = "certificates/" + accountId + "/" + categoryId + "/";
        if (!fileKey.startsWith(expectedPrefix)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Object key file chứng chỉ không hợp lệ.");
        }
    }
}
