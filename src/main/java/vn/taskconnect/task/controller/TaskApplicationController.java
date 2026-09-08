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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.taskconnect.common.response.ApiResponse;
import vn.taskconnect.security.jwt.AuthenticatedPrincipal;
import vn.taskconnect.task.dto.request.ApplyToTaskRequest;
import vn.taskconnect.task.dto.response.MyApplicationResponse;
import vn.taskconnect.task.dto.response.TaskApplicationResponse;
import vn.taskconnect.task.dto.response.TaskFeedItemResponse;
import vn.taskconnect.task.service.TaskApplicationService;

/**
 * Endpoint tim/ung tuyen cong viec (UC10, phia Tasker) va Poster xac nhan ung vien (UC11, gioi
 * han doi trang thai Task/Application - chua tao Booking that, xem
 * docs/TASK-MODULE-SPLIT.md). File rieng voi TaskController.java (phia Poster dang/xem viec
 * cua minh) de tranh dung cham, cung mot module vn.taskconnect.task.
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskApplicationController {

    private final TaskApplicationService applicationService;

    public TaskApplicationController(TaskApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /** Feed cong viec dang mo cho Tasker duyet, loc tuy chon theo danh muc va tu khoa. Khong hien viec do chinh tai khoan nay dang (vai tro Poster). */
    @GetMapping
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<List<TaskFeedItemResponse>> browseOpenTasks(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestParam(required = false) UUID categoryId, @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(applicationService.browseOpenTasks(principal.accountId(), categoryId, keyword));
    }

    /** Xem chi tiet 1 cong viec dang mo - dung khi Tasker vao thang URL chi tiet, khong qua danh sach feed. */
    @GetMapping("/{taskId}/browse")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<TaskFeedItemResponse> getOpenTaskForBrowse(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID taskId) {
        return ApiResponse.ok(applicationService.getOpenTaskForBrowse(principal.accountId(), taskId));
    }

    /** Tasker gui don ung tuyen mot cong viec. */
    @PostMapping("/{taskId}/applications")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<TaskApplicationResponse> apply(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId, @Valid @RequestBody ApplyToTaskRequest request) {
        return ApiResponse.ok(applicationService.apply(principal.accountId(), taskId, request), "Đã gửi ứng tuyển.");
    }

    /** Toan bo don ung tuyen cua chinh Tasker dang goi (moi trang thai) - dung cho man "Viec da nhan". */
    @GetMapping("/applications/mine")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<List<MyApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.ok(applicationService.listMyApplications(principal.accountId()));
    }

    /** Poster xem danh sach ung vien cua mot cong viec cua chinh minh. */
    @GetMapping("/{taskId}/applications")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<List<TaskApplicationResponse>> getApplicants(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID taskId) {
        return ApiResponse.ok(applicationService.listApplicantsForOwner(principal.accountId(), taskId));
    }

    /** Poster xac nhan mot ung vien - Task chuyen ASSIGNED, cac don PENDING con lai chuyen NEEDS_RECONFIRM. */
    @PostMapping("/{taskId}/applications/{applicationId}/confirm")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<TaskApplicationResponse> confirm(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId, @PathVariable UUID applicationId) {
        return ApiResponse.ok(applicationService.confirm(principal.accountId(), taskId, applicationId),
                "Đã xác nhận Tasker cho công việc này.");
    }

    /** Poster tu choi mot ung vien. */
    @PostMapping("/{taskId}/applications/{applicationId}/reject")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<TaskApplicationResponse> reject(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId, @PathVariable UUID applicationId) {
        return ApiResponse.ok(applicationService.reject(principal.accountId(), taskId, applicationId),
                "Đã từ chối ứng viên này.");
    }
}
