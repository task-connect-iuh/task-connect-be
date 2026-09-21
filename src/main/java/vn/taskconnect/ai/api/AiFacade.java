package vn.taskconnect.ai.api;

import java.util.List;
import java.util.Optional;
import vn.taskconnect.ai.api.dto.CategoryClassificationRequest;
import vn.taskconnect.ai.api.dto.CategoryClassificationResult;
import vn.taskconnect.ai.api.dto.SuggestionReasonRequest;
import vn.taskconnect.ai.api.dto.SuggestionReasonResult;

/**
 * Be mat cong khai duy nhat cua module AI. Module khac (Matching cho goi y Tasker, Task cho
 * phan loai category luc dang viec) chi duoc goi qua day, cam import class trong
 * {@code ai.infrastructure}/{@code ai.service} hoac tu inject GeminiEmbeddingClient/
 * GroqChatClient. Module AI khong biet gi ve Task/Tasker/Invite - moi tham so deu la kieu
 * du lieu tong quat (String, float[], record trung lap voi vocab cua module goi).
 *
 * <p>Ca ba method deu KHONG BAO GIO nem exception ra ngoai - loi goi provider hoac het
 * quota deu duoc nuot va tra ve gia tri rong, de module goi tu quyet dinh fallback (vd
 * TaskerMatchingService bo qua semantic score, AiSuggestionService dung template dung san,
 * TaskService gan co hau kiem CLASSIFICATION_FAILED).
 */
public interface AiFacade {

    /**
     * Sinh embedding vector cho mot doan van ban bang Gemini Embedding API. Rong neu het
     * quota embedding trong ngay hoac loi goi API (timeout, provider tra loi, parse loi...).
     */
    Optional<float[]> embed(String text);

    /**
     * Goi Groq mot lan duy nhat cho ca lo ung vien, yeu cau LLM viet ly do/diem tru bang
     * tieng Viet cho TUNG ung vien dua tren facts da cho - khong tu bia so lieu ngoai input
     * (xem PromptTemplates). Tra {@code List.of()} neu het quota LLM trong ngay hoac loi goi/
     * parse JSON tra ve - nguoi goi (AiSuggestionService) phai tu dung template fallback rieng
     * cua minh, module nay khong biet nghia nghiep vu cua "template".
     */
    List<SuggestionReasonResult> generateSuggestionReasons(SuggestionReasonRequest request);

    /**
     * Phan loai mot doan mo ta vao 1 trong cac danh muc ung vien, hoac OTHER/SUSPICIOUS (xem
     * CategoryClassificationRequest/Result, .claude/rules/15-ai-module.md). Rong neu het quota
     * LLM trong ngay hoac loi goi/parse JSON tra ve - nguoi goi (TaskService) tu quyet dinh
     * gan co hau kiem CLASSIFICATION_FAILED khi rong, module nay khong tu quyet dinh fallback.
     */
    Optional<CategoryClassificationResult> classifyTaskCategory(CategoryClassificationRequest request);
}
