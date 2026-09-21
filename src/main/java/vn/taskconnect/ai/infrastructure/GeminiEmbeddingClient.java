package vn.taskconnect.ai.infrastructure;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Goi Google Gemini Embedding API qua REST (khong dung SDK, khong pull model ve local -
 * xem quyet dinh trong plan da duyet). Dung {@link RestClient} co san trong spring-web,
 * khong them dependency Maven moi.
 *
 * <p>LUU Y CHUA XAC MINH: hinh dang request/response duoi day duoc hien thuc theo dung
 * tai lieu cong khai cua Gemini API tai thoi diem viet code
 * (POST .../models/{model}:embedContent?key={apiKey}, body {"content": {"parts": [{"text":
 * ...}]}}, response {"embedding": {"values": [...]}}), nhung CHUA duoc goi thu that qua
 * mang do moi truong phat trien khong co ket noi Internet luc code. Bat buoc kiem tra lai
 * bang mot lan goi that (Postman hoac test thu cong) truoc khi dua vao dung o production.
 */
@Component
public class GeminiEmbeddingClient {

    private static final String ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:embedContent?key=%s";

    private final AiProperties properties;
    private final RestClient restClient;

    GeminiEmbeddingClient(AiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    /**
     * Goi Gemini de lay embedding vector cho mot doan van ban. Nem RuntimeException (bao boc
     * moi loi HTTP/parse) neu that bai - AiFacadeImpl la noi bat va quy dinh fallback, client
     * nay khong tu quyet dinh fallback.
     */
    public float[] embed(String text) {
        String url = String.format(ENDPOINT_TEMPLATE, properties.embedding().model(), properties.embedding().apiKey());
        EmbedRequest requestBody = new EmbedRequest(new Content(List.of(new Part(text))));
        try {
            EmbedResponse response = restClient.post()
                    .uri(url)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(EmbedResponse.class);
            if (response == null || response.embedding() == null || response.embedding().values() == null) {
                throw new IllegalStateException("Gemini embedding response rong hoac sai dinh dang");
            }
            List<Double> values = response.embedding().values();
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i).floatValue();
            }
            return vector;
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Goi Gemini embedding API that bai: " + ex.getMessage(), ex);
        }
    }

    /** Body request theo dung dinh dang embedContent cua Gemini - xem Javadoc class. */
    private record EmbedRequest(Content content) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }

    /** Body response theo dung dinh dang embedContent cua Gemini - xem Javadoc class. */
    private record EmbedResponse(Embedding embedding) {
    }

    private record Embedding(List<Double> values) {
    }
}
