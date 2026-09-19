package vn.taskconnect.matching.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.ai.api.AiFacade;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.matching.entity.TaskEmbeddingCache;
import vn.taskconnect.matching.entity.TaskerProfileEmbedding;
import vn.taskconnect.matching.infrastructure.MatchingProperties;
import vn.taskconnect.matching.repository.TaskEmbeddingCacheRepository;
import vn.taskconnect.matching.repository.TaskerProfileEmbeddingRepository;
import vn.taskconnect.task.api.TaskFacade;
import vn.taskconnect.task.api.dto.TaskSummary;
import vn.taskconnect.user.api.KycStatus;
import vn.taskconnect.user.api.SkillVerificationStatus;
import vn.taskconnect.user.api.UserFacade;
import vn.taskconnect.user.api.dto.TaskerMatchCandidateSummary;

/**
 * Buoc 1 (retrieval cau truc) + buoc 2 (rerank ngu nghia) cua pipeline goi y Tasker - xem
 * plan da duyet "AI Tasker Suggestion". Chi tinh diem/xep hang, KHONG viet ly do (xem
 * AiSuggestionService cho buoc 3). Khong tu goi Gemini/Groq HTTP truc tiep - moi loi goi AI
 * di qua {@link AiFacade}, dung theo dung ranh gioi module da chot trong plan.
 *
 * <p>Gia dinh mui gio Viet Nam (Asia/Ho_Chi_Minh) khi quy doi Task.scheduledAt (UTC, luu
 * Instant) sang thu/gio de khop TaskerAvailability - san pham chi phuc vu thi truong Viet
 * Nam (xem CLAUDE.md pham vi du an), chua co nhu cau da mui gio.
 */
@Service
public class TaskerMatchingService {

    private static final Logger log = LoggerFactory.getLogger(TaskerMatchingService.class);
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    // Muc kinh nghiem tu do tro di duoc coi la diem toi da (1.0) cho tieu chi kinh nghiem -
    // gia tri tam, chua duoc kiem chung bang du lieu that (xem plan da duyet).
    private static final int EXPERIENCE_SCORE_CAP_YEARS = 5;
    // Ban kinh mac dinh (km) dung de TINH DIEM khoang cach khi Tasker chua tu khai bao
    // preferredRadiusKm - KHONG dung de loc cung (Tasker khong khai bao thi khong bi loai vi
    // khoang cach), chi de cong thuc diem co mau so hop ly.
    private static final int DEFAULT_SCORING_RADIUS_KM = 20;
    private static final int TOP_K = 10;
    // Khi Poster bam "Xem them" (FE, expand=true) va danh sach mac dinh qua it/toan diem thap:
    // lay nhieu ung vien hon VA bo qua preferredRadiusKm tu khai cua Tasker (withinPreferredRadius),
    // dung y "mo rong pham vi tim kiem" nguoi dung yeu cau - khong doi cong thuc tinh diem, chi
    // noi long buoc loc cung o buoc 1.
    private static final int EXPANDED_TOP_K = 20;
    // Tran khoang cach tuyet doi (km) khi expand=true - bo preferredRadiusKm tu khai cua Tasker
    // KHONG co nghia la goi y ca nguoi cach hang tram km, van can 1 tran hop ly rieng cho truong
    // hop mo rong (yeu cau nguoi dung: gioi han 100km).
    private static final int EXPANDED_MAX_DISTANCE_KM = 100;

    private final TaskFacade taskFacade;
    private final UserFacade userFacade;
    private final AiFacade aiFacade;
    private final TaskEmbeddingCacheRepository taskEmbeddingCacheRepository;
    private final TaskerProfileEmbeddingRepository taskerProfileEmbeddingRepository;
    private final MatchingProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public TaskerMatchingService(TaskFacade taskFacade, UserFacade userFacade, AiFacade aiFacade,
            TaskEmbeddingCacheRepository taskEmbeddingCacheRepository,
            TaskerProfileEmbeddingRepository taskerProfileEmbeddingRepository, MatchingProperties properties,
            ObjectMapper objectMapper, Clock clock) {
        this.taskFacade = taskFacade;
        this.userFacade = userFacade;
        this.aiFacade = aiFacade;
        this.taskEmbeddingCacheRepository = taskEmbeddingCacheRepository;
        this.taskerProfileEmbeddingRepository = taskerProfileEmbeddingRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Chay du ca 2 buoc: loc cung (category qua UserFacade.findMatchCandidates + skill VERIFIED +
     * KYC VERIFIED + ban kinh) roi tinh structuredScore, lay top 10; roi rerank bang semanticScore
     * (embedding cache, tinh moi khi chua co). Tra ve toi da 10 phan tu, sap giam dan finalScore -
     * it hon 10 chi khi thuc su khong du ung vien qua loc cung.
     */
    @Transactional
    public List<RankedCandidate> rankCandidates(UUID taskId, UUID posterId) {
        return rankCandidates(taskId, posterId, false);
    }

    /**
     * @param expand true khi Poster bam "Xem them" tren FE (danh sach mac dinh qua it/diem thap) -
     *               bo qua preferredRadiusKm cua Tasker va lay toi da {@link #EXPANDED_TOP_K}
     *               thay vi {@link #TOP_K}.
     */
    @Transactional
    public List<RankedCandidate> rankCandidates(UUID taskId, UUID posterId, boolean expand) {
        TaskSummary task = taskFacade.findTask(taskId)
                .filter(t -> t.posterId().equals(posterId))
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        List<TaskerMatchCandidateSummary> candidates = userFacade.findMatchCandidates(task.categoryId());
        List<RankedCandidate> structured = candidates.stream()
                // Mot tai khoan co the mang ca 2 vai tro Poster/Tasker (xem 01-domain-glossary.md) -
                // khong duoc tu goi y chinh Poster dang goi cho viec cua ho, du ho co ho so Tasker
                // VERIFIED cung category. Loai truoc tien de khong tinh toan/embed thua cho truong hop nay.
                .filter(c -> !c.accountId().equals(posterId))
                .filter(c -> c.verificationStatus() == SkillVerificationStatus.VERIFIED)
                // Chua xac minh danh tinh (KYC) thi khong duoc goi y, ca danh sach mac dinh lan
                // "Xem them" (yeu cau nguoi dung) - KHAC voi verificationStatus o tren (do la xac
                // minh ho so ky nang theo tung category, khong phai danh tinh tai khoan).
                .filter(c -> c.kycStatus() == KycStatus.VERIFIED)
                .filter(c -> c.locationLat() != null && c.locationLng() != null)
                .map(c -> toStructured(task, c))
                .filter(c -> expand || withinPreferredRadius(c, candidates))
                // Tran 100km rieng cho expand - bo preferredRadiusKm khong co nghia la khong con
                // gioi han khoang cach nao (yeu cau nguoi dung).
                .filter(c -> !expand || c.distanceKm() <= EXPANDED_MAX_DISTANCE_KM)
                .sorted(Comparator.comparingDouble(RankedCandidate::structuredScore).reversed())
                .limit(expand ? EXPANDED_TOP_K : TOP_K)
                .toList();

        if (structured.isEmpty()) {
            return List.of();
        }

        String taskVector = getOrComputeTaskEmbedding(task);
        return structured.stream()
                .map(candidate -> applySemanticRerank(candidate, task, taskVector))
                .sorted(Comparator.comparingDouble(RankedCandidate::finalScore).reversed())
                .toList();
    }

    /** Kiem tra khoang cach nam trong preferredRadiusKm cua chinh candidate do - tach rieng de doc ro trong filter chain. */
    private boolean withinPreferredRadius(RankedCandidate candidate, List<TaskerMatchCandidateSummary> allCandidates) {
        return allCandidates.stream()
                .filter(c -> c.accountId().equals(candidate.accountId()))
                .findFirst()
                .map(c -> c.preferredRadiusKm() == null || candidate.distanceKm() <= c.preferredRadiusKm())
                .orElse(false);
    }

    /** Tinh structuredScore cho mot ung vien theo 4 tieu chi da chot trong plan (khoang cach/gia/kinh nghiem/lich ranh). */
    private RankedCandidate toStructured(TaskSummary task, TaskerMatchCandidateSummary candidate) {
        double distanceKm = GeoUtils.distanceKm(task.lat(), task.lng(), candidate.locationLat(), candidate.locationLng());
        int radiusForScoring = candidate.preferredRadiusKm() != null ? candidate.preferredRadiusKm() : DEFAULT_SCORING_RADIUS_KM;
        double distanceScore = Math.max(0.0, 1.0 - (distanceKm / Math.max(1, radiusForScoring)));

        double priceScore = priceScore(task.budgetAmount(), candidate.priceMin(), candidate.priceMax());
        double experienceScore = Math.min(1.0, candidate.yearsExperience() / (double) EXPERIENCE_SCORE_CAP_YEARS);
        Boolean availabilityMatches = availabilityMatches(task.scheduledAt(), candidate.availability());
        double availabilityScore = availabilityMatches == null ? 0.5 : (availabilityMatches ? 1.0 : 0.2);

        MatchingProperties.Weights w = properties.weights();
        double structuredScore = w.distance() * distanceScore + w.price() * priceScore
                + w.experience() * experienceScore + w.availability() * availabilityScore;

        return new RankedCandidate(candidate.accountId(), distanceKm, candidate.priceMin(), candidate.priceMax(),
                candidate.yearsExperience(), availabilityMatches, structuredScore, null, structuredScore);
    }

    /**
     * 1.0 neu budget nam trong [priceMin, priceMax] cua Tasker, giam dan neu lech ra ngoai.
     * Trung lap (0.5 - neutral) neu thieu du lieu mot phia (Task khong ghi budget hoac Tasker
     * chua khai gia) - khong the ket luan hop/khong hop, khong phat/thuong sai.
     */
    private double priceScore(Long budget, Long priceMin, Long priceMax) {
        if (budget == null || priceMin == null || priceMax == null) {
            return 0.5;
        }
        if (budget >= priceMin && budget <= priceMax) {
            return 1.0;
        }
        long distanceOutside = budget < priceMin ? (priceMin - budget) : (budget - priceMax);
        long range = Math.max(1, priceMax - priceMin);
        return Math.max(0.0, 1.0 - (distanceOutside / (double) range));
    }

    /**
     * true neu co khung gio ranh cua Tasker khop dung thu + nam trong [start,end) cua thoi
     * diem Task.scheduledAt (quy doi ve Asia/Ho_Chi_Minh); false neu Tasker co khai lich
     * nhung khong khop; null neu khong du du lieu de ket luan (Task khong co scheduledAt, hoac
     * Tasker chua khai bao khung gio nao).
     */
    private Boolean availabilityMatches(Instant scheduledAt, List<TaskerMatchCandidateSummary.AvailabilitySlot> slots) {
        if (scheduledAt == null || slots.isEmpty()) {
            return null;
        }
        LocalDateTime local = LocalDateTime.ofInstant(scheduledAt, APP_ZONE);
        // TaskerAvailability.dayOfWeek: 1 = Thu 2 ... 7 = Chu nhat (xem entity Javadoc);
        // DayOfWeek.getValue() cua java.time: 1 = Monday ... 7 = Sunday - trung nhau.
        int dayOfWeek = local.getDayOfWeek().getValue();
        LocalTime time = local.toLocalTime();
        return slots.stream().anyMatch(slot -> slot.dayOfWeek() == dayOfWeek
                && !time.isBefore(slot.startTime()) && time.isBefore(slot.endTime()));
    }

    /** Lay embedding Task tu cache, tinh moi qua AiFacade neu chua co - tra null neu AiFacade khong tra duoc (het quota/loi). */
    private String getOrComputeTaskEmbedding(TaskSummary task) {
        Optional<TaskEmbeddingCache> cached = taskEmbeddingCacheRepository.findById(task.id());
        if (cached.isPresent()) {
            return cached.get().getVector();
        }
        String text = (task.title() == null ? "" : task.title()) + ". " + (task.description() == null ? "" : task.description());
        Optional<float[]> vector = aiFacade.embed(text);
        if (vector.isEmpty()) {
            return null;
        }
        String json = toJson(vector.get());
        if (json == null) {
            return null;
        }
        taskEmbeddingCacheRepository.save(TaskEmbeddingCache.create(task.id(), json, "gemini-embedding-001", clock.instant()));
        return json;
    }

    /** Lay embedding ho so Tasker cho category nay tu cache, tinh moi neu chua co - tra null neu AiFacade khong tra duoc. */
    private String getOrComputeTaskerEmbedding(UUID accountId, UUID categoryId, String bio) {
        Optional<TaskerProfileEmbedding> cached = taskerProfileEmbeddingRepository.findByAccountIdAndCategoryId(accountId, categoryId);
        if (cached.isPresent()) {
            return cached.get().getVector();
        }
        if (bio == null || bio.isBlank()) {
            return null;
        }
        Optional<float[]> vector = aiFacade.embed(bio);
        if (vector.isEmpty()) {
            return null;
        }
        String json = toJson(vector.get());
        if (json == null) {
            return null;
        }
        taskerProfileEmbeddingRepository.save(TaskerProfileEmbedding.create(
                UUID.randomUUID(), accountId, categoryId, json, "gemini-embedding-001", clock.instant()));
        return json;
    }

    /** Tinh semanticScore (cosine) neu ca hai embedding san sang, roi tron vao finalScore theo trong so matching.weights.semantic. */
    private RankedCandidate applySemanticRerank(RankedCandidate candidate, TaskSummary task, String taskVectorJson) {
        if (taskVectorJson == null) {
            return candidate;
        }
        // Bio dung de embed lay tu chinh UserFacade.findMatchCandidates() da goi truoc do -
        // goi lai o day de tranh phai truyen ca danh sach candidate goc xuyen qua nhieu tang;
        // chap nhan mot lan doc them cho moi ung vien trong top 10 (toi da 10 lan/lan goi y).
        var profile = userFacade.findMatchCandidates(task.categoryId()).stream()
                .filter(c -> c.accountId().equals(candidate.accountId()))
                .findFirst();
        String taskerVectorJson = profile.map(p -> getOrComputeTaskerEmbedding(candidate.accountId(), task.categoryId(), p.bio()))
                .orElse(null);
        if (taskerVectorJson == null) {
            return candidate;
        }
        float[] taskVector = fromJson(taskVectorJson);
        float[] taskerVector = fromJson(taskerVectorJson);
        if (taskVector == null || taskerVector == null || taskVector.length != taskerVector.length) {
            return candidate;
        }
        double semanticScore = cosineSimilarity(taskVector, taskerVector);
        double semanticWeight = properties.weights().semantic();
        double finalScore = (1 - semanticWeight) * candidate.structuredScore() + semanticWeight * semanticScore;
        return new RankedCandidate(candidate.accountId(), candidate.distanceKm(), candidate.priceMin(),
                candidate.priceMax(), candidate.yearsExperience(), candidate.availabilityMatches(),
                candidate.structuredScore(), semanticScore, finalScore);
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

    private String toJson(float[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException ex) {
            log.warn("Khong serialize duoc embedding vector sang JSON: {}", ex.getMessage());
            return null;
        }
    }

    private float[] fromJson(String json) {
        try {
            return objectMapper.readValue(json, float[].class);
        } catch (JsonProcessingException ex) {
            log.warn("Khong deserialize duoc embedding vector tu JSON: {}", ex.getMessage());
            return null;
        }
    }
}
