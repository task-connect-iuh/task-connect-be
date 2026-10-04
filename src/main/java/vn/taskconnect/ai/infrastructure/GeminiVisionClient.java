package vn.taskconnect.ai.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import vn.taskconnect.ai.api.dto.CategoryClassificationResult;
import vn.taskconnect.ai.api.dto.ClarifyingQuestion;
import vn.taskconnect.ai.api.dto.ImageTaskSuggestionRequest;
import vn.taskconnect.ai.api.dto.ImageTaskSuggestionResult;

/**
 * Goi Google Gemini API endpoint generateContent (KHAC voi embedContent cua
 * GeminiEmbeddingClient) de doc anh + sinh goi y dien form dang viec. Dung {@link RestClient}
 * co san trong spring-web, khong them dependency Maven moi, cung quy uoc voi
 * GeminiEmbeddingClient/GroqChatClient (khong dung SDK, khong pull model ve local).
 *
 * <p>Hinh dang REQUEST (POST .../models/{model}:generateContent?key={apiKey}, body
 * {"contents": [{"parts": [{"text": ...}, {"inline_data": {"mime_type": ..., "data":
 * base64}}]}]}) DA XAC NHAN DUNG bang mot lan goi that (2026-09-26): Gemini tra ve loi 404 co
 * cau truc JSON dung dinh dang cua Gemini ("model khong con ton tai"), tuc request da toi
 * dung endpoint va duoc parse hop le, chi model luc do (gemini-2.0-flash) da bi Google go -
 * xem docs/PROGRESS-AI-MATCHING-MODULE.md. Rieng hinh dang RESPONSE THANH CONG
 * (candidates[0].content.parts[0].text chua JSON) van CHUA duoc xac nhan bang mot lan goi
 * thanh cong that su - kiem tra lai sau khi doi model dung (GEMINI_VISION_MODEL).
 *
 * <p>Gemini thinh thoang tra 503 UNAVAILABLE ("model dang qua tai, thu lai sau") - loi tam
 * thoi phia provider, KHONG phai loi request cua minh (xac nhan thuc te 2026-09-26). Tu dong
 * thu lai toi da {@link #MAX_ATTEMPTS} lan cho DUNG truong hop 5xx nay truoc khi nem loi ra
 * ngoai cho AiFacadeImpl fallback - KHONG retry loi 4xx (400/401/404/429...) vi retry khong
 * giai quyet duoc nguyen nhan (request sai, quota het, model sai ten).
 */
@Component
public class GeminiVisionClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiVisionClient.class);

    private static final String ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    /** So lan thu toi da khi Gemini tra 5xx tam thoi (1 lan goc + 2 lan thu lai). */
    private static final int MAX_ATTEMPTS = 3;
    /** Khoang cho giua cac lan thu lai (ms), tang dan - cung tinh than voi FE (RETRY_DELAYS_MS o SuggestedTaskersPanel.tsx). */
    private static final long[] RETRY_DELAYS_MS = {500, 1500};

    private final AiProperties properties;
    private final PromptTemplates promptTemplates;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    GeminiVisionClient(AiProperties properties, PromptTemplates promptTemplates, ObjectMapper objectMapper) {
        this.properties = properties;
        this.promptTemplates = promptTemplates;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    /**
     * Goi Gemini Vision voi anh + prompt (dung PromptTemplates), roi parse noi dung JSON model
     * tra ve thanh ImageTaskSuggestionResult. Tu dong thu lai neu Gemini tra 5xx tam thoi (xem
     * Javadoc class) - LAN DAU dung {@code vision.model}, TU LAN THU LAI THU 2 tro di doi sang
     * {@code vision.fallbackModel} (yeu cau nguoi dung: mot model khac co the roi vao pool ha
     * tang/quota khac cua Gemini, tang co hoi thanh cong thay vi lien tuc doi 1 model dang qua
     * tai). Nem RuntimeException neu het luot thu lai van loi, hoac loi khac 5xx, hoac JSON tra
     * ve khong dung dinh dang - AiFacadeImpl la noi bat va fallback ve rong, client nay khong
     * tu quyet dinh fallback.
     */
    public ImageTaskSuggestionResult suggestTaskFromImage(ImageTaskSuggestionRequest request) {
        String prompt = promptTemplates.buildImageTaskSuggestionPrompt(request.candidates());
        String base64Image = Base64.getEncoder().encodeToString(request.imageBytes());
        GenerateContentRequest body = new GenerateContentRequest(List.of(new Content(List.of(
                new Part(prompt, null), new Part(null, new InlineData(request.mimeType(), base64Image))))));

        RuntimeException lastError = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // Lan dau (attempt=0) dung model chinh, tu lan thu lai thu 2 (attempt>=1) doi sang
            // fallbackModel - xem Javadoc method.
            String model = attempt == 0 ? properties.vision().model() : properties.vision().fallbackModel();
            String url = String.format(ENDPOINT_TEMPLATE, model, properties.vision().apiKey());
            try {
                return callAndParse(url, body);
            } catch (HttpServerErrorException ex) {
                lastError = ex;
                log.warn("Gemini Vision ({}) tra loi 5xx tam thoi (lan thu {}/{}): {}", model, attempt + 1, MAX_ATTEMPTS, ex.getMessage());
                if (attempt < RETRY_DELAYS_MS.length) {
                    sleep(RETRY_DELAYS_MS[attempt]);
                }
            } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException ex) {
                throw new IllegalStateException(
                        "Goi Gemini Vision API hoac parse JSON goi y anh that bai: " + ex.getMessage(), ex);
            }
        }
        throw new IllegalStateException(
                "Goi Gemini Vision API that bai sau " + MAX_ATTEMPTS + " lan thu (Gemini qua tai lien tuc): "
                        + (lastError != null ? lastError.getMessage() : "khong ro loi"), lastError);
    }

    /** Mot lan goi HTTP + parse response - tach rieng de vong lap retry o suggestTaskFromImage() goi lai duoc. */
    private ImageTaskSuggestionResult callAndParse(String url, GenerateContentRequest body)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        GenerateContentResponse response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(GenerateContentResponse.class);
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalStateException("Gemini generateContent response rong");
        }
        List<Part> parts = response.candidates().get(0).content().parts();
        if (parts == null || parts.isEmpty() || parts.get(0).text() == null) {
            throw new IllegalStateException("Gemini generateContent khong tra ve noi dung text");
        }
        String json = stripMarkdownFence(parts.get(0).text());
        SuggestionJson parsed = objectMapper.readValue(json, SuggestionJson.class);
        CategoryClassificationResult category = new CategoryClassificationResult(
                parsed.category().outcome(), parsed.category().candidateId(),
                parsed.category().confidence(), null);
        List<ClarifyingQuestion> clarifyingQuestions = parsed.clarifyingQuestions() == null
                ? Collections.emptyList() : parsed.clarifyingQuestions();
        return new ImageTaskSuggestionResult(parsed.title(), parsed.description(), category, clarifyingQuestions);
    }

    /** Cho giua cac lan thu lai - nuot InterruptedException va khoi phuc interrupt flag dung chuan. */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /** Gemini doi khi boc JSON trong markdown code fence du prompt da yeu cau khong lam vay - go bo phong. */
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

    /** Body request theo dung dinh dang generateContent cua Gemini - xem Javadoc class. */
    private record GenerateContentRequest(List<Content> contents) {
    }

    private record Content(List<Part> parts) {
    }

    /** Mot phan tu trong "parts" - CHI mot trong hai truong (text HOAC inlineData) co gia tri khac null. */
    private record Part(String text, @JsonProperty("inline_data") InlineData inlineData) {
    }

    private record InlineData(@JsonProperty("mime_type") String mimeType, String data) {
    }

    private record GenerateContentResponse(List<Candidate> candidates) {
    }

    private record Candidate(Content content) {
    }

    /** Hinh dang JSON model tra ve trong parts[0].text - xem prompt buildImageTaskSuggestionPrompt(). */
    private record SuggestionJson(String title, String description, CategoryJson category,
            List<ClarifyingQuestion> clarifyingQuestions) {
    }

    private record CategoryJson(CategoryClassificationResult.ClassificationOutcome outcome, String candidateId,
            int confidence) {
    }
}
