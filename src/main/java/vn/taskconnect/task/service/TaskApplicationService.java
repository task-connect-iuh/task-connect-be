package vn.taskconnect.task.service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.task.api.TaskApplicationStatus;
import vn.taskconnect.task.api.TaskStatus;
import vn.taskconnect.task.dto.request.ApplyToTaskRequest;
import vn.taskconnect.task.dto.response.MyApplicationResponse;
import vn.taskconnect.task.dto.response.TaskApplicationResponse;
import vn.taskconnect.task.dto.response.TaskFeedItemResponse;
import vn.taskconnect.task.entity.Task;
import vn.taskconnect.task.entity.TaskApplication;
import vn.taskconnect.task.entity.TaskImage;
import vn.taskconnect.task.repository.TaskApplicationRepository;
import vn.taskconnect.task.repository.TaskImageRepository;
import vn.taskconnect.task.repository.TaskRepository;
import vn.taskconnect.user.api.UserFacade;
import vn.taskconnect.user.api.dto.ServiceCategorySummary;
import vn.taskconnect.user.api.dto.UserProfileSummary;

/**
 * Nghiep vu tim/ung tuyen cong viec (UC10) va Poster xac nhan Tasker (UC11, gioi han doi
 * trang thai Task/Application - KHONG tao Booking that, module Booking/Payment chua ton tai,
 * xem docs/TASK-MODULE-SPLIT.md va GUARDRAIL 2 cua CLAUDE.md ve escrow). Muc don gian: browse
 * chi loc theo trang thai OPEN + danh muc + tu khoa, chua dung Redis Geo/ban kinh (OQ-02 con
 * MO), khong tra diem uy tin/khoang cach (xem TaskFeedItemResponse).
 */
@Service
public class TaskApplicationService {

    private final TaskRepository taskRepository;
    private final TaskImageRepository imageRepository;
    private final TaskApplicationRepository applicationRepository;
    private final UserFacade userFacade;
    private final Clock clock;

    public TaskApplicationService(TaskRepository taskRepository, TaskImageRepository imageRepository,
            TaskApplicationRepository applicationRepository, UserFacade userFacade, Clock clock) {
        this.taskRepository = taskRepository;
        this.imageRepository = imageRepository;
        this.applicationRepository = applicationRepository;
        this.userFacade = userFacade;
        this.clock = clock;
    }

    /**
     * Feed cong viec dang OPEN cho Tasker duyet, loc tuy chon theo danh muc va tu khoa (khop
     * khong phan biet hoa/thuong trong title). Khong sap theo khoang cach (xem Javadoc class).
     * Loai tru viec do chinh taskerId dang goi tu dang (1 tai khoan co the mang ca 2 vai tro
     * Poster/Tasker, xem 01-domain-glossary.md) - khong ai duoc thay/ung tuyen viec cua chinh
     * minh trong feed, dung ca cho apply() (CANNOT_APPLY_OWN_TASK).
     */
    @Transactional(readOnly = true)
    public List<TaskFeedItemResponse> browseOpenTasks(UUID taskerId, UUID categoryId, String keyword) {
        List<Task> tasks = categoryId != null
                ? taskRepository.findByStatusAndCategoryIdOrderByCreatedAtDesc(TaskStatus.OPEN, categoryId)
                : taskRepository.findByStatusOrderByCreatedAtDesc(TaskStatus.OPEN);
        tasks = tasks.stream().filter(task -> !task.getPosterId().equals(taskerId)).toList();
        if (keyword != null && !keyword.isBlank()) {
            String needle = keyword.trim().toLowerCase(Locale.ROOT);
            tasks = tasks.stream().filter(task -> task.getTitle().toLowerCase(Locale.ROOT).contains(needle)).toList();
        }
        if (tasks.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> categoryNameById = activeCategoryNameById();
        Map<UUID, List<String>> imageUrlsByTaskId = imageUrlsByTaskId(tasks.stream().map(Task::getId).toList());
        return tasks.stream().map(task -> toFeedItem(task, categoryNameById.get(task.getCategoryId()),
                imageUrlsByTaskId.getOrDefault(task.getId(), List.of()))).toList();
    }

    /**
     * Chi tiet 1 cong viec dang OPEN cho Tasker xem - dung khi vao thang URL /tim-viec/{id}.
     * Viec cua chinh taskerId dang goi coi nhu khong tim thay, dong bo voi viec bi loai khoi
     * feed (xem Javadoc browseOpenTasks).
     */
    @Transactional(readOnly = true)
    public TaskFeedItemResponse getOpenTaskForBrowse(UUID taskerId, UUID taskId) {
        Task task = taskRepository.findByIdAndStatus(taskId, TaskStatus.OPEN)
                .filter(candidate -> !candidate.getPosterId().equals(taskerId))
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        String categoryName = activeCategoryNameById().get(task.getCategoryId());
        List<String> imageUrls = imageRepository.findByTaskIdOrderByDisplayOrderAsc(taskId).stream()
                .map(TaskImage::getImageUrl).toList();
        return toFeedItem(task, categoryName, imageUrls);
    }

    /**
     * Tasker gui don ung tuyen. Chan: task khong ton tai/khong OPEN (TASK_NOT_OPEN), tu ung
     * tuyen viec cua chinh minh (CANNOT_APPLY_OWN_TASK), da ung tuyen roi (ALREADY_APPLIED -
     * kiem tra truoc de thong bao ro, du DB co UNIQUE constraint chan lai lan nua).
     */
    @Transactional
    public TaskApplicationResponse apply(UUID taskerId, UUID taskId, ApplyToTaskRequest request) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessException(ErrorCode.TASK_NOT_OPEN);
        }
        if (task.getPosterId().equals(taskerId)) {
            throw new BusinessException(ErrorCode.CANNOT_APPLY_OWN_TASK);
        }
        if (applicationRepository.findByTaskIdAndTaskerId(taskId, taskerId).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_APPLIED);
        }

        TaskApplication application = TaskApplication.submit(UUID.randomUUID(), taskId, taskerId,
                request.proposedArrivalText(), request.message(), clock.instant());
        try {
            applicationRepository.saveAndFlush(application);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ALREADY_APPLIED);
        }
        return toApplicationResponse(application);
    }

    /** Toan bo don ung tuyen (moi trang thai) cua chinh Tasker dang goi - dung cho man "Viec da nhan". */
    @Transactional(readOnly = true)
    public List<MyApplicationResponse> listMyApplications(UUID taskerId) {
        List<TaskApplication> applications = applicationRepository.findByTaskerIdOrderByCreatedAtDesc(taskerId);
        if (applications.isEmpty()) {
            return List.of();
        }
        List<UUID> taskIds = applications.stream().map(TaskApplication::getTaskId).distinct().toList();
        Map<UUID, Task> taskById = taskRepository.findAllById(taskIds).stream()
                .collect(Collectors.toMap(Task::getId, t -> t));
        Map<UUID, String> categoryNameById = activeCategoryNameById();
        Map<UUID, List<String>> imageUrlsByTaskId = imageUrlsByTaskId(taskIds);
        Map<UUID, String> posterNameById = new HashMap<>();
        return applications.stream()
                .filter(app -> taskById.containsKey(app.getTaskId()))
                .map(app -> {
                    Task task = taskById.get(app.getTaskId());
                    String posterName = posterNameById.computeIfAbsent(task.getPosterId(), this::resolveDisplayName);
                    return new MyApplicationResponse(app.getId(), app.getStatus(), app.getProposedArrivalText(),
                            app.getMessage(), app.getCreatedAt(), app.getRespondedAt(), task.getId(), task.getTitle(),
                            task.getAddressText(), task.getBudgetAmount(),
                            task.getScheduledAt(), task.getStatus(), task.getCategoryId(),
                            categoryNameById.get(task.getCategoryId()), posterName,
                            imageUrlsByTaskId.getOrDefault(task.getId(), List.of()));
                })
                .toList();
    }

    /** Poster xem danh sach ung vien cua 1 cong viec cua chinh minh - 404 kieu TASK_NOT_FOUND neu khong phai chu. */
    @Transactional(readOnly = true)
    public List<TaskApplicationResponse> listApplicantsForOwner(UUID posterId, UUID taskId) {
        requireOwnedTask(posterId, taskId);
        return applicationRepository.findByTaskId(taskId).stream().map(this::toApplicationResponse).toList();
    }

    /**
     * Poster xac nhan mot ung vien: don duoc chon chuyen ACCEPTED, Task chuyen ASSIGNED, moi
     * don PENDING con lai cua cung task chuyen NEEDS_RECONFIRM (OQ-08: 1 Tasker/task). Khong
     * tao Booking/escrow o day - xem Javadoc class.
     */
    @Transactional
    public TaskApplicationResponse confirm(UUID posterId, UUID taskId, UUID applicationId) {
        Task task = requireOwnedTask(posterId, taskId);
        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_ASSIGNED);
        }
        TaskApplication target = applicationRepository.findByIdAndTaskId(applicationId, taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        if (target.getStatus() != TaskApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_PENDING);
        }

        Instant now = clock.instant();
        target.accept(now);
        task.assignTo(now);
        for (TaskApplication other : applicationRepository.findByTaskId(taskId)) {
            if (!other.getId().equals(target.getId()) && other.getStatus() == TaskApplicationStatus.PENDING) {
                other.markNeedsReconfirm(now);
            }
        }
        return toApplicationResponse(target);
    }

    /** Poster tu choi mot ung vien - chuyen don do sang REJECTED, khong dong den cac don khac. */
    @Transactional
    public TaskApplicationResponse reject(UUID posterId, UUID taskId, UUID applicationId) {
        requireOwnedTask(posterId, taskId);
        TaskApplication target = applicationRepository.findByIdAndTaskId(applicationId, taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        if (target.getStatus() != TaskApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_PENDING);
        }
        target.reject(clock.instant());
        return toApplicationResponse(target);
    }

    /** Kiem tra task ton tai va thuoc ve dung Poster - gop 403/404 thanh TASK_NOT_FOUND, cung pattern voi TaskService.getTaskForOwner. */
    private Task requireOwnedTask(UUID posterId, UUID taskId) {
        return taskRepository.findById(taskId)
                .filter(task -> task.getPosterId().equals(posterId))
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
    }

    private TaskFeedItemResponse toFeedItem(Task task, String categoryName, List<String> imageUrls) {
        UserProfileSummary poster = userFacade.findProfile(task.getPosterId()).orElse(null);
        return new TaskFeedItemResponse(task.getId(), task.getCategoryId(), categoryName, task.getTitle(),
                task.getDescription(), task.getAddressText(), task.getLat(), task.getLng(), task.getBudgetAmount(),
                task.getScheduledAt(), imageUrls, task.getPosterId(), poster != null ? poster.fullName() : null,
                poster != null ? poster.avatarUrl() : null, task.getCreatedAt());
    }

    private TaskApplicationResponse toApplicationResponse(TaskApplication application) {
        String taskerName = resolveDisplayName(application.getTaskerId());
        String taskerAvatarUrl = userFacade.findProfile(application.getTaskerId()).map(UserProfileSummary::avatarUrl)
                .orElse(null);
        return new TaskApplicationResponse(application.getId(), application.getTaskerId(), taskerName,
                taskerAvatarUrl, application.getProposedArrivalText(), application.getMessage(),
                application.getStatus(), application.getCreatedAt(), application.getRespondedAt());
    }

    private String resolveDisplayName(UUID accountId) {
        return userFacade.findProfile(accountId).map(UserProfileSummary::fullName).orElse(null);
    }

    private Map<UUID, String> activeCategoryNameById() {
        return userFacade.listActiveServiceCategories().stream()
                .collect(Collectors.toMap(ServiceCategorySummary::id, ServiceCategorySummary::name));
    }

    private Map<UUID, List<String>> imageUrlsByTaskId(List<UUID> taskIds) {
        return imageRepository.findByTaskIdInOrderByDisplayOrderAsc(taskIds).stream()
                .collect(Collectors.groupingBy(TaskImage::getTaskId,
                        Collectors.mapping(TaskImage::getImageUrl, Collectors.toList())));
    }
}
