package vn.taskconnect.ai.service;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.taskconnect.ai.api.AiFacade;
import vn.taskconnect.ai.api.dto.SuggestionReasonRequest;
import vn.taskconnect.ai.api.dto.SuggestionReasonResult;
import vn.taskconnect.ai.infrastructure.GeminiEmbeddingClient;
import vn.taskconnect.ai.infrastructure.GroqChatClient;

/**
 * Trien khai duy nhat cua AiFacade - khong module nao khac trong ai duoc implements interface
 * nay. Dieu phoi quota + goi client ha tang, bat MOI exception tu GeminiEmbeddingClient/
 * GroqChatClient va tra ve gia tri rong thay vi nem tiep ra ngoai (xem Javadoc AiFacade -
 * day la hop dong bat buoc, module goi (Matching) dua vao no de khong bao gio sap vi loi AI).
 */
@Service
class AiFacadeImpl implements AiFacade {

    private static final Logger log = LoggerFactory.getLogger(AiFacadeImpl.class);

    private final GeminiEmbeddingClient embeddingClient;
    private final GroqChatClient chatClient;
    private final QuotaTrackerService quotaTracker;

    AiFacadeImpl(GeminiEmbeddingClient embeddingClient, GroqChatClient chatClient,
            QuotaTrackerService quotaTracker) {
        this.embeddingClient = embeddingClient;
        this.chatClient = chatClient;
        this.quotaTracker = quotaTracker;
    }

    /** Kiem tra quota truoc, goi GeminiEmbeddingClient, nuot moi loi va log warn thay vi nem ra. */
    @Override
    public Optional<float[]> embed(String text) {
        if (!quotaTracker.tryConsumeEmbeddingQuota()) {
            log.warn("Da het quota embedding trong ngay - bo qua goi Gemini, tra ve rong.");
            return Optional.empty();
        }
        try {
            return Optional.of(embeddingClient.embed(text));
        } catch (RuntimeException ex) {
            log.warn("Goi Gemini embedding that bai, tra ve rong de nguoi goi tu fallback: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /** Kiem tra quota truoc, goi GroqChatClient, nuot moi loi va log warn thay vi nem ra. */
    @Override
    public List<SuggestionReasonResult> generateSuggestionReasons(SuggestionReasonRequest request) {
        if (request.candidates().isEmpty()) {
            return List.of();
        }
        if (!quotaTracker.tryConsumeLlmQuota()) {
            log.warn("Da het quota LLM trong ngay - bo qua goi Groq, tra ve rong.");
            return List.of();
        }
        try {
            return chatClient.generateSuggestionReasons(request);
        } catch (RuntimeException ex) {
            log.warn("Goi Groq LLM that bai, tra ve rong de nguoi goi tu fallback template: {}", ex.getMessage());
            return List.of();
        }
    }
}
