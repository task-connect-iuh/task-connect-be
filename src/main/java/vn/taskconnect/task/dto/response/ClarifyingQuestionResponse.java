package vn.taskconnect.task.dto.response;

/**
 * Mot cau hoi lam ro hien trong modal "Hoi them" tren FE khi goi y tu anh khong khop ro danh
 * muc nao (xem TaskImageSuggestionResponse.suggestedCategoryId == null). Anh xa 1-1 tu
 * vn.taskconnect.ai.api.dto.ClarifyingQuestion - dinh nghia rieng o day (khong tra thang kieu
 * cua module AI ra API cong khai) de giu dung ranh gioi module, cung quy uoc voi cach
 * CategoryClassificationResult cua module AI duoc anh xa thanh UUID/int o TaskImageSuggestionResponse.
 *
 * @param key nhan ngan tom tat chu de cau hoi, FE dung de ghep cau tra loi vao mo ta
 * @param text cau hoi day du bang tieng Viet
 * @param placeholder vi du cau tra loi ngan goi y cach tra loi
 */
public record ClarifyingQuestionResponse(String key, String text, String placeholder) {
}
