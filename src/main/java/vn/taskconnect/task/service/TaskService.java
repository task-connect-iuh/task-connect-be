package vn.taskconnect.task.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.ai.api.AiFacade;
import vn.taskconnect.ai.api.dto.CategoryClassificationRequest;
import vn.taskconnect.ai.api.dto.CategoryClassificationResult;
import vn.taskconnect.booking.api.BookingFacade;
import vn.taskconnect.booking.api.dto.BookingSummary;
import vn.taskconnect.chat.api.ChatFacade;
import vn.taskconnect.chat.api.ChatSystemMessages;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.task.api.TaskApplicationStatus;
import vn.taskconnect.task.api.TaskAiFlagReason;
import vn.taskconnect.task.api.TaskEditableField;
import vn.taskconnect.task.api.TaskStatus;
import vn.taskconnect.task.dto.request.CreateTaskRequest;
import vn.taskconnect.task.dto.request.RejectTaskRequest;
import vn.taskconnect.task.dto.request.UpdateTaskRequest;
import vn.taskconnect.task.dto.response.TaskResponse;
import vn.taskconnect.task.dto.response.TaskReviewSummaryResponse;
import vn.taskconnect.task.entity.Task;
import vn.taskconnect.task.entity.TaskApplication;
import vn.taskconnect.task.entity.TaskEditChange;
import vn.taskconnect.task.entity.TaskEditEvent;
import vn.taskconnect.task.entity.TaskImage;
import vn.taskconnect.task.repository.TaskApplicationRepository;
import vn.taskconnect.task.repository.TaskEditChangeRepository;
import vn.taskconnect.task.repository.TaskEditEventRepository;
import vn.taskconnect.task.repository.TaskImageRepository;
import vn.taskconnect.task.repository.TaskRepository;
import vn.taskconnect.user.api.UserFacade;
import vn.taskconnect.user.api.dto.ServiceCategorySummary;

/**
 * Nghiep vu dang viec (UC06 toi gian, dot 1): tao, xem danh sach cong viec cua chinh minh, xem
 * chi tiet mot cong viec (chi chu task), va tu UC07 sua/huy khi con OPEN/PENDING_REVIEW (xem
 * cancelTask()/updateTask()). Theo doi lich su trang thai (UC08) va ung tuyen/xac nhan Tasker
 * (UC10/UC11) o TaskApplicationService - xem docs/PROGRESS-TASK-POSTER-MODULE.md.
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

    /**
     * Hai trang thai duy nhat Poster con tu sua/huy duoc (UC07). Tu ASSIGNED tro di: huy qua
     * UC14 (dong thuan hai ben, ngoai pham vi UC07), doi lich qua RESCHEDULE_PROPOSAL (module
     * Chat) - khong dung duong updateTask()/cancelTask() nay nua.
     */
    private static final Set<TaskStatus> POSTER_EDITABLE_STATUSES = EnumSet.of(TaskStatus.OPEN,
            TaskStatus.PENDING_REVIEW);

    /**
     * Trang thai don con "song", dung cho: cascade sang CANCELLED khi Poster huy ca cong viec
     * (UC07) - dong bo voi isStillActiveForCascade() cua UC11 (TaskApplicationService). Cong them
     * TIME_CHANGED_NEEDS_RECONFIRM (don cung dang cho phan hoi, khong khac gi PENDING ve ban chat
     * "chua ket thuc").
     */
    private static final Set<TaskApplicationStatus> ACTIVE_STATUSES_ON_TASK_CANCEL = EnumSet.of(
            TaskApplicationStatus.PENDING, TaskApplicationStatus.INQUIRING, TaskApplicationStatus.INVITED,
            TaskApplicationStatus.TIME_CHANGED_NEEDS_RECONFIRM);

    /**
     * Don dang "cho quyet dinh that su" cua Poster - PENDING hoac TIME_CHANGED_NEEDS_RECONFIRM
     * (van la 1 don PENDING ve ban chat, chi dang tam gan co "cho Tasker xac nhan lai gio" - khac
     * INQUIRING/INVITED la loi hoi truoc/moi truoc, CHUA phai don ung tuyen chinh thuc). Dung khoa
     * FULL_LOCK_FIELDS - yeu cau nguoi dung 2026-09-30 (vong 2, sau khi phat hien khoa ca vat tu
     * boi INQUIRING lam mat luon SYSTEM message bao Poster sua viec cho kenh INQUIRING/INVITED).
     */
    private static final Set<TaskApplicationStatus> PENDING_LIKE_STATUSES = EnumSet.of(
            TaskApplicationStatus.PENDING, TaskApplicationStatus.TIME_CHANGED_NEEDS_RECONFIRM);

    /**
     * Vat tu (Tang 2) CHI khoa khi co don PENDING_LIKE_STATUSES - INQUIRING/INVITED khong khoa
     * (van sua duoc, van con SYSTEM message "Poster vừa cập nhật..." cho 2 trang thai nay).
     */
    private static final Set<TaskEditableField> FULL_LOCK_FIELDS = EnumSet.of(TaskEditableField.SUPPLIES_STATUS,
            TaskEditableField.SUPPLIES_NOTE);

    /**
     * Ngan sach (Tang 2) khoa RONG hon vat tu - CA ACTIVE_STATUSES_ON_TASK_CANCEL (tinh them
     * INQUIRING/INVITED), vi nhay cam hon: doi gia luc dang co nguoi hoi/duoc moi de dua ra quyet
     * dinh la khong cong bang cho ho (yeu cau nguoi dung 2026-09-30, vong 1).
     */
    private static final Set<TaskEditableField> BROAD_LOCK_FIELDS = EnumSet.of(TaskEditableField.BUDGET_AMOUNT);

    private final TaskRepository taskRepository;
    private final TaskImageRepository imageRepository;
    private final TaskApplicationRepository applicationRepository;
    private final TaskEditEventRepository editEventRepository;
    private final TaskEditChangeRepository editChangeRepository;
    private final UserFacade userFacade;
    private final AiFacade aiFacade;
    private final ChatFacade chatFacade;
    private final BookingFacade bookingFacade;
    private final Clock clock;

    public TaskService(TaskRepository taskRepository, TaskImageRepository imageRepository,
            TaskApplicationRepository applicationRepository, TaskEditEventRepository editEventRepository,
            TaskEditChangeRepository editChangeRepository, UserFacade userFacade, AiFacade aiFacade,
            ChatFacade chatFacade, BookingFacade bookingFacade, Clock clock) {
        this.taskRepository = taskRepository;
        this.imageRepository = imageRepository;
        this.applicationRepository = applicationRepository;
        this.editEventRepository = editEventRepository;
        this.editChangeRepository = editChangeRepository;
        this.userFacade = userFacade;
        this.aiFacade = aiFacade;
        this.chatFacade = chatFacade;
        this.bookingFacade = bookingFacade;
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
                request.arrivalNotes(), request.suppliesStatus(), request.suppliesNote(), request.budgetAmount(),
                request.scheduledAt(), estimatedWorkersNeeded, now);
        classifyAndFlag(task, request.description(), categories);
        taskRepository.save(task);

        List<TaskImage> savedImages = saveImages(task.getId(), imageUrls);
        return TaskResponse.from(task, category.name(),
                savedImages.stream().map(TaskImage::getImageUrl).toList(), 0, false, false, null);
    }

    /** Danh sach cong viec da dang cua chinh Poster dang goi, moi dang gan day nhat truoc. */
    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(UUID posterId) {
        List<Task> tasks = taskRepository.findByPosterIdOrderByCreatedAtDesc(posterId);
        if (tasks.isEmpty()) {
            return List.of();
        }
        List<UUID> taskIds = tasks.stream().map(Task::getId).toList();
        Map<UUID, String> categoryNameById = activeCategoryNameById();
        Map<UUID, List<String>> imageUrlsByTaskId = imageRepository
                .findByTaskIdInOrderByDisplayOrderAsc(taskIds).stream()
                .collect(Collectors.groupingBy(TaskImage::getTaskId,
                        Collectors.mapping(TaskImage::getImageUrl, Collectors.toList())));
        Map<UUID, Long> pendingCountByTaskId = applicationRepository
                .findByTaskIdInAndStatus(taskIds, TaskApplicationStatus.PENDING).stream()
                .collect(Collectors.groupingBy(TaskApplication::getTaskId, Collectors.counting()));
        // budgetLocked hang loat - taskId nao co it nhat 1 don thuoc ACTIVE_STATUSES_ON_TASK_CANCEL
        // (RONG hon pendingCountByTaskId o tren, tinh ca INQUIRING/INVITED/TIME_CHANGED_NEEDS_RECONFIRM).
        Set<UUID> budgetLockedTaskIds = applicationRepository
                .findByTaskIdInAndStatusIn(taskIds, ACTIVE_STATUSES_ON_TASK_CANCEL).stream()
                .map(TaskApplication::getTaskId)
                .collect(Collectors.toSet());
        // suppliesLocked hang loat - HEP hon budgetLockedTaskIds, chi tinh PENDING_LIKE_STATUSES
        // (khong tinh INQUIRING/INVITED, xem Javadoc TaskResponse.suppliesLocked).
        Set<UUID> suppliesLockedTaskIds = applicationRepository
                .findByTaskIdInAndStatusIn(taskIds, PENDING_LIKE_STATUSES).stream()
                .map(TaskApplication::getTaskId)
                .collect(Collectors.toSet());
        return tasks.stream()
                .map(task -> TaskResponse.from(task, categoryNameById.get(task.getCategoryId()),
                        imageUrlsByTaskId.getOrDefault(task.getId(), List.of()),
                        pendingCountByTaskId.getOrDefault(task.getId(), 0L).intValue(),
                        budgetLockedTaskIds.contains(task.getId()),
                        suppliesLockedTaskIds.contains(task.getId()), resolveWinningApplicationId(task)))
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
        Task task = requireOwnedTask(posterId, taskId);
        return buildTaskResponse(task);
    }

    /**
     * Poster tu huy cong viec cua chinh minh (UC07) - tu do hoan toan khi con OPEN/PENDING_REVIEW,
     * khong phan biet co ung vien hay khong, khong phan biet PENDING/INQUIRING/INVITED. Tu
     * ASSIGNED tro di 409 CANNOT_CANCEL_ASSIGNED (huy phai qua UC14, dong thuan hai ben, ngoai
     * pham vi nay). Cascade dung nguyen dang TaskService.rejectFlaggedTask(): duyet toan bo don
     * cua task, don nao con "song" (ACTIVE_STATUSES_ON_TASK_CANCEL) chuyen CANCELLED, VA vo dieu
     * kien dong kenh chat neu co (closeChannelIfExists la no-op an toan cho don khong/da dong
     * kenh - khong can loc truoc theo status).
     */
    @Transactional
    public void cancelTask(UUID posterId, UUID taskId) {
        Task task = requireOwnedTask(posterId, taskId);
        if (!POSTER_EDITABLE_STATUSES.contains(task.getStatus())) {
            throw new BusinessException(ErrorCode.CANNOT_CANCEL_ASSIGNED);
        }
        Instant now = clock.instant();
        task.cancelByPoster(now);
        for (TaskApplication application : applicationRepository.findByTaskId(taskId)) {
            if (ACTIVE_STATUSES_ON_TASK_CANCEL.contains(application.getStatus())) {
                application.cancelBecauseTaskCancelled(now);
            }
            chatFacade.closeChannelIfExists(application.getId(), ChatSystemMessages.TASK_CANCELLED_BY_POSTER);
        }
    }

    /**
     * Poster sua mot cong viec da dang (UC07) - chi 6 truong trong UpdateTaskRequest, GHI DE
     * TOAN BO (null = xoa gia tri). title/description/anh/addressText/lat/lng/categoryId/
     * estimatedWorkersNeeded KHOA CUNG VINH VIEN, khong nam trong request nay nen khong bao gio
     * bi dong (giu nguyen rule UC07 goc).
     *
     * <p>Thu tu kiem tra: (1) task phai con OPEN/PENDING_REVIEW, khac 409 CANNOT_CANCEL_ASSIGNED
     * (huy/sua sau ASSIGNED di qua UC14/RESCHEDULE_PROPOSAL, khong qua duong nay); (2) tinh diff
     * THAT SU (so gia tri moi voi gia tri hien tai, KHONG dua vao viec field co mat trong request
     * hay khong - client luon gui du 6 truong ke ca truong dang bi disable tren form); (3) khoa
     * Tang 2 theo 2 muc (yeu cau nguoi dung 2026-09-30, vong 2): BROAD_LOCK_FIELDS (ngan sach) bi
     * khoa khi co BAT KY don nao thuoc ACTIVE_STATUSES_ON_TASK_CANCEL (tinh ca INQUIRING/INVITED -
     * ngan sach nhay cam hon, khoa som hon); FULL_LOCK_FIELDS (vat tu) CHI bi khoa khi co don
     * thuoc PENDING_LIKE_STATUSES (PENDING/TIME_CHANGED_NEEDS_RECONFIRM - INQUIRING/INVITED khong
     * khoa vat tu, van sua duoc). Ca 2 truy van SONG NGAY trong transaction nay, khong dung co luu
     * san. Field nao bi khoa (theo dung nhom cua no) VA co trong diff -> 409
     * FIELD_LOCKED_HAS_APPLICANTS kem danh sach field bi khoa trong details, KHONG luu gi ca (toan
     * bo request bi tu choi, ke ca cac truong khong bi khoa - dung y "tat ca hoac khong gi" de FE
     * xu ly don gian: submit lai sau khi da reset dung field bi khoa); (4) neu diff rong (khong
     * field nao thuc su doi) -> tra ve nguyen trang, KHONG tao su kien sua/khong doi status don
     * nao (double-submit vo hai); (5) ghi de Task, luu 1 TaskEditEvent + cac TaskEditChange tuong
     * ung diff; (6) duyet toan bo don cua task: don PENDING ma SCHEDULED_AT vua doi -> chuyen
     * TIME_CHANGED_NEEDS_RECONFIRM kem SYSTEM message rieng (Tier 3, doc lap voi khoa Tang 2 o
     * buoc 3 - SCHEDULED_AT khong thuoc BROAD_LOCK_FIELDS/FULL_LOCK_FIELDS); don INQUIRING/INVITED
     * ma SCHEDULED_AT hoac field FULL_LOCK_FIELDS (vat tu) vua doi that su -> SYSTEM message
     * "Poster vừa cập nhật..." kem ref_task_edit_id (budgetAmount KHONG bao gio roi vao nhanh nay
     * vi da bi BROAD_LOCK_FIELDS chan tu buoc 3 khi con INQUIRING/INVITED).
     */
    @Transactional
    public TaskResponse updateTask(UUID posterId, UUID taskId, UpdateTaskRequest request) {
        Task task = requireOwnedTask(posterId, taskId);
        if (!POSTER_EDITABLE_STATUSES.contains(task.getStatus())) {
            throw new BusinessException(ErrorCode.CANNOT_CANCEL_ASSIGNED,
                    "Công việc đã được giao, không thể tự sửa thông tin. Hãy trao đổi với Tasker trong hội thoại.");
        }

        List<TaskEditChangeDraft> diff = computeDiff(task, request);
        long activeApplicantCount = applicationRepository.countByTaskIdAndStatusIn(taskId, ACTIVE_STATUSES_ON_TASK_CANCEL);
        long pendingLikeCount = applicationRepository.countByTaskIdAndStatusIn(taskId, PENDING_LIKE_STATUSES);
        List<TaskEditableField> lockedFields = diff.stream().map(TaskEditChangeDraft::field)
                .filter(field -> (activeApplicantCount > 0 && BROAD_LOCK_FIELDS.contains(field))
                        || (pendingLikeCount > 0 && FULL_LOCK_FIELDS.contains(field)))
                .toList();
        if (!lockedFields.isEmpty()) {
            throw new BusinessException(ErrorCode.FIELD_LOCKED_HAS_APPLICANTS,
                    ErrorCode.FIELD_LOCKED_HAS_APPLICANTS.defaultMessage(),
                    Map.of("lockedFields", lockedFields.stream().map(Enum::name).toList()));
        }
        if (diff.isEmpty()) {
            return buildTaskResponse(task);
        }

        boolean scheduledAtChanged = diff.stream().anyMatch(c -> c.field() == TaskEditableField.SCHEDULED_AT);
        boolean suppliesChanged = diff.stream().anyMatch(c -> FULL_LOCK_FIELDS.contains(c.field()));

        Instant now = clock.instant();
        task.updateEditableFields(request.locationType(), request.arrivalNotes(), request.suppliesStatus(),
                request.suppliesNote(), request.budgetAmount(), request.scheduledAt(), now);

        TaskEditEvent event = editEventRepository.save(TaskEditEvent.of(UUID.randomUUID(), taskId, posterId, now));
        List<TaskEditChange> changeRows = new ArrayList<>();
        int sortOrder = 0;
        for (TaskEditChangeDraft change : diff) {
            changeRows.add(TaskEditChange.of(UUID.randomUUID(), event.getId(), change.field(), change.oldValue(),
                    change.newValue(), sortOrder++));
        }
        editChangeRepository.saveAll(changeRows);

        if (scheduledAtChanged || suppliesChanged) {
            for (TaskApplication application : applicationRepository.findByTaskId(taskId)) {
                TaskApplicationStatus status = application.getStatus();
                if (scheduledAtChanged && status == TaskApplicationStatus.PENDING) {
                    application.markTimeChangedNeedsReconfirm(now);
                    chatFacade.postTaskEditSystemMessageIfOpen(application.getId(),
                            ChatSystemMessages.TASK_TIME_CHANGED_NEEDS_RECONFIRM, event.getId());
                } else if ((suppliesChanged || scheduledAtChanged)
                        && (status == TaskApplicationStatus.INQUIRING || status == TaskApplicationStatus.INVITED)) {
                    chatFacade.postTaskEditSystemMessageIfOpen(application.getId(),
                            ChatSystemMessages.TASK_UPDATED_BY_POSTER, event.getId());
                }
            }
        }
        return buildTaskResponse(task);
    }

    /** Kiem tra task ton tai va thuoc ve dung Poster - gop 403/404 thanh TASK_NOT_FOUND. */
    private Task requireOwnedTask(UUID posterId, UUID taskId) {
        return taskRepository.findById(taskId)
                .filter(candidate -> candidate.getPosterId().equals(posterId))
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
    }

    /** Ghep TaskResponse tu 1 Task da co san trong transaction - dung sau createTask()/getTaskForOwner()/updateTask(). */
    private TaskResponse buildTaskResponse(Task task) {
        String categoryName = activeCategoryNameById().get(task.getCategoryId());
        List<String> imageUrls = imageRepository.findByTaskIdOrderByDisplayOrderAsc(task.getId()).stream()
                .map(TaskImage::getImageUrl).toList();
        int pendingApplicantCount = (int) applicationRepository.countByTaskIdAndStatus(task.getId(),
                TaskApplicationStatus.PENDING);
        boolean budgetLocked = applicationRepository.countByTaskIdAndStatusIn(task.getId(),
                ACTIVE_STATUSES_ON_TASK_CANCEL) > 0;
        boolean suppliesLocked = applicationRepository.countByTaskIdAndStatusIn(task.getId(),
                PENDING_LIKE_STATUSES) > 0;
        return TaskResponse.from(task, categoryName, imageUrls, pendingApplicantCount, budgetLocked, suppliesLocked,
                resolveWinningApplicationId(task));
    }

    /**
     * Id application da duoc chon o UC11 cho task nay (2026-10-02) - CHI goi BookingFacade khi
     * task dang ASSIGNED (truong hop OPEN pho bien hon nhieu khong co booking nao, tranh goi
     * thua). Null neu task chua ASSIGNED hoac (khong nen xay ra) ASSIGNED ma chua co booking.
     */
    private UUID resolveWinningApplicationId(Task task) {
        if (task.getStatus() != TaskStatus.ASSIGNED) {
            return null;
        }
        return bookingFacade.findByTaskId(task.getId()).map(BookingSummary::applicationId).orElse(null);
    }

    /**
     * So gia tri hien tai cua Task voi gia tri moi trong UpdateTaskRequest cho toan bo 6 truong
     * sua duoc - CHI field nao THAT SU doi gia tri (sau khi chuoi rong da quy ve null) moi co mat
     * trong danh sach tra ve, dung thu tu khai bao TaskEditableField. Day la co so DUY NHAT de
     * quyet dinh field nao bi khoa (Tang 2 + dang co PENDING) va field nao can luu lai lich su -
     * KHONG dua vao viec request co gui field do hay khong (client luon gui du 6 truong).
     */
    private List<TaskEditChangeDraft> computeDiff(Task task, UpdateTaskRequest request) {
        List<TaskEditChangeDraft> diff = new ArrayList<>();
        addIfChanged(diff, TaskEditableField.LOCATION_TYPE, enumToRaw(task.getLocationType()),
                enumToRaw(request.locationType()));
        addIfChanged(diff, TaskEditableField.ARRIVAL_NOTES, blankToNull(task.getArrivalNotes()),
                blankToNull(request.arrivalNotes()));
        addIfChanged(diff, TaskEditableField.BUDGET_AMOUNT, longToRaw(task.getBudgetAmount()),
                longToRaw(request.budgetAmount()));
        addIfChanged(diff, TaskEditableField.SUPPLIES_STATUS, enumToRaw(task.getSuppliesStatus()),
                enumToRaw(request.suppliesStatus()));
        addIfChanged(diff, TaskEditableField.SUPPLIES_NOTE, blankToNull(task.getSuppliesNote()),
                blankToNull(request.suppliesNote()));
        addIfChanged(diff, TaskEditableField.SCHEDULED_AT, instantToRaw(task.getScheduledAt()),
                instantToRaw(request.scheduledAt()));
        return diff;
    }

    private static void addIfChanged(List<TaskEditChangeDraft> diff, TaskEditableField field, String oldValue,
            String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            diff.add(new TaskEditChangeDraft(field, oldValue, newValue));
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String enumToRaw(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static String longToRaw(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String instantToRaw(Instant value) {
        return value == null ? null : value.toString();
    }

    /** 1 field da doi gia tri, tinh truoc khi ghi de Task - gia tri THO, dung de quyet dinh khoa va luu TaskEditChange. */
    private record TaskEditChangeDraft(TaskEditableField field, String oldValue, String newValue) {
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
     * dang co (ngoai pham vi xu ly cua tinh nang hau kiem nay). Vi task con OPEN nen cac don
     * ung tuyen (neu co) chi co the dang PENDING/INQUIRING/INVITED - dong TAT CA kenh chat con
     * OPEN cua cac don nay kem 1 SYSTEM message bao ly do (theo yeu cau nguoi dung: Poster/Tasker
     * khong duoc tiep tuc nhan tin cho 1 cong viec da bi Admin tu choi). closeChannelIfExists()
     * la no-op an toan cho don nao chua tung co kenh hoac kenh da CLOSED tu truoc, nen khong can
     * loc truoc theo status - duyet thang toan bo don cua task.
     */
    @Transactional
    public void rejectFlaggedTask(UUID taskId, UUID adminAccountId, RejectTaskRequest request) {
        Task task = requireFlaggedTask(taskId);
        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessException(ErrorCode.TASK_NOT_OPEN_FOR_REJECTION);
        }
        task.rejectByAdmin(adminAccountId, request.rejectionReason(), clock.instant());
        for (TaskApplication application : applicationRepository.findByTaskId(taskId)) {
            chatFacade.closeChannelIfExists(application.getId(), ChatSystemMessages.TASK_REJECTED_BY_ADMIN);
        }
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
