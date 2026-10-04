package vn.taskconnect.ai.api.dto;

import java.util.List;

/**
 * Ket qua Gemini Vision phan tich anh cho ImageTaskSuggestionRequest. Day CHI la buoc "goi y
 * dien form" (function 1, xem .claude/rules/15-ai-module.md) - khong rang buoc gi trang thai
 * cong viec, Poster xem va sua/xoa tuy y truoc khi dang. Phan loai category CHINH THUC van
 * chay lai mot lan nua luc submit tren mo ta CUOI CUNG (function 2, TaskService.classifyAndFlag()),
 * KHONG tai su dung ket qua category o day cho quyet dinh do.
 *
 * @param title tieu de goi y, vd "Sua vong nuoc bi ri trong bep"
 * @param description mo ta chi tiet goi y dua tren nhung gi quan sat duoc trong anh
 * @param category ket qua phan loai danh muc goi y, tai su dung schema CategoryClassificationResult
 *        nhung outcome o day CHI co the la CATEGORY hoac OTHER - khong phan loai SUSPICIOUS tu
 *        anh (kiem duyet noi dung van la trach nhiem cua function 2 tren van ban cuoi cung)
 * @param clarifyingQuestions toi da 3 cau hoi lam ro khi description CHUA NEU RO duoc mot su co
 *        cu the (vd chi mo ta hinh dang thiet bi chung, khong neu trieu chung gi dang xay ra) -
 *        DOC LAP voi category.outcome() (co the co cau hoi du outcome la CATEGORY hay OTHER,
 *        xem Javadoc PromptTemplates.buildImageTaskSuggestionPrompt - giong bac si hoi benh
 *        nhan them du trieu chung du da biet benh nhan den vi khoa nao). RONG neu description da
 *        neu ro mot su co cu the. Day la "chuc nang 4" trong .claude/rules/15-ai-module.md: ảnh
 *        chỉ chụp được triệu chứng bề mặt, câu hỏi bù đắp phần nguyên nhân/diễn biến chỉ người
 *        tại hiện trường mới biết.
 */
public record ImageTaskSuggestionResult(String title, String description, CategoryClassificationResult category,
        List<ClarifyingQuestion> clarifyingQuestions) {
}
