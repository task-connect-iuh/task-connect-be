package vn.taskconnect.user.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.common.crypto.AesEncryptionService;
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
import vn.taskconnect.user.repository.KycVerificationRepository;
import vn.taskconnect.user.repository.UserProfileRepository;

/**
 * Nghiep vu xac minh danh tinh (KYC) - Buoc 4: nop ho so, xem trang thai cua chinh minh, va
 * Admin duyet/tu choi/liet ke hang doi tung lan nop. Chi Tasker can KYC (UC05 "Xac minh
 * danh tinh Tasker") - Task Poster khong can, gate o KycVerificationController.
 */
@Service
public class KycVerificationService {

    /** URL xem anh CCCD chi ngan han, sinh moi lan Admin mo chi tiet, khong luu lai. */
    private static final Duration VIEW_URL_TTL = Duration.ofMinutes(10);

    private final KycVerificationRepository kycRepository;
    private final UserProfileRepository profileRepository;
    private final AesEncryptionService encryptionService;
    private final S3PresignedUploadService s3Service;
    private final Clock clock;

    public KycVerificationService(KycVerificationRepository kycRepository, UserProfileRepository profileRepository,
            AesEncryptionService encryptionService, S3PresignedUploadService s3Service, Clock clock) {
        this.kycRepository = kycRepository;
        this.profileRepository = profileRepository;
        this.encryptionService = encryptionService;
        this.s3Service = s3Service;
        this.clock = clock;
    }

    /**
     * Nop ho so KYC moi. Chan neu lan nop gan nhat con VERIFYING (dang cho duyet, khong cho
     * nop chong len) hoac da VERIFIED (khong can nop lai); cho phep nop lai khi lan gan nhat
     * la REJECTED hoac chua tung nop lan nao - khong co UNIQUE tren account_id trong V2
     * migration, moi lan nop la mot dong rieng, dung nguyen mau audit trail.
     */
    @Transactional
    public KycVerification submitKyc(UUID accountId, SubmitKycRequest request) {
        kycRepository.findFirstByAccountIdOrderBySubmittedAtDesc(accountId).ifPresent(latest -> {
            if (latest.getStatus() == KycStatus.VERIFYING) {
                throw new BusinessException(ErrorCode.KYC_ALREADY_VERIFYING);
            }
            if (latest.getStatus() == KycStatus.VERIFIED) {
                throw new BusinessException(ErrorCode.KYC_ALREADY_VERIFIED);
            }
        });
        requireOwnKycPrefix(accountId, request.idCardFrontKey());
        requireOwnKycPrefix(accountId, request.idCardBackKey());

        Instant now = clock.instant();
        KycVerification verification = new KycVerification(
                UUID.randomUUID(),
                accountId,
                request.fullNameOnId(),
                encryptionService.encrypt(request.idNumber()),
                encryptionService.encrypt(request.idCardFrontKey()),
                encryptionService.encrypt(request.idCardBackKey()),
                now);
        KycVerification saved = kycRepository.save(verification);
        syncProfileKycStatus(accountId, KycStatus.VERIFYING, now);
        return saved;
    }

    /** Doc lan nop gan nhat cua chinh chu tai khoan. Nem USR-404-KYC_NOT_FOUND neu chua tung nop. */
    @Transactional(readOnly = true)
    public KycVerification getMyLatestKyc(UUID accountId) {
        return kycRepository.findFirstByAccountIdOrderBySubmittedAtDesc(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KYC_NOT_FOUND));
    }

    /**
     * Chi danh cho Admin: doc lan nop gan nhat cua mot tai khoan bat ky kem giai ma so
     * CCCD va sinh presigned GET URL ngan han de xem anh mat truoc/sau - phuc vu man hinh
     * xet duyet, khong luu lai URL nay o dau ca.
     */
    @Transactional(readOnly = true)
    public KycReviewDetailResponse getLatestKycForReview(UUID accountId) {
        KycVerification verification = kycRepository.findFirstByAccountIdOrderBySubmittedAtDesc(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KYC_NOT_FOUND));
        String idNumber = encryptionService.decrypt(verification.getIdNumberEnc());
        String frontKey = encryptionService.decrypt(verification.getIdCardFrontUrlEnc());
        String backKey = encryptionService.decrypt(verification.getIdCardBackUrlEnc());
        return new KycReviewDetailResponse(
                verification.getId(),
                verification.getAccountId(),
                verification.getFullNameOnId(),
                idNumber,
                s3Service.createPresignedGetUrl(frontKey, VIEW_URL_TTL),
                s3Service.createPresignedGetUrl(backKey, VIEW_URL_TTL),
                verification.getStatus(),
                verification.getSubmittedAt(),
                verification.getReviewedAt(),
                verification.getRejectionReason());
    }

    /**
     * Chi danh cho Admin: hang doi cac lan nop KYC theo status (mac dinh VERIFYING o
     * controller), moi nhat truoc. Tra ve DTO nhe (khong giai ma so CCCD, khong ky presigned
     * URL) - xem chi tiet that su goi getLatestKycForReview() rieng cho dung accountId.
     */
    @Transactional(readOnly = true)
    public Page<KycReviewSummaryResponse> listForReview(KycStatus status, Pageable pageable) {
        return kycRepository.findByStatus(status, pageable).map(KycReviewSummaryResponse::from);
    }

    /** Admin duyet mot lan nop - chi cho phep khi dang VERIFYING, dong bo sang user_profiles neu co ho so. */
    @Transactional
    public KycVerification approve(UUID kycVerificationId, UUID adminAccountId) {
        KycVerification verification = requirePendingReview(kycVerificationId);
        Instant now = clock.instant();
        verification.approve(adminAccountId, now);
        syncProfileKycStatus(verification.getAccountId(), KycStatus.VERIFIED, now);
        return verification;
    }

    /** Admin tu choi mot lan nop kem ly do - chi cho phep khi dang VERIFYING. */
    @Transactional
    public KycVerification reject(UUID kycVerificationId, UUID adminAccountId, RejectKycRequest request) {
        KycVerification verification = requirePendingReview(kycVerificationId);
        Instant now = clock.instant();
        verification.reject(adminAccountId, request.rejectionReason(), now);
        syncProfileKycStatus(verification.getAccountId(), KycStatus.REJECTED, now);
        return verification;
    }

    /** Tim lan nop theo id, bat buoc dang o trang thai VERIFYING moi duoc duyet/tu choi. */
    private KycVerification requirePendingReview(UUID kycVerificationId) {
        KycVerification verification = kycRepository.findById(kycVerificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KYC_NOT_FOUND));
        if (verification.getStatus() != KycStatus.VERIFYING) {
            throw new BusinessException(ErrorCode.KYC_NOT_PENDING_REVIEW);
        }
        return verification;
    }

    /**
     * Dong bo user_profiles.kyc_status sau khi nop/duyet/tu choi - best-effort, bo qua neu
     * tai khoan chua tung tao ho so (lazy-create o Buoc 1, xem docs/PROGRESS-USER-MODULE.md).
     * Khong tao ho so moi o day: user_profiles con thieu fullName/operatingArea NOT NULL ma
     * KYC khong thu thap, tao ho so trong rong se vi pham rang buoc do.
     */
    private void syncProfileKycStatus(UUID accountId, KycStatus status, Instant now) {
        profileRepository.findByAccountId(accountId).ifPresent(profile -> {
            profile.changeKycStatus(status, now);
            profileRepository.save(profile);
        });
    }

    /**
     * Chan client gui object key khong thuoc prefix "kyc/{accountId}/" cua chinh minh -
     * phong truong hop client tinh nghich gui key cua tai khoan khac hoac chuoi bat ky,
     * du khong lo du lieu (Admin xem duoc moi object trong bucket) nhung tranh luu sai
     * lien ket giua ho so va anh that.
     */
    private void requireOwnKycPrefix(UUID accountId, String objectKey) {
        String expectedPrefix = "kyc/" + accountId + "/";
        if (!objectKey.startsWith(expectedPrefix)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Object key ảnh CCCD không hợp lệ.");
        }
    }
}
