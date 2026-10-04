package vn.taskconnect.task.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Ket qua goi y dien form tu anh cho POST /api/v1/tasks/analyze-image - CHI la goi y (function 1,
 * xem .claude/rules/15-ai-module.md), Poster xem va sua/xoa tuy y truoc khi dang, khong rang
 * buoc gi. Khong goi y gia/lich ranh - anh khong the hien thi hai thu nay (xem quyet dinh da
 * chot voi nguoi dung), Poster tu nhap hoac de trong nhu binh thuong.
 *
 * @param available false neu AI khong phan tich duoc (het quota/loi mang) - FE hien thong bao
 *                   va de Poster tu dien tay, cac truong con lai deu null/0/rong khi available=false
 * @param title tieu de goi y, null neu available=false
 * @param description mo ta chi tiet goi y, null neu available=false
 * @param suggestedCategoryId id danh muc goi y (da anh xa tu code sang UUID that), null neu
 *                             available=false hoac AI khong khop ro danh muc nao (outcome=OTHER)
 * @param suggestedCategoryConfidence do tin cay 0-100, chi co y nghia khi suggestedCategoryId khac null
 * @param clarifyingQuestions toi da 3 cau hoi lam ro khi description CHUA neu ro mot su co cu
 *        the - DOC LAP voi suggestedCategoryId (co the co cau hoi du da xac dinh duoc category
 *        hay chua, xem Javadoc ImageTaskSuggestionResult). Rong neu description da du cu the.
 *        FE hien modal "Hoi them" khi list nay khong rong, bat ke suggestedCategoryId co gia
 *        tri hay khong.
 */
public record TaskImageSuggestionResponse(boolean available, String title, String description,
        UUID suggestedCategoryId, int suggestedCategoryConfidence,
        List<ClarifyingQuestionResponse> clarifyingQuestions) {

    /** Ket qua khi AI khong phan tich duoc (het quota Gemini Vision/loi mang) - tat ca truong con lai rong. */
    public static TaskImageSuggestionResponse unavailable() {
        return new TaskImageSuggestionResponse(false, null, null, null, 0, List.of());
    }
}
