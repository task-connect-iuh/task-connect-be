package vn.taskconnect.ai.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;
import vn.taskconnect.ai.infrastructure.AiProperties;

/**
 * Bo dem so luot goi Gemini/Groq trong ngay, tach rieng cho embedding va LLM vi hai
 * provider co quota doc lap nhau. Luu hoan toan trong bo nho (AtomicInteger + LocalDate
 * cho ngay reset gan nhat) - KHONG ben vung qua Redis/DB, se reset ve 0 moi khi app restart.
 * Chap nhan duoc voi quy mo do an: so luot goi that su rat nho (vai chuc/ngay), muc dich
 * chinh cua bo dem nay la chan tran quota mien phi cua provider, khong phai bao toan tuyet
 * doi so lieu thong ke qua cac lan restart.
 */
@Service
class QuotaTrackerService {

    private final AiProperties properties;

    private final AtomicInteger embeddingCount = new AtomicInteger(0);
    private final AtomicInteger llmCount = new AtomicInteger(0);
    private final Clock clock;

    private LocalDate embeddingResetDate;
    private LocalDate llmResetDate;

    QuotaTrackerService(AiProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        LocalDate today = LocalDate.now(clock);
        this.embeddingResetDate = today;
        this.llmResetDate = today;
    }

    /**
     * Kiem tra con quota embedding hom nay khong, neu con thi tang bo dem va tra true (atomic
     * check-and-increment). Dung synchronized don gian thay vi CAS phuc tap - tan suat goi
     * rat thap (toi da vai chuc lan/ngay), khong dang lo tranh chap luong.
     */
    synchronized boolean tryConsumeEmbeddingQuota() {
        resetIfNewDay();
        if (embeddingCount.get() >= properties.embedding().dailyQuota()) {
            return false;
        }
        embeddingCount.incrementAndGet();
        return true;
    }

    /** Tuong tu tryConsumeEmbeddingQuota() nhung cho quota LLM (Groq) rieng biet. */
    synchronized boolean tryConsumeLlmQuota() {
        resetIfNewDay();
        if (llmCount.get() >= properties.llm().dailyQuota()) {
            return false;
        }
        llmCount.incrementAndGet();
        return true;
    }

    /** Ve 0 ca hai bo dem neu ngay hien tai da sang ngay moi so voi lan reset gan nhat. */
    private void resetIfNewDay() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(embeddingResetDate)) {
            embeddingCount.set(0);
            embeddingResetDate = today;
        }
        if (!today.equals(llmResetDate)) {
            llmCount.set(0);
            llmResetDate = today;
        }
    }
}
