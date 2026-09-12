package vn.taskconnect.task.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.task.dto.request.CreateTaskRequest;
import vn.taskconnect.task.dto.response.TaskResponse;
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

    private final TaskRepository taskRepository;
    private final TaskImageRepository imageRepository;
    private final UserFacade userFacade;
    private final Clock clock;

    public TaskService(TaskRepository taskRepository, TaskImageRepository imageRepository, UserFacade userFacade,
            Clock clock) {
        this.taskRepository = taskRepository;
        this.imageRepository = imageRepository;
        this.userFacade = userFacade;
        this.clock = clock;
    }

    /**
     * Tao mot cong viec moi cho Poster dang goi. Validate categoryId ton tai va con active
     * qua UserFacade (khong duoc tu JOIN bang user_service_categories, xem
     * .claude/rules/00-architecture.md). lat/lng bat buoc kiem tra thu cong o day (khong
     * phai Bean Validation) de nem dung TSK-400-MISSING_LOCATION. Task tao xong chuyen thang
     * OPEN ngay (xem Task.createOpen(), quyet dinh da chot voi nguoi dung cho dot nay).
     */
    @Transactional
    public TaskResponse createTask(UUID posterId, CreateTaskRequest request) {
        ServiceCategorySummary category = requireActiveCategory(request.categoryId());
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

    /** Validate categoryId ton tai va con active - chi qua UserFacade, khong tu JOIN bang cua module User. */
    private ServiceCategorySummary requireActiveCategory(UUID categoryId) {
        return userFacade.listActiveServiceCategories().stream()
                .filter(category -> category.id().equals(categoryId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
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
