package vn.taskconnect.task.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.taskconnect.common.response.ApiResponse;
import vn.taskconnect.common.response.PageResponse;
import vn.taskconnect.security.jwt.AuthenticatedPrincipal;
import vn.taskconnect.task.api.TaskAiFlagReason;
import vn.taskconnect.task.dto.request.AnalyzeTaskImageRequest;
import vn.taskconnect.task.dto.request.CreateTaskRequest;
import vn.taskconnect.task.dto.request.RefineClarifyingAnswersRequest;
import vn.taskconnect.task.dto.request.RejectTaskRequest;
import vn.taskconnect.task.dto.request.SuggestTaskPriceRequest;
import vn.taskconnect.task.dto.request.TaskImageUploadUrlRequest;
import vn.taskconnect.task.dto.request.UpdateTaskRequest;
import vn.taskconnect.task.dto.response.RefineClarifyingAnswersResponse;
import vn.taskconnect.task.dto.response.TaskImageSuggestionResponse;
import vn.taskconnect.task.dto.response.TaskImageUploadUrlResponse;
import vn.taskconnect.task.dto.response.TaskPriceSuggestionResponse;
import vn.taskconnect.task.dto.response.TaskResponse;
import vn.taskconnect.task.dto.response.TaskReviewSummaryResponse;
import vn.taskconnect.task.service.TaskImageUploadService;
import vn.taskconnect.task.service.TaskPriceSuggestionService;
import vn.taskconnect.task.service.TaskService;

/**
 * Endpoint dang viec (UC06 toi gian, dot 1): tao, xem danh sach cua chinh minh, xem chi
 * tiet, va tu UC07 sua/huy khi con OPEN/PENDING_REVIEW (xem updateTask()/cancelTask()). Theo
 * doi lich su trang thai (UC08) va ung tuyen/xac nhan (UC10/UC11) lam dot sau - xem
 * docs/PROGRESS-TASK-POSTER-MODULE.md. Chua co endpoint duyet danh sach cong khai cho Tasker
 * (browse) o dot nay.
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskImageUploadService imageUploadService;
    private final TaskPriceSuggestionService priceSuggestionService;

    public TaskController(TaskService taskService, TaskImageUploadService imageUploadService,
            TaskPriceSuggestionService priceSuggestionService) {
        this.taskService = taskService;
        this.imageUploadService = imageUploadService;
        this.priceSuggestionService = priceSuggestionService;
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
     * Poster sua cong viec cua chinh minh (UC07) - chi khi task con OPEN/PENDING_REVIEW. Chi
     * sua duoc 6 truong trong UpdateTaskRequest; tieu de/mo ta/anh/dia chi khoa cung vinh vien.
     * Nhom truong "nang" (ngan sach, vat tu) bi khoa khi dang co it nhat 1 don PENDING - BE van
     * kiem tra lai trong cung transaction du FE da vo hieu hoa o nhap (chong dua: Tasker ung
     * tuyen xen giua luc mo form va luc bam Luu).
     */
    @PatchMapping("/{taskId}")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<TaskResponse> updateTask(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequest request) {
        return ApiResponse.ok(taskService.updateTask(principal.accountId(), taskId, request), "Đã cập nhật công việc.");
    }

    /**
     * Poster tu huy cong viec cua chinh minh (UC07) - chi khi con OPEN/PENDING_REVIEW, huy tu do
     * bat ke co bao nhieu nguoi ung tuyen. Tu ASSIGNED tro di phai qua UC14 (dong thuan hai ben).
     */
    @PatchMapping("/{taskId}/cancel")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<Void> cancelTask(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId) {
        taskService.cancelTask(principal.accountId(), taskId);
        return ApiResponse.ok(null, "Đã huỷ công việc.");
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

    /**
     * Goi y dien form (tieu de/mo ta/danh muc) tu MOT anh minh hoa da tai len S3 truoc do (xem
     * POST /tasks/images-upload-url) - CHI la goi y, Poster xem va sua/xoa tuy y truoc khi bam
     * "Dang viec". Khong goi y gia/lich ranh (anh khong the hien thi hai thu nay).
     */
    @PostMapping("/analyze-image")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<TaskImageSuggestionResponse> analyzeTaskImage(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody AnalyzeTaskImageRequest request) {
        return ApiResponse.ok(taskService.analyzeTaskImage(principal.accountId(), request));
    }

    /**
     * Goi y muc gia luc dang viec, dua tren gia da chot cua cac task tuong tu trong qua khu
     * cung category (xem Javadoc TaskPriceSuggestionService) - CHI la goi y, Poster xem va
     * sua/xoa tuy y, khong rang buoc gi. available=false (het du lieu tuong tu/AI loi) van tra
     * 200, KHONG phai loi - FE tu quyet dinh giu nguyen "thoa thuan".
     */
    @PostMapping("/suggest-price")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<TaskPriceSuggestionResponse> suggestTaskPrice(
            @Valid @RequestBody SuggestTaskPriceRequest request) {
        return ApiResponse.ok(priceSuggestionService.suggestPrice(request));
    }

    /**
     * Gop mo ta hien tai voi cac cau tra loi Poster vua dien trong modal "Hoi them" (xem
     * ClarifyAssistantDialog.tsx) thanh MOT doan mo ta hoan chinh do AI viet lai - CHI la goi y,
     * Poster van xem va sua tren o Mo ta truoc khi dang. available=false (het quota/loi mang)
     * van tra 200, KHONG phai loi - FE tu fallback ve cach ghep tho "{questionText}: {answer}.".
     */
    @PostMapping("/refine-description")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<RefineClarifyingAnswersResponse> refineClarifyingAnswers(
            @Valid @RequestBody RefineClarifyingAnswersRequest request) {
        return ApiResponse.ok(taskService.refineClarifyingAnswers(request));
    }

    /**
     * Chi Admin: hang doi cac cong viec dang can hau kiem (AI gan co OTHER_CATEGORY,
     * mot trong ba nhom SUSPICIOUS, POSTER_OVERRIDE, hoac CLASSIFICATION_FAILED - xem
     * .claude/rules/15-ai-module.md), moi nhat truoc, loc duoc theo ly do gan co.
     */
    @GetMapping("/flagged")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<TaskReviewSummaryResponse>> listFlaggedTasks(
            @RequestParam(required = false) TaskAiFlagReason reason,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.from(
                taskService.listFlaggedTasks(reason, PageRequest.of(page, Math.min(size, 100)))));
    }

    /** Chi Admin: xac nhan mot cong viec dang hau kiem la khong vi pham - go co, giu nguyen trang thai hien tai. */
    @PatchMapping("/{taskId}/resolve-review")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> resolveFlaggedTask(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId) {
        taskService.resolveFlaggedTask(taskId, principal.accountId());
        return ApiResponse.ok(null, "Đã xác nhận công việc không vi phạm.");
    }

    /**
     * Chi Admin: tu choi mot cong viec dang hau kiem, bat buoc kem ly do - chuyen sang REJECTED,
     * an khoi feed Tasker ngay lap tuc.
     */
    @PatchMapping("/{taskId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> rejectFlaggedTask(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId, @Valid @RequestBody RejectTaskRequest request) {
        taskService.rejectFlaggedTask(taskId, principal.accountId(), request);
        return ApiResponse.ok(null, "Đã từ chối công việc này.");
    }
}
