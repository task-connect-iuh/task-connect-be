package vn.taskconnect.task.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.ai.api.AiFacade;
import vn.taskconnect.ai.api.dto.CategoryClassificationRequest;
import vn.taskconnect.ai.api.dto.CategoryClassificationResult;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.task.api.TaskAiFlagReason;
import vn.taskconnect.task.api.TaskStatus;
import vn.taskconnect.task.dto.request.CreateTaskRequest;
import vn.taskconnect.task.dto.request.RejectTaskRequest;
import vn.taskconnect.task.dto.response.TaskResponse;
import vn.taskconnect.task.dto.response.TaskReviewSummaryResponse;
import vn.taskconnect.task.entity.Task;
import vn.taskconnect.task.entity.TaskImage;
import vn.taskconnect.task.repository.TaskImageRepository;
import vn.taskconnect.task.repository.TaskRepository;
import vn.taskconnect.user.api.UserFacade;
import vn.taskconnect.user.api.dto.ServiceCategorySummary;

/**
 * Nghiep vu dang viec (UC06 toi gian, dot 1): tao, xem danh sach cong viec cua chinh minh, va
 * xem chi tiet mot cong viec (chi chu task). Sua/huy (UC07), theo doi lich su trang thai
 * (UC08), va ung tuyen/xac nhan Tasker (UC10/UC11) chua lam dot nay - xem
 * docs/PROGRESS-TASK-POSTER-MODULE.md.
 */
@Service
public class TaskService {

    /** Toi da 5 anh minh hoa moi cong viec - kiem tra o day, khong o rang buoc DB (xem V18 migration). */
    private static final int MAX_IMAGES = 5;

    /**
     * Mac dinh khi request khong gui estimatedWorkersNeeded (FE dot nay khong con o nhap,
     * luon gui 1 - xem docs/PROGRESS-TASK-POSTER-MODULE.md). Chi de hien thi, khong co nghia
     * he thong, xem Javadoc Task.estimatedWorkersNeeded.
     */
    private static final int DEFAULT_ESTIMATED_WORKERS_NEEDED = 1;

    /**
     * Cac tieu chi SUSPICIOUS da chot voi nguoi dung (bo nhom "ne escrow" - da chan cung o
     * tang luong tien theo GUARDRAIL #2, khong can AI phat hien lai qua text). Ma (code) dung
     * de khop nguoc ket qua Groq tra ve sang TaskAiFlagReason, xem classifyAndFlag().
     */
    private static final List<CategoryClassificationRequest.SuspiciousCriterion> SUSPICIOUS_CRITERIA = List.of(
            new CategoryClassificationRequest.SuspiciousCriterion("UNSAFE",
                    "Yeu cau viec nguy hiem/trai phep ngoai pham vi dien-nuoc dan dung, vi du cau dien "
                            + "trom, dau noi luoi dien khong qua dang ky, pha khoa dong ho nuoc"),
            new CategoryClassificationRequest.SuspiciousCriterion("SPAM",
                    "Dau hieu spam/tai khoan rac: mo ta rong hoac lap tu khoa vo nghia, gia bat thuong "
                            + "(qua cao hoac 0 dong)"),
            new CategoryClassificationRequest.SuspiciousCriterion("HARASSMENT",
                    "Ngon tu phan cam/quay roi/phan biet doi xu, vi du cong kich ca nhan, phan biet vung "
                            + "mien hoac gioi tinh trong yeu cau"));

    private static final Map<String, TaskAiFlagReason> SUSPICIOUS_REASON_BY_CODE = Map.of(
            "UNSAFE", TaskAiFlagReason.SUSPICIOUS_UNSAFE,
            "SPAM", TaskAiFlagReason.SUSPICIOUS_SPAM,
            "HARASSMENT", TaskAiFlagReason.SUSPICIOUS_HARASSMENT);

    private final TaskRepository taskRepository;
    private final TaskImageRepository imageRepository;
    private final UserFacade userFacade;
    private final AiFacade aiFacade;
    private final Clock clock;

    public TaskService(TaskRepository taskRepository, TaskImageRepository imageRepository, UserFacade userFacade,
            AiFacade aiFacade, Clock clock) {
        this.taskRepository = taskRepository;
        this.imageRepository = imageRepository;
        this.userFacade = userFacade;
        this.aiFacade = aiFacade;
        this.clock = clock;
    }

    /**
     * Tao mot cong viec moi cho Poster dang goi. Validate categoryId ton tai va con active
     * qua UserFacade (khong duoc tu JOIN bang user_service_categories, xem
     * .claude/rules/00-architecture.md). lat/lng bat buoc kiem tra thu cong o day (khong
     * phai Bean Validation) de nem dung TSK-400-MISSING_LOCATION. Task tao xong chuyen thang
     * OPEN ngay (xem Task.createOpen(), quyet dinh da chot voi nguoi dung cho dot nay) - AI
     * phan loai category (classifyAndFlag()) chi gan co hau kiem, KHONG doi trang thai nay,
     * dung theo co che hau kiem hoan toan da chot (xem .claude/rules/15-ai-module.md).
     */
    @Transactional
    public TaskResponse createTask(UUID posterId, CreateTaskRequest request) {
        List<ServiceCategorySummary> categories = userFacade.listActiveServiceCategories();
        ServiceCategorySummary category = categories.stream()
                .filter(candidate -> candidate.id().equals(request.categoryId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        if (request.lat() == null || request.lng() == null) {
            throw new BusinessException(ErrorCode.MISSING_LOCATION);
        }
        List<String> imageUrls = request.imageUrls() == null ? List.of() : request.imageUrls();
        if (imageUrls.size() > MAX_IMAGES) {
            throw new BusinessException(ErrorCode.TOO_MANY_TASK_IMAGES);
        }

        Instant now = clock.instant();
        int estimatedWorkersNeeded = request.estimatedWorkersNeeded() != null
                ? request.estimatedWorkersNeeded() : DEFAULT_ESTIMATED_WORKERS_NEEDED;
        Task task = Task.createOpen(UUID.randomUUID(), posterId, request.categoryId(), request.title(),
                request.description(), request.addressText(), request.lat(), request.lng(), request.locationType(),
                request.arrivalNotes(), request.budgetAmount(), request.scheduledAt(), estimatedWorkersNeeded, now);
        classifyAndFlag(task, request.description(), categories);
        taskRepository.save(task);

        List<TaskImage> savedImages = saveImages(task.getId(), imageUrls);
        return TaskResponse.from(task, category.name(),
                savedImages.stream().map(TaskImage::getImageUrl).toList());
    }

    /** Danh sach cong viec da dang cua chinh Poster dang goi, moi dang gan day nhat truoc. */
    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(UUID posterId) {
        List<Task> tasks = taskRepository.findByPosterIdOrderByCreatedAtDesc(posterId);
        if (tasks.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> categoryNameById = activeCategoryNameById();
        Map<UUID, List<String>> imageUrlsByTaskId = imageRepository
                .findByTaskIdInOrderByDisplayOrderAsc(tasks.stream().map(Task::getId).toList()).stream()
                .collect(Collectors.groupingBy(TaskImage::getTaskId,
                        Collectors.mapping(TaskImage::getImageUrl, Collectors.toList())));
        return tasks.stream()
                .map(task -> TaskResponse.from(task, categoryNameById.get(task.getCategoryId()),
                        imageUrlsByTaskId.getOrDefault(task.getId(), List.of())))
                .toList();
    }

    /**
     * Xem chi tiet mot cong viec - dot nay chi chu task xem duoc (chua co man xem cong khai
     * cho Tasker, de dot UC10). Khong phai chu hoac khong ton tai deu nem cung
     * TSK-404-TASK_NOT_FOUND, khong phan biet 403/404 de tranh lo cong viec nao ton tai
     * thuoc ve tai khoan khac.
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskForOwner(UUID posterId, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .filter(candidate -> candidate.getPosterId().equals(posterId))
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        String categoryName = activeCategoryNameById().get(task.getCategoryId());
        List<String> imageUrls = imageRepository.findByTaskIdOrderByDisplayOrderAsc(taskId).stream()
                .map(TaskImage::getImageUrl).toList();
        return TaskResponse.from(task, categoryName, imageUrls);
    }

    /**
     * Chi Admin: hang doi cong viec dang can hau kiem (needs_admin_review = true, xem
     * .claude/rules/15-ai-module.md), moi nhat truoc, loc duoc theo ly do gan co. reasonFilter
     * null nghia la lay tat ca ly do.
     */
    @Transactional(readOnly = true)
    public Page<TaskReviewSummaryResponse> listFlaggedTasks(TaskAiFlagReason reasonFilter, Pageable pageable) {
        Page<Task> page = reasonFilter == null
                ? taskRepository.findByNeedsAdminReviewTrueOrderByCreatedAtDesc(pageable)
                : taskRepository.findByNeedsAdminReviewTrueAndAiFlagReasonOrderByCreatedAtDesc(reasonFilter, pageable);
        Map<UUID, String> categoryNameById = activeCategoryNameById();
        Map<UUID, List<String>> imageUrlsByTaskId = imageRepository
                .findByTaskIdInOrderByDisplayOrderAsc(page.map(Task::getId).toList()).stream()
                .collect(Collectors.groupingBy(TaskImage::getTaskId,
                        Collectors.mapping(TaskImage::getImageUrl, Collectors.toList())));
        return page.map(task -> TaskReviewSummaryResponse.from(task, categoryNameById.get(task.getCategoryId()),
                imageUrlsByTaskId.getOrDefault(task.getId(), List.of())));
    }

    /**
     * Chi Admin: xac nhan mot cong viec dang hau kiem la KHONG vi pham - go co, giu nguyen
     * status hien tai (task van OPEN tu luc dang, khong doi gi ngoai co hau kiem).
     */
    @Transactional
    public void resolveFlaggedTask(UUID taskId, UUID adminAccountId) {
        Task task = requireFlaggedTask(taskId);
        task.resolveReview(adminAccountId, clock.instant());
    }

    /**
     * Chi Admin: tu choi mot cong viec dang hau kiem, bat buoc kem ly do - chuyen status sang
     * REJECTED (nhanh thoat da co san trong state machine, xem .claude/rules/01-domain-glossary.md),
     * an khoi feed Tasker ngay (feed chi loc status OPEN). Chi cho phep khi con OPEN - da
     * ASSIGNED (co Tasker nhan) thi khong cho tu choi nua qua duong nay, tranh pha vo booking
     * dang co (ngoai pham vi xu ly cua tinh nang hau kiem nay).
     */
    @Transactional
    public void rejectFlaggedTask(UUID taskId, UUID adminAccountId, RejectTaskRequest request) {
        Task task = requireFlaggedTask(taskId);
        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessException(ErrorCode.TASK_NOT_OPEN_FOR_REJECTION);
        }
        task.rejectByAdmin(adminAccountId, request.rejectionReason(), clock.instant());
    }

    /** Tim cong viec dang can hau kiem theo id - nem TASK_NOT_FOUND neu khong ton tai, TASK_NOT_FLAGGED_FOR_REVIEW neu co da duoc xu ly truoc do. */
    private Task requireFlaggedTask(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        if (!task.isNeedsAdminReview()) {
            throw new BusinessException(ErrorCode.TASK_NOT_FLAGGED_FOR_REVIEW);
        }
        return task;
    }

    /**
     * Chuc nang kiem duyet luc submit (xem .claude/rules/15-ai-module.md): goi AI phan loai
     * tren mo ta CUOI CUNG Poster da chot, roi gan co hau kiem vao task neu can. Poster tu
     * chon danh muc hoan toan tu do - KHONG con so sanh voi danh muc AI de xuat (bo nhom
     * POSTER_OVERRIDE), AI chi con phat hien OTHER (ngoai 5 nhom dich vu) va SUSPICIOUS.
     */
    private void classifyAndFlag(Task task, String description, List<ServiceCategorySummary> categories) {
        Optional<CategoryClassificationResult> result = aiFacade.classifyTaskCategory(
                new CategoryClassificationRequest(description, toCandidates(categories), SUSPICIOUS_CRITERIA));

        if (result.isEmpty()) {
            task.applyAiClassification(true, TaskAiFlagReason.CLASSIFICATION_FAILED);
            return;
        }
        CategoryClassificationResult classification = result.get();
        switch (classification.outcome()) {
            case SUSPICIOUS -> task.applyAiClassification(true,
                    SUSPICIOUS_REASON_BY_CODE.getOrDefault(classification.suspiciousReason(),
                            TaskAiFlagReason.SUSPICIOUS_UNSAFE));
            case OTHER -> task.applyAiClassification(true, TaskAiFlagReason.OTHER_CATEGORY);
            case CATEGORY -> task.applyAiClassification(false, null);
        }
    }

    /** Ghep danh sach danh muc active thanh ung vien cho AI - contextText la kho tri thuc RAG (description+keywords). */
    private List<CategoryClassificationRequest.CandidateCategory> toCandidates(List<ServiceCategorySummary> categories) {
        return categories.stream()
                .map(category -> new CategoryClassificationRequest.CandidateCategory(category.code(),
                        category.name(), buildContextText(category)))
                .toList();
    }

    /** Ghep description + keywords cua mot danh muc thanh mot doan ngu canh cho AI, khong de null lot vao chuoi. */
    private String buildContextText(ServiceCategorySummary category) {
        String description = category.description() == null ? "" : category.description();
        String keywords = category.keywords() == null ? "" : category.keywords();
        return (description + " " + keywords).trim();
    }

    /** Tra ten danh muc theo id, dung de enrich TaskResponse ma khong tu JOIN bang cua module User. */
    private Map<UUID, String> activeCategoryNameById() {
        return userFacade.listActiveServiceCategories().stream()
                .collect(Collectors.toMap(ServiceCategorySummary::id, ServiceCategorySummary::name));
    }

    /** Luu danh sach anh minh hoa theo dung thu tu nguoi dung da chon (displayOrder = vi tri trong danh sach). */
    private List<TaskImage> saveImages(UUID taskId, List<String> imageUrls) {
        List<TaskImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(new TaskImage(UUID.randomUUID(), taskId, imageUrls.get(i), i));
        }
        return imageRepository.saveAll(images);
    }
}
