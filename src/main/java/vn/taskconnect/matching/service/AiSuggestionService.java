package vn.taskconnect.matching.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.ai.api.AiFacade;
import vn.taskconnect.ai.api.dto.SuggestionReasonRequest;
import vn.taskconnect.ai.api.dto.SuggestionReasonResult;
import vn.taskconnect.matching.dto.response.SuggestedTaskerResponse;
import vn.taskconnect.matching.infrastructure.MatchingProperties;
import vn.taskconnect.task.api.TaskFacade;
import vn.taskconnect.task.api.dto.TaskSummary;
import vn.taskconnect.user.api.UserFacade;
import vn.taskconnect.user.api.dto.UserProfileSummary;

/**
 * Buoc 3 (generation) cua pipeline goi y Tasker - xem plan da duyet "AI Tasker Suggestion".
 * Nhan danh sach da xep hang tu {@link TaskerMatchingService}, goi Groq (qua AiFacade) mot
 * lan duy nhat cho ca lo de sinh ly do/diem tru bang tieng Viet; neu Groq loi/het quota thi
 * TU DUNG TEMPLATE dung tren chinh cac facts da tinh o buoc 1 (khong chan response, khong
 * bao gio tra 500 vi ly do AI). Logic fallback template thuoc ve day (module Matching, biet
 * nghia nghiep vu cua distance/price/availability), khong thuoc module ai (module ai khong
 * biet Task/Tasker la gi - xem Javadoc AiFacade).
 *
 * <p>Ket qua duoc cache trong bo nho theo taskId trong mot khoang thoi gian ngan de cac lan
 * Poster mo lai trang trong cung phien khong dot them quota AI - ConcurrentHashMap don gian
 * la du cho quy mo do an (khong can Redis), mat cache khi app restart la chap nhan duoc.
 */
@Service
public class AiSuggestionService {

    // TTL cache ket qua goi y theo taskId - du ngan de khong tra du lieu qua cu khi Task/ung
    // vien thay doi, du dai de tranh dot quota AI khi Poster bam qua lai vao lai trang nhieu lan.
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    // Nguong kinh nghiem "chua co viec nao" dung de sinh concern template - Tasker moi verify,
    // chua co du lieu Booking/Review that (xem SuggestedTaskerResponse.completedJobsNearby).
    private static final int NEW_TASKER_EXPERIENCE_YEARS = 1;

    private final TaskerMatchingService taskerMatchingService;
    private final TaskFacade taskFacade;
    private final UserFacade userFacade;
    private final AiFacade aiFacade;
    private final MatchingProperties properties;
    private final Clock clock;

    private final Map<CacheKey, CachedEntry> cache = new ConcurrentHashMap<>();

    public AiSuggestionService(TaskerMatchingService taskerMatchingService, TaskFacade taskFacade,
            UserFacade userFacade, AiFacade aiFacade, MatchingProperties properties, Clock clock) {
        this.taskerMatchingService = taskerMatchingService;
        this.taskFacade = taskFacade;
        this.userFacade = userFacade;
        this.aiFacade = aiFacade;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Danh sach goi y day du (toi da 10, sap giam dan finalScore) cho Poster xem tren
     * GET /tasks/{taskId}/suggested-taskers. Dung cache trong TTL neu con hieu luc.
     */
    @Transactional
    public List<SuggestedTaskerResponse> getSuggestions(UUID taskId, UUID posterId) {
        return getSuggestions(taskId, posterId, false);
    }

    /**
     * @param expand true khi Poster bam "Xem them" tren FE - mo rong pham vi tim kiem (xem
     *               TaskerMatchingService.rankCandidates(taskId, posterId, expand)), cache
     *               rieng voi ket qua mac dinh (CacheKey gom ca expand).
     */
    @Transactional
    public List<SuggestedTaskerResponse> getSuggestions(UUID taskId, UUID posterId, boolean expand) {
        CacheKey key = new CacheKey(taskId, expand);
        CachedEntry cached = cache.get(key);
        if (cached != null && Duration.between(cached.computedAt(), clock.instant()).compareTo(CACHE_TTL) < 0) {
            return cached.responses();
        }
        List<SuggestedTaskerResponse> responses = compute(taskId, posterId, expand);
        cache.put(key, new CachedEntry(responses, clock.instant()));
        return responses;
    }

    /** Xoa cache goi y (ca 2 bien the mac dinh/expand) cua mot Task - goi khi co loi moi moi duoc tao cho task do (xem TaskerInviteService.create()). */
    void invalidateCache(UUID taskId) {
        cache.remove(new CacheKey(taskId, false));
        cache.remove(new CacheKey(taskId, true));
    }

    /** Tinh danh sach goi y tu dau: xep hang (buoc 1+2) roi sinh ly do (buoc 3, uu tien Groq, fallback template). */
    private List<SuggestedTaskerResponse> compute(UUID taskId, UUID posterId, boolean expand) {
        TaskSummary task = taskFacade.findTask(taskId).orElseThrow();
        List<RankedCandidate> ranked = taskerMatchingService.rankCandidates(taskId, posterId, expand);
        if (ranked.isEmpty()) {
            return List.of();
        }

        Map<UUID, Map<String, String>> factsByCandidate = new HashMap<>();
        for (RankedCandidate candidate : ranked) {
            factsByCandidate.put(candidate.accountId(), buildFacts(task, candidate));
        }

        SuggestionReasonRequest aiRequest = new SuggestionReasonRequest(buildContextDescription(task),
                ranked.stream()
                        .map(c -> new SuggestionReasonRequest.CandidateFacts(c.accountId().toString(),
                                factsByCandidate.get(c.accountId())))
                        .toList());
        List<SuggestionReasonResult> aiResults = aiFacade.generateSuggestionReasons(aiRequest);
        Map<String, SuggestionReasonResult> aiResultById = new HashMap<>();
        for (SuggestionReasonResult result : aiResults) {
            aiResultById.put(result.candidateId(), result);
        }

        List<SuggestedTaskerResponse> responses = new ArrayList<>();
        for (RankedCandidate candidate : ranked) {
            SuggestionReasonResult aiResult = aiResultById.get(candidate.accountId().toString());
            int confidence;
            List<String> reasons;
            List<String> concerns;
            if (aiResult != null) {
                confidence = aiResult.confidence();
                reasons = aiResult.reasons();
                concerns = aiResult.concerns();
            } else {
                confidence = (int) Math.round(candidate.finalScore() * 100);
                reasons = templateReasons(task, candidate);
                concerns = templateConcerns(task, candidate);
            }
            boolean lowConfidence = confidence < properties.lowConfidenceThreshold();
            UserProfileSummary profile = userFacade.findProfile(candidate.accountId()).orElse(null);
            responses.add(new SuggestedTaskerResponse(
                    candidate.accountId(),
                    profile != null ? profile.fullName() : null,
                    profile != null ? profile.avatarUrl() : null,
                    profile != null ? profile.kycStatus() : null,
                    confidence, reasons, concerns, lowConfidence,
                    candidate.distanceKm(), candidate.priceMin(), candidate.priceMax(), 0));
        }
        return responses;
    }

    /** Mo ta ngan gon boi canh Task de dua vao prompt Groq (tieu de + mo ta). */
    private String buildContextDescription(TaskSummary task) {
        return "Công việc: " + task.title() + ". Mô tả: " + task.description();
    }

    /** Facts tieng Viet de doc cho mot ung vien, dung ca cho prompt Groq lan template fallback. */
    private Map<String, String> buildFacts(TaskSummary task, RankedCandidate candidate) {
        Map<String, String> facts = new HashMap<>();
        facts.put("distance", String.format("%.1f km", candidate.distanceKm()));
        facts.put("priceFit", describePriceFit(task.budgetAmount(), candidate.priceMin(), candidate.priceMax()));
        facts.put("availability", describeAvailability(candidate.availabilityMatches()));
        facts.put("experience", describeExperience(candidate.yearsExperience()));
        return facts;
    }

    /**
     * Dien dat so nam kinh nghiem thanh cau tu nhien - dac biet tranh ghi may moc "0 nam kinh
     * nghiem" (doc ky cuc, Groq de bi lap lai nguyen van thanh "(0 nam)" trong cau tra loi) khi
     * Tasker chua co nam kinh nghiem nao.
     */
    private String describeExperience(int years) {
        if (years <= 0) {
            return "Mới vào nghề, chưa có nhiều kinh nghiệm thực tế";
        }
        if (years == 1) {
            return "Khoảng 1 năm kinh nghiệm";
        }
        return years + " năm kinh nghiệm";
    }

    private String describePriceFit(Long budget, Long priceMin, Long priceMax) {
        if (budget == null || priceMin == null || priceMax == null) {
            return "Chưa đủ thông tin để so sánh giá";
        }
        if (budget >= priceMin && budget <= priceMax) {
            return "Trong khoảng ngân sách bạn đề xuất";
        }
        return budget < priceMin ? "Cao hơn ngân sách bạn đề xuất" : "Thấp hơn ngân sách bạn đề xuất";
    }

    private String describeAvailability(Boolean matches) {
        if (matches == null) {
            return "Chưa rõ lịch rảnh so với thời gian bạn chọn";
        }
        return matches ? "Khớp lịch rảnh với thời gian bạn chọn" : "Không rảnh đúng thời gian bạn chọn";
    }

    /**
     * Diễn đạt khoảng cách thành câu tự nhiên theo từng mốc, chọn 1 trong 2 cách viết cùng mốc
     * (dựa vào tính chẵn/lẻ của accountId) để cùng một khoảng cách không luôn ra đúng 1 câu -
     * tránh việc đọc 10 gợi ý mà thấy lặp lại y hệt một khuôn câu (phản hồi trực tiếp từ người
     * dùng: cách viết trước đó "Khoảng cách X km, xa" bị lặp khuôn và còn thiếu dấu tiếng Việt).
     */
    private String describeDistance(double km, boolean variantA) {
        String kmText = String.format("%.1f km", km);
        if (km <= 3) {
            return variantA ? "Ở ngay gần bạn, chỉ " + kmText : "Rất gần, chỉ khoảng " + kmText;
        }
        if (km <= 8) {
            return variantA ? "Khoảng cách khá gần, chỉ " + kmText : "Không xa lắm, chừng " + kmText;
        }
        if (km <= 15) {
            return variantA ? "Hơi xa một chút, khoảng " + kmText : "Cách bạn tầm " + kmText + ", hơi xa";
        }
        if (km <= 25) {
            return variantA ? "Khá xa so với khu vực bạn cần, khoảng " + kmText
                    : "Cách khá xa, tầm " + kmText + " từ vị trí bạn chọn";
        }
        return variantA ? "Rất xa so với khu vực bạn cần, khoảng " + kmText
                : "Cách khá xa (khoảng " + kmText + "), có thể mất nhiều thời gian di chuyển hơn bình thường";
    }

    /** Ly do fallback dung template khi Groq loi/het quota - dung chinh cac facts da tinh, khong bia them. */
    private List<String> templateReasons(TaskSummary task, RankedCandidate candidate) {
        List<String> reasons = new ArrayList<>();
        boolean variantA = candidate.accountId().hashCode() % 2 == 0;
        if (candidate.distanceKm() <= 8) {
            reasons.add(describeDistance(candidate.distanceKm(), variantA));
        }
        if (candidate.priceMin() != null && candidate.priceMax() != null && task.budgetAmount() != null
                && task.budgetAmount() >= candidate.priceMin() && task.budgetAmount() <= candidate.priceMax()) {
            reasons.add(variantA ? "Mức giá đúng với ngân sách bạn đưa ra" : "Giá đề xuất nằm trong khoảng bạn dự tính");
        }
        if (Boolean.TRUE.equals(candidate.availabilityMatches())) {
            reasons.add(variantA ? "Đúng khung giờ rảnh bạn chọn" : "Lịch rảnh khớp với thời gian bạn cần");
        }
        if (candidate.yearsExperience() >= NEW_TASKER_EXPERIENCE_YEARS) {
            reasons.add("Có " + candidate.yearsExperience() + " năm kinh nghiệm trong lĩnh vực này");
        }
        if (reasons.isEmpty()) {
            reasons.add("Đã xác minh kỹ năng phù hợp với công việc này");
        }
        return reasons;
    }

    /** Diem tru fallback dung template khi Groq loi/het quota - dung chinh cac facts da tinh, khong bia them. */
    private List<String> templateConcerns(TaskSummary task, RankedCandidate candidate) {
        List<String> concerns = new ArrayList<>();
        boolean variantA = candidate.accountId().hashCode() % 2 == 0;
        if (candidate.distanceKm() > 8) {
            concerns.add(describeDistance(candidate.distanceKm(), variantA));
        }
        if (Boolean.FALSE.equals(candidate.availabilityMatches())) {
            concerns.add(variantA ? "Không rảnh đúng khung giờ bạn chọn, cần hỏi lại"
                    : "Lịch rảnh hiện tại chưa khớp thời gian bạn cần, nên trao đổi thêm");
        }
        if (candidate.yearsExperience() < NEW_TASKER_EXPERIENCE_YEARS) {
            concerns.add(variantA ? "Mới xác minh gần đây, chưa có nhiều kinh nghiệm trong lĩnh vực này"
                    : "Còn khá mới trong nghề, chưa tích luỹ nhiều kinh nghiệm thực tế");
        }
        return concerns;
    }

    /** Mot ban ghi cache: danh sach goi y da tinh + thoi diem tinh, dung de kiem tra TTL. */
    private record CachedEntry(List<SuggestedTaskerResponse> responses, Instant computedAt) {
    }

    /** Khoa cache: 1 Task co the co 2 ket qua cache doc lap (mac dinh va sau khi "Xem them"). */
    private record CacheKey(UUID taskId, boolean expand) {
    }
}
