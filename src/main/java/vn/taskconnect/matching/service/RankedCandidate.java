package vn.taskconnect.matching.service;

import java.util.UUID;

/**
 * Ket qua trung gian sau buoc 1+2 cua pipeline (retrieval cau truc + rerank ngu nghia) cho
 * MOT ung vien Tasker, dung noi bo giua TaskerMatchingService va AiSuggestionService - khong
 * phai DTO tra ra ngoai API (xem SuggestedTaskerResponse cho DTO cong khai).
 *
 * @param accountId id tai khoan Tasker
 * @param distanceKm khoang cach haversine tu Tasker toi dia diem Task, don vi km
 * @param priceMin gia toi thieu Tasker chao cho category nay, null neu khong khai bao
 * @param priceMax gia toi da Tasker chao cho category nay, null neu khong khai bao
 * @param yearsExperience so nam kinh nghiem Tasker tu khai
 * @param availabilityMatches true neu Tasker co khung gio ranh khop dung thu/gio cua
 *                             Task.scheduledAt, false neu co khai bao lich nhung khong khop
 *                             thoi diem nay, null neu Task khong co scheduledAt hoac Tasker
 *                             chua khai bao lich ranh nao (khong du du lieu de ket luan)
 * @param structuredScore diem cau truc da tinh (khoang cach/gia/kinh nghiem/lich ranh)
 * @param semanticScore cosine similarity giua embedding Task va embedding Tasker, null neu
 *                       thieu mot trong hai embedding (het quota/loi goi AI)
 * @param finalScore diem cuoi dung de sap thu tu tra ve FE
 */
record RankedCandidate(
        UUID accountId,
        double distanceKm,
        Long priceMin,
        Long priceMax,
        int yearsExperience,
        Boolean availabilityMatches,
        double structuredScore,
        Double semanticScore,
        double finalScore
) {
}
