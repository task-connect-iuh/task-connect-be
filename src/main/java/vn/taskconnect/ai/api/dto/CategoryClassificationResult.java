package vn.taskconnect.ai.api.dto;

/**
 * Ket qua Groq tra ve cho mot CategoryClassificationRequest.
 *
 * @param outcome CATEGORY (khop mot candidate), OTHER (khong khop candidate nao), hoac
 *                SUSPICIOUS (khop mot tieu chi vi pham) - xem Javadoc ClassificationOutcome
 * @param candidateId id danh muc khop, chi co gia tri khi outcome == CATEGORY, trung voi
 *                     CandidateCategory.candidateId da gui, null trong hai truong hop con lai
 * @param confidence do tin cay 0-100 Groq uoc luong, chi co y nghia khi outcome == CATEGORY,
 *                    bang 0 trong hai truong hop con lai
 * @param suspiciousReason ma cua tieu chi vi pham khop (SuspiciousCriterion.code da gui), chi
 *                          co gia tri khi outcome == SUSPICIOUS, null trong hai truong hop con lai
 */
public record CategoryClassificationResult(ClassificationOutcome outcome, String candidateId, int confidence,
        String suspiciousReason) {

    /** Ba ket qua phan loai co the co, xem Javadoc CategoryClassificationResult tung truong. */
    public enum ClassificationOutcome {
        CATEGORY,
        OTHER,
        SUSPICIOUS
    }
}
