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
import vn.taskconnect.task.dto.request.ConfirmApplicationRequest;
import vn.taskconnect.task.dto.request.ExtraCostImageUploadUrlRequest;
import vn.taskconnect.task.dto.request.InquireRequest;
import vn.taskconnect.task.dto.request.InviteTaskerRequest;
import vn.taskconnect.task.dto.request.SubmitExtraCostBatchRequest;
import vn.taskconnect.task.dto.response.ConfirmApplicationResponse;
import vn.taskconnect.task.dto.response.ExtraCostImageUploadUrlResponse;
import vn.taskconnect.task.dto.response.ExtraCostMoneySummaryResponse;
import vn.taskconnect.task.dto.response.MyApplicationResponse;
import vn.taskconnect.task.dto.response.TaskApplicationResponse;
import vn.taskconnect.task.dto.response.TaskFeedItemResponse;
import vn.taskconnect.task.dto.response.TaskPriceHistoryEntryResponse;
import vn.taskconnect.task.service.TaskApplicationService;
import vn.taskconnect.task.service.TaskExtraCostImageUploadService;
import vn.taskconnect.task.service.TaskExtraCostService;

/**
 * Endpoint tim/ung tuyen cong viec (UC10, phia Tasker) va Poster xac nhan ung vien (UC11, gioi
 * han doi trang thai Task/Application - chua tao Booking that, xem
 * docs/TASK-MODULE-SPLIT.md). File rieng voi TaskController.java (phia Poster dang/xem viec
 * cua minh) de tranh dung cham, cung mot module vn.taskconnect.task. Tu 2026-10-02 them cac
 * endpoint "Chi phi phat sinh" (xem TaskExtraCostService) - chung mot controller vi cung xoay
 * quanh 1 applicationId da duoc chon o UC11, giong price-history.
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskApplicationController {

    private final TaskApplicationService applicationService;
    private final TaskExtraCostService extraCostService;
    private final TaskExtraCostImageUploadService extraCostImageUploadService;

    public TaskApplicationController(TaskApplicationService applicationService,
            TaskExtraCostService extraCostService, TaskExtraCostImageUploadService extraCostImageUploadService) {
        this.applicationService = applicationService;
        this.extraCostService = extraCostService;
        this.extraCostImageUploadService = extraCostImageUploadService;
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

    /** Tasker bam "Nhan tin hoi them" - tu dong tao don INQUIRING + mo kenh chat ngay (UC16 muc 2). */
    @PostMapping("/{taskId}/applications/inquire")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<TaskApplicationResponse> inquire(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId, @Valid @RequestBody InquireRequest request) {
        return ApiResponse.ok(applicationService.createInquiry(principal.accountId(), taskId, request),
                "Đã gửi lời hỏi thêm.");
    }

    /** Tasker rut mot don dang PENDING hoac INQUIRING cua chinh minh. */
    @PostMapping("/{taskId}/applications/{applicationId}/withdraw")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<TaskApplicationResponse> withdraw(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId, @PathVariable UUID applicationId) {
        return ApiResponse.ok(applicationService.withdraw(principal.accountId(), taskId, applicationId),
                "Đã rút ứng tuyển.");
    }

    /**
     * Tasker bam "Van nhan viec" sau khi Poster doi "Thoi gian mong muon" cua cong viec (UC07
     * Tier 3) - don dang TIME_CHANGED_NEEDS_RECONFIRM tro ve PENDING.
     */
    @PostMapping("/{taskId}/applications/{applicationId}/reconfirm")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<TaskApplicationResponse> reconfirmAfterTimeChange(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID taskId,
            @PathVariable UUID applicationId) {
        return ApiResponse.ok(applicationService.reconfirmAfterTimeChange(principal.accountId(), taskId, applicationId),
                "Đã xác nhận vẫn nhận công việc này.");
    }

    /**
     * Tasker bam "Ung tuyen" tu the "Dang hoi them" - chuyen thang 1 don dang INQUIRING thanh
     * PENDING, khong bat buoc phai thuong luong gia truoc trong chat (xem Javadoc
     * TaskApplicationService.applyFromInquiry()).
     */
    @PostMapping("/{taskId}/applications/{applicationId}/apply-from-inquiry")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<TaskApplicationResponse> applyFromInquiry(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID taskId,
            @PathVariable UUID applicationId) {
        return ApiResponse.ok(applicationService.applyFromInquiry(principal.accountId(), taskId, applicationId),
                "Đã gửi ứng tuyển.");
    }

    /** Toan bo don ung tuyen cua chinh Tasker dang goi (moi trang thai) - dung cho man "Viec da nhan". */
    @GetMapping("/applications/mine")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<List<MyApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.ok(applicationService.listMyApplications(principal.accountId()));
    }

    /**
     * Toan bo lich su gia (chi ghi them) cua 1 application - dung cho man "Lich su gia". Ca
     * Poster va Tasker cua don do xem duoc (khong @PreAuthorize theo role, kiem tra quyen o
     * chinh service qua chinh application/task, giong nguyen tac cua ChatController).
     */
    @GetMapping("/applications/{applicationId}/price-history")
    public ApiResponse<List<TaskPriceHistoryEntryResponse>> getPriceHistory(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId) {
        return ApiResponse.ok(applicationService.listPriceHistory(principal.accountId(), applicationId));
    }

    /** Poster xem danh sach ung vien cua mot cong viec cua chinh minh. */
    @GetMapping("/{taskId}/applications")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<List<TaskApplicationResponse>> getApplicants(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID taskId) {
        return ApiResponse.ok(applicationService.listApplicantsForOwner(principal.accountId(), taskId));
    }

    /**
     * UC11 "Chon nguoi nay" - tao booking-lite that VA giu tien thuc te (gia lap) theo
     * paymentMethod Poster chon, cac ung vien con lai cua task chuyen REJECTED_AUTO kem dong
     * kenh chat. Xem Javadoc TaskApplicationService.confirm().
     */
    @PostMapping("/{taskId}/applications/{applicationId}/confirm")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<ConfirmApplicationResponse> confirm(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId, @PathVariable UUID applicationId,
            @Valid @RequestBody ConfirmApplicationRequest request) {
        return ApiResponse.ok(
                applicationService.confirm(principal.accountId(), taskId, applicationId, request.paymentMethod()),
                "Đã chọn Tasker này cho công việc.");
    }

    /** Poster tu choi mot ung vien. */
    @PostMapping("/{taskId}/applications/{applicationId}/reject")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<TaskApplicationResponse> reject(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId, @PathVariable UUID applicationId) {
        return ApiResponse.ok(applicationService.reject(principal.accountId(), taskId, applicationId),
                "Đã từ chối ứng viên này.");
    }

    /**
     * Poster moi truc tiep mot Tasker nhan cong viec (UC09, Round B5) - xem Javadoc
     * TaskApplicationService.invite() ve cac gate chong spam loi moi.
     */
    @PostMapping("/{taskId}/invitations")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<TaskApplicationResponse> invite(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId, @Valid @RequestBody InviteTaskerRequest request) {
        return ApiResponse.ok(applicationService.invite(principal.accountId(), taskId, request), "Đã gửi lời mời.");
    }

    /** Tasker nhan mot loi moi truc tiep dang cho (INVITED). */
    @PostMapping("/{taskId}/invitations/{applicationId}/accept")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<TaskApplicationResponse> acceptInvite(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID taskId,
            @PathVariable UUID applicationId) {
        return ApiResponse.ok(applicationService.acceptInvite(principal.accountId(), taskId, applicationId),
                "Đã nhận lời mời.");
    }

    /** Tasker tu choi mot loi moi truc tiep dang cho (INVITED) - chan moi lai vinh vien cho cung task nay. */
    @PostMapping("/{taskId}/invitations/{applicationId}/decline")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<TaskApplicationResponse> declineInvite(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID taskId,
            @PathVariable UUID applicationId) {
        return ApiResponse.ok(applicationService.declineInvite(principal.accountId(), taskId, applicationId),
                "Đã từ chối lời mời.");
    }

    /**
     * Tong hop tien cua 1 application (chi phi chot ban dau + cac batch phat sinh + so sanh voi
     * so dang tam giu) - ca Poster va Tasker cua don do xem duoc, khong @PreAuthorize theo role
     * (kiem tra quyen o service, cung nguyen tac voi getPriceHistory).
     */
    @GetMapping("/applications/{applicationId}/extra-costs/summary")
    public ApiResponse<ExtraCostMoneySummaryResponse> getExtraCostSummary(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId) {
        return ApiResponse.ok(extraCostService.getSummary(principal.accountId(), applicationId));
    }

    /** Tasker dang 1 batch chi phi phat sinh moi - xem Javadoc TaskExtraCostService.submit(). */
    @PostMapping("/applications/{applicationId}/extra-costs")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<ExtraCostMoneySummaryResponse> submitExtraCost(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId,
            @Valid @RequestBody SubmitExtraCostBatchRequest request) {
        return ApiResponse.ok(
                extraCostService.submit(principal.accountId(), applicationId, request.note(), request.items()),
                "Đã đăng chi phí phát sinh.");
    }

    /** Tasker tu thu hoi mot batch chi phi phat sinh do chinh minh dang, dang cho duyet. */
    @PostMapping("/applications/{applicationId}/extra-costs/{batchId}/withdraw")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<ExtraCostMoneySummaryResponse> withdrawExtraCost(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId,
            @PathVariable UUID batchId) {
        return ApiResponse.ok(extraCostService.withdraw(principal.accountId(), applicationId, batchId),
                "Đã thu hồi khoản chi phí phát sinh.");
    }

    /** Poster dong y mot batch chi phi phat sinh dang cho duyet. */
    @PostMapping("/applications/{applicationId}/extra-costs/{batchId}/approve")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<ExtraCostMoneySummaryResponse> approveExtraCost(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId,
            @PathVariable UUID batchId) {
        return ApiResponse.ok(extraCostService.approve(principal.accountId(), applicationId, batchId),
                "Đã đồng ý khoản chi phí phát sinh.");
    }

    /** Poster tu choi mot batch chi phi phat sinh dang cho duyet. */
    @PostMapping("/applications/{applicationId}/extra-costs/{batchId}/reject")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<ExtraCostMoneySummaryResponse> rejectExtraCost(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId,
            @PathVariable UUID batchId) {
        return ApiResponse.ok(extraCostService.reject(principal.accountId(), applicationId, batchId),
                "Đã từ chối khoản chi phí phát sinh.");
    }

    /** Poster bam "Nap" de nap them (gia lap) cho du so phai tra sau khi cac batch da duyet tang len. */
    @PostMapping("/applications/{applicationId}/extra-costs/top-up")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<ExtraCostMoneySummaryResponse> topUpExtraCost(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId) {
        return ApiResponse.ok(extraCostService.topUp(principal.accountId(), applicationId), "Đã nạp thêm.");
    }

    /** Xin presigned URL de Tasker tu tai 1 anh minh chung chi phi phat sinh len S3. */
    @PostMapping("/applications/{applicationId}/extra-cost-images/upload-url")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<ExtraCostImageUploadUrlResponse> createExtraCostImageUploadUrl(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId,
            @Valid @RequestBody ExtraCostImageUploadUrlRequest request) {
        return ApiResponse.ok(extraCostImageUploadService.createUploadUrl(applicationId, request));
    }
}
