package vn.taskconnect.ai.api.dto;

import java.util.List;

/**
 * Yeu cau Groq phan loai mot doan mo ta tu do vao DUNG MOT trong ba truong hop: khop mot danh
 * muc ung vien, khong khop danh muc nao (OTHER), hoac co dau hieu vi pham theo tieu chi cho
 * truoc (SUSPICIOUS). Cau truc chung, khong dung tu vung Task/ServiceCategory de module AI co
 * the tai su dung cho use case phan loai khac trong tuong lai ma khong doi API nay (xem
 * Javadoc AiFacade).
 *
 * @param description doan mo ta tu do can phan loai (vd mo ta cong viec Poster nhap)
 * @param candidates danh sach danh muc ung vien - day la buoc "retrieval" cua RAG, nguoi goi
 *        (Task) tu doc kho tri thuc (vd ServiceCategorySummary.description/keywords) va ghep
 *        thanh contextText truoc khi goi, module AI khong tu truy van kho tri thuc nao
 * @param suspiciousCriteria danh sach tieu chi duoc coi la vi pham, do nguoi goi tu dinh
 *        nghia - module AI khong biet truoc nghiep vu nao la "vi pham"
 */
public record CategoryClassificationRequest(String description, List<CandidateCategory> candidates,
        List<SuspiciousCriterion> suspiciousCriteria) {

    /**
     * Mot danh muc ung vien. candidateId dung de khop nguoc ket qua Groq tra ve voi dung danh
     * muc o phia nguoi goi, khong mang y nghia nghiep vu voi module AI.
     */
    public record CandidateCategory(String candidateId, String name, String contextText) {
    }

    /**
     * Mot tieu chi vi pham. code dung de khop nguoc ket qua Groq tra ve, description la noi
     * dung tieu chi bang tieng Viet de LLM doi chieu.
     */
    public record SuspiciousCriterion(String code, String description) {
    }
}
