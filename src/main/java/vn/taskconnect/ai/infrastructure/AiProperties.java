package vn.taskconnect.ai.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cau hinh cho ca hai provider AI dung chung trong module (Gemini embedding + Groq LLM),
 * doc tu prefix {@code ai.*} trong application.yml - anh xa tu bien moi truong
 * GEMINI_API_KEY/GROQ_API_KEY qua .env. apiKey khong co gia tri default (fail-fast giong
 * JWT_SECRET/DB_PASSWORD), model/dailyQuota co default vi day la config it rui ro, tien loi
 * cho dev (xem comment trong application.yml).
 *
 * @param embedding cau hinh Gemini Embedding API
 * @param llm cau hinh Groq chat completions API
 */
@ConfigurationProperties(prefix = "ai")
public record AiProperties(EmbeddingConfig embedding, LlmConfig llm) {

    /**
     * @param provider ten provider, hien chi ho tro "gemini" - giu truong nay de doi provider
     *                 sau nay khong phai doi ten field cau hinh
     * @param apiKey khoa API Gemini, BAT BUOC set qua bien moi truong GEMINI_API_KEY
     * @param model ten model embedding, vd gemini-embedding-001
     * @param dailyQuota so luot goi embedding toi da moi ngay truoc khi QuotaTrackerService
     *                    chan bot va AiFacadeImpl fallback ve rong
     */
    public record EmbeddingConfig(String provider, String apiKey, String model, int dailyQuota) {
    }

    /**
     * @param provider ten provider, hien chi ho tro "groq"
     * @param apiKey khoa API Groq, BAT BUOC set qua bien moi truong GROQ_API_KEY
     * @param model ten model LLM, vd llama-3.3-70b-versatile
     * @param dailyQuota so luot goi LLM toi da moi ngay truoc khi QuotaTrackerService chan bot
     *                    va AiFacadeImpl fallback ve danh sach rong
     */
    public record LlmConfig(String provider, String apiKey, String model, int dailyQuota) {
    }
}
