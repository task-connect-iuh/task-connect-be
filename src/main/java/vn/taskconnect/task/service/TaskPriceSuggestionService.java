package vn.taskconnect.task.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.ai.api.AiFacade;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.task.dto.request.SuggestTaskPriceRequest;
import vn.taskconnect.task.dto.response.TaskPriceSuggestionResponse;
import vn.taskconnect.task.entity.Task;
import vn.taskconnect.task.entity.TaskApplication;
import vn.taskconnect.task.entity.TaskPriceHistory;
import vn.taskconnect.task.infrastructure.TaskPriceSuggestionProperties;
import vn.taskconnect.task.repository.TaskApplicationRepository;
import vn.taskconnect.task.repository.TaskPriceHistoryRepository;
import vn.taskconnect.task.repository.TaskRepository;
import vn.taskconnect.user.api.UserFacade;

/**
 * Goi y muc gia luc dang viec (UC06), dua tren gia da THAT SU chot (task_price_history.accepted_at
 * != null, xem UC16 thuong luong gia) cua cac task TUONG TU trong qua khu, cung category. Day
 * la "anchor pricing" cho Poster (Poster tu do sua/bo qua) - KHAC voi viec da bo tieu chi gia
 * khoi Matching (Matching so sanh Tasker/budget cheo nhau, khong hop ly; o day chi lay median
 * cua cac gia da chot tuong tu de goi y diem khoi dau cho MOT task moi, khong so sanh cheo Tasker).
 *
 * <p>KHONG cache embedding (yeu cau nguoi dung, quyet dinh 2026-09-26): tinh lai tu dau moi lan
 * goi, chap nhan ton quota Gemini Embedding hon theo thoi gian khi du lieu lon dan - phu hop quy
 * mo do an hien tai (AI_EMBEDDING_DAILY_QUOTA=1000/ngay, vai chuc ban ghi moi category). Neu sau
 * nay du lieu lon, can chuyen sang cache embedding rieng (xem thao luan da chot, chua lam).
 */
@Service
public class TaskPriceSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(TaskPriceSuggestionService.class);

    private final TaskPriceHistoryRepository priceHistoryRepository;
    private final TaskApplicationRepository applicationRepository;
    private final TaskRepository taskRepository;
    private final UserFacade userFacade;
    private final AiFacade aiFacade;
    private final TaskPriceSuggestionProperties properties;

    public TaskPriceSuggestionService(TaskPriceHistoryRepository priceHistoryRepository,
            TaskApplicationRepository applicationRepository, TaskRepository taskRepository,
            UserFacade userFacade, AiFacade aiFacade, TaskPriceSuggestionProperties properties) {
        this.priceHistoryRepository = priceHistoryRepository;
        this.applicationRepository = applicationRepository;
        this.taskRepository = taskRepository;
        this.userFacade = userFacade;
        this.aiFacade = aiFacade;
        this.properties = properties;
    }

    /**
     * Tinh goi y gia cho request (categoryId + title + description hien tai tren form). Nem
     * CATEGORY_NOT_FOUND neu categoryId khong hop le/khong active - dung cung kieu kiem tra voi
     * TaskService.createTask(). Tra ve unavailable() (KHONG nem loi) neu khong du du lieu tuong
     * tu hoac AiFacade.embed() loi/het quota - khong chan Poster tiep tuc dien tay.
     */
    @Transactional(readOnly = true)
    public TaskPriceSuggestionResponse suggestPrice(SuggestTaskPriceRequest request) {
        boolean categoryExists = userFacade.listActiveServiceCategories().stream()
                .anyMatch(c -> c.id().equals(request.categoryId()));
        if (!categoryExists) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        List<TaskPriceHistory> accepted = priceHistoryRepository.findByAcceptedAtIsNotNull();
        if (accepted.isEmpty()) {
            log.info("Goi y gia: khong co du lieu gia da chot nao trong he thong.");
            return TaskPriceSuggestionResponse.unavailable();
        }

        Set<UUID> applicationIds = new HashSet<>();
        for (TaskPriceHistory history : accepted) {
            applicationIds.add(history.getApplicationId());
        }
        Map<UUID, UUID> taskIdByApplicationId = new HashMap<>();
        for (TaskApplication application : applicationRepository.findAllById(applicationIds)) {
            taskIdByApplicationId.put(application.getId(), application.getTaskId());
        }

        Set<UUID> candidateTaskIds = new HashSet<>(taskIdByApplicationId.values());
        Map<UUID, Task> taskInCategoryById = new HashMap<>();
        for (Task task : taskRepository.findAllById(candidateTaskIds)) {
            if (task.getCategoryId().equals(request.categoryId())) {
                taskInCategoryById.put(task.getId(), task);
            }
        }
        if (taskInCategoryById.isEmpty()) {
            log.info("Goi y gia: khong co task nao da chot gia trong category {}.", request.categoryId());
            return TaskPriceSuggestionResponse.unavailable();
        }

        String draftText = (request.title() == null ? "" : request.title()) + ". "
                + (request.description() == null ? "" : request.description());
        Optional<float[]> draftVector = aiFacade.embed(draftText);
        if (draftVector.isEmpty()) {
            log.warn("Goi y gia: khong embed duoc noi dung form (het quota/loi AI), tra ve unavailable.");
            return TaskPriceSuggestionResponse.unavailable();
        }

        List<ScoredCandidate> scored = new ArrayList<>();
        for (TaskPriceHistory history : accepted) {
            UUID taskId = taskIdByApplicationId.get(history.getApplicationId());
            Task task = taskId != null ? taskInCategoryById.get(taskId) : null;
            if (task == null) {
                continue;
            }
            String candidateText = (task.getTitle() == null ? "" : task.getTitle()) + ". "
                    + (task.getDescription() == null ? "" : task.getDescription());
            Optional<float[]> candidateVector = aiFacade.embed(candidateText);
            if (candidateVector.isEmpty()) {
                continue;
            }
            double similarity = cosineSimilarity(draftVector.get(), candidateVector.get());
            log.info("Goi y gia - ung vien task={} similarity={} amount={}", task.getId(),
                    String.format("%.4f", similarity), history.getAmount());
            if (similarity >= properties.similarityThreshold()) {
                scored.add(new ScoredCandidate(task.getId(), similarity, history.getAmount()));
            }
        }

        if (scored.size() < properties.minSamples()) {
            log.info("Goi y gia: chi co {} ung vien vuot nguong similarity {} (can toi thieu {}), tra ve unavailable.",
                    scored.size(), properties.similarityThreshold(), properties.minSamples());
            return TaskPriceSuggestionResponse.unavailable();
        }

        scored.sort(Comparator.comparingDouble(ScoredCandidate::similarity).reversed());
        List<ScoredCandidate> top = scored.subList(0, Math.min(properties.topK(), scored.size()));
        long median = median(top.stream().map(ScoredCandidate::amount).sorted().toList());
        log.info("Goi y gia: median={} dong tu {} ung vien tuong tu nhat (trong {} ung vien vuot nguong).",
                median, top.size(), scored.size());
        return new TaskPriceSuggestionResponse(true, median, top.size());
    }

    /** Trung vi (median) cua danh sach da sap tang dan - lay trung binh 2 phan tu giua neu so luong chan. */
    private long median(List<Long> sortedAmounts) {
        int size = sortedAmounts.size();
        int mid = size / 2;
        if (size % 2 == 1) {
            return sortedAmounts.get(mid);
        }
        return Math.round((sortedAmounts.get(mid - 1) + sortedAmounts.get(mid)) / 2.0);
    }

    /** Cosine similarity co ban, gia tri trong [-1, 1] - dung 0 cho vector 0-do dai de tranh chia cho 0. */
    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** Mot ung vien task qua khu da vuot nguong similarity - dung noi bo de sap xep/lay top K truoc khi tinh median. */
    private record ScoredCandidate(UUID taskId, double similarity, long amount) {
    }
}
