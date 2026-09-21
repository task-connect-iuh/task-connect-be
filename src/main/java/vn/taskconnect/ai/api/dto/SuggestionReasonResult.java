package vn.taskconnect.ai.api.dto;

import java.util.List;

/**
 * Ket qua Groq tra ve cho DUNG MOT ung vien trong lo da gui o SuggestionReasonRequest.
 * candidateId dung de khop nguoc lai ung vien tuong ung o phia nguoi goi (Matching).
 *
 * @param candidateId dinh danh ung vien, trung voi CandidateFacts.candidateId da gui
 * @param confidence do tin cay 0-100 do Groq uoc luong, khong phai diem xep hang cuoi cung
 *                    (xep hang van do Java tinh o TaskerMatchingService, xem Javadoc class)
 * @param reasons danh sach diem hop (uu diem) bang tieng Viet
 * @param concerns danh sach diem chua hop/rui ro bang tieng Viet, rong neu khong co gi dang ke
 */
public record SuggestionReasonResult(String candidateId, int confidence, List<String> reasons,
        List<String> concerns) {
}
