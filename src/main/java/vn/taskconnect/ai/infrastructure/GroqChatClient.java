package vn.taskconnect.ai.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.taskconnect.ai.api.dto.CategoryClassificationRequest;
import vn.taskconnect.ai.api.dto.CategoryClassificationResult;
import vn.taskconnect.ai.api.dto.SuggestionReasonRequest;
import vn.taskconnect.ai.api.dto.SuggestionReasonResult;

/**
 * Goi Groq chat completions API (tuong thich OpenAI) de sinh ly do/diem tru cho lo ung vien.
 * Dung {@link RestClient} co san trong spring-web, khong them dependency Maven moi. Dung
 * Jackson {@link ObjectMapper} (co san trong Spring Boot) de parse noi dung JSON ma model
 * tra ve trong choices[0].message.content.
 */
@Component
public class GroqChatClient {

    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";

    private final AiProperties properties;
    private final PromptTemplates promptTemplates;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    GroqChatClient(AiProperties properties, PromptTemplates promptTemplates, ObjectMapper objectMapper) {
        this.properties = properties;
        this.promptTemplates = promptTemplates;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    /**
     * Goi Groq voi 1 message duy nhat (dung PromptTemplates de dung noi dung), roi parse
     * noi dung JSON model tra ve thanh List&lt;SuggestionReasonResult&gt;. Nem RuntimeException
     * neu goi HTTP that bai hoac JSON tra ve khong dung dinh dang - AiFacadeImpl la noi bat
     * va fallback ve danh sach rong, client nay khong tu quyet dinh fallback.
     */
    public List<SuggestionReasonResult> generateSuggestionReasons(SuggestionReasonRequest request) {
        String prompt = promptTemplates.buildSuggestionReasonPrompt(request);
        ChatRequest body = new ChatRequest(properties.llm().model(),
                List.of(new ChatMessage("user", prompt)), 0.2);
        try {
            ChatResponse response = restClient.post()
                    .uri(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.llm().apiKey())
                    .body(body)
                    .retrieve()
                    .body(ChatResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new IllegalStateException("Groq response rong");
            }
            String content = response.choices().get(0).message().content();
            String json = stripMarkdownFence(content);
            SuggestionReasonResult[] results = objectMapper.readValue(json, SuggestionReasonResult[].class);
            return List.of(results);
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("Goi Groq API hoac parse JSON that bai: " + ex.getMessage(), ex);
        }
    }

    /**
     * Goi Groq voi 1 message duy nhat de phan loai mo ta cong viec vao category/OTHER/
     * SUSPICIOUS (dung PromptTemplates), roi parse noi dung JSON model tra ve thanh
     * CategoryClassificationResult. Nem RuntimeException neu goi HTTP that bai hoac JSON tra
     * ve khong dung dinh dang - AiFacadeImpl la noi bat va fallback ve rong, client nay khong
     * tu quyet dinh fallback. Dung temperature thap hon (0.1) so voi generateSuggestionReasons
     * vi day la quyet dinh phan loai can on dinh, khong phai van phong dien giai tu do.
     */
    public CategoryClassificationResult classifyTaskCategory(CategoryClassificationRequest request) {
        String prompt = promptTemplates.buildCategoryClassificationPrompt(request);
        ChatRequest body = new ChatRequest(properties.llm().model(),
                List.of(new ChatMessage("user", prompt)), 0.1);
        try {
            ChatResponse response = restClient.post()
                    .uri(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.llm().apiKey())
                    .body(body)
                    .retrieve()
                    .body(ChatResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new IllegalStateException("Groq response rong");
            }
            String content = response.choices().get(0).message().content();
            String json = stripMarkdownFence(content);
            return objectMapper.readValue(json, CategoryClassificationResult.class);
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Goi Groq API hoac parse JSON phan loai category that bai: " + ex.getMessage(), ex);
        }
    }

    /**
     * Groq doi khi boc JSON trong markdown code fence (```json ... ```) du prompt da yeu cau
     * khong lam vay - go bo phong khi de tranh loi parse khong dang co.
     */
    private String stripMarkdownFence(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int lastFence = trimmed.lastIndexOf("```");
            if (lastFence >= 0) {
                trimmed = trimmed.substring(0, lastFence);
            }
        }
        return trimmed.trim();
    }

    private record ChatRequest(String model, List<ChatMessage> messages, double temperature) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ChatResponse(List<Choice> choices) {
    }

    private record Choice(ChatMessage message) {
    }
}
