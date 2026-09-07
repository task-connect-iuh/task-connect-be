package vn.taskconnect.task.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.taskconnect.common.response.ApiResponse;
import vn.taskconnect.security.jwt.AuthenticatedPrincipal;
import vn.taskconnect.task.dto.request.CreateTaskRequest;
import vn.taskconnect.task.dto.request.TaskImageUploadUrlRequest;
import vn.taskconnect.task.dto.response.TaskImageUploadUrlResponse;
import vn.taskconnect.task.dto.response.TaskResponse;
import vn.taskconnect.task.service.TaskImageUploadService;
import vn.taskconnect.task.service.TaskService;

/**
 * Endpoint dang viec (UC06 toi gian, dot 1): tao, xem danh sach cua chinh minh, xem chi
 * tiet. Sua/huy (UC07), theo doi lich su trang thai (UC08), va ung tuyen/xac nhan (UC10/UC11)
 * lam dot sau - xem docs/PROGRESS-TASK-POSTER-MODULE.md. Chua co endpoint duyet danh sach
 * cong khai cho Tasker (browse) o dot nay.
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskImageUploadService imageUploadService;

    public TaskController(TaskService taskService, TaskImageUploadService imageUploadService) {
        this.taskService = taskService;
        this.imageUploadService = imageUploadService;
    }

    /** Dang mot cong viec moi - chi tai khoan mang role TASK_POSTER goi duoc. */
    @PostMapping
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<TaskResponse> createTask(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.ok(taskService.createTask(principal.accountId(), request), "Đăng việc thành công.");
    }

    /** Danh sach cong viec da dang cua chinh Poster dang dang nhap. */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<List<TaskResponse>> getMyTasks(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.ok(taskService.getMyTasks(principal.accountId()));
    }

    /** Xem chi tiet mot cong viec - dot nay chi chu task xem duoc. */
    @GetMapping("/{taskId}")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<TaskResponse> getTask(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId) {
        return ApiResponse.ok(taskService.getTaskForOwner(principal.accountId(), taskId));
    }

    /**
     * Xin presigned URL de tu tai mot anh minh hoa cong viec len S3 truc tiep tu client,
     * khong qua backend. Client gom cac publicUrl tra ve vao imageUrls cua CreateTaskRequest
     * khi goi POST /tasks.
     */
    @PostMapping("/images-upload-url")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<TaskImageUploadUrlResponse> createImageUploadUrl(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody TaskImageUploadUrlRequest request) {
        return ApiResponse.ok(imageUploadService.createUploadUrl(principal.accountId(), request));
    }
}
