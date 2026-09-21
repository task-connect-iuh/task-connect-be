package vn.taskconnect.matching.controller;

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
import vn.taskconnect.matching.dto.request.CreateInviteRequest;
import vn.taskconnect.matching.dto.response.MyInviteResponse;
import vn.taskconnect.matching.dto.response.SuggestedTaskerResponse;
import vn.taskconnect.matching.dto.response.TaskerInviteResponse;
import vn.taskconnect.matching.service.AiSuggestionService;
import vn.taskconnect.matching.service.TaskerInviteService;
import vn.taskconnect.security.jwt.AuthenticatedPrincipal;

/**
 * Endpoint goi y Tasker bang AI (UC09) va Poster moi truc tiep Tasker (luong moi, song song
 * UC10/UC11) - xem plan da duyet "AI Tasker Suggestion". File rieng cho module Matching,
 * cung quy uoc voi TaskController/TaskApplicationController cua module Task.
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class MatchingController {

    private final AiSuggestionService aiSuggestionService;
    private final TaskerInviteService inviteService;

    public MatchingController(AiSuggestionService aiSuggestionService, TaskerInviteService inviteService) {
        this.aiSuggestionService = aiSuggestionService;
        this.inviteService = inviteService;
    }

    /**
     * Poster xem danh sach Tasker duoc AI goi y cho mot cong viec cua chinh minh, toi da 10 (hoac
     * 20 neu expand=true - "Xem them" tren FE, mo rong pham vi bo qua preferredRadiusKm tu khai
     * cua Tasker), sap giam dan do phu hop.
     */
    @GetMapping("/{taskId}/suggested-taskers")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<List<SuggestedTaskerResponse>> getSuggestedTaskers(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID taskId,
            @RequestParam(defaultValue = "false") boolean expand) {
        return ApiResponse.ok(aiSuggestionService.getSuggestions(taskId, principal.accountId(), expand));
    }

    /** Poster moi truc tiep mot Tasker cho cong viec cua minh. */
    @PostMapping("/{taskId}/invites")
    @PreAuthorize("hasRole('TASK_POSTER')")
    public ApiResponse<TaskerInviteResponse> createInvite(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId, @Valid @RequestBody CreateInviteRequest request) {
        return ApiResponse.ok(inviteService.create(principal.accountId(), taskId, request.taskerId()),
                "Đã gửi lời mời tới Tasker này.");
    }

    /** Tasker chap nhan mot loi moi - Task chuyen ASSIGNED. */
    @PostMapping("/{taskId}/invites/{inviteId}/accept")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<TaskerInviteResponse> acceptInvite(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId, @PathVariable UUID inviteId) {
        return ApiResponse.ok(inviteService.accept(principal.accountId(), taskId, inviteId),
                "Bạn đã nhận công việc này.");
    }

    /** Tasker tu choi mot loi moi. */
    @PostMapping("/{taskId}/invites/{inviteId}/decline")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<TaskerInviteResponse> declineInvite(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID taskId, @PathVariable UUID inviteId) {
        return ApiResponse.ok(inviteService.decline(principal.accountId(), taskId, inviteId),
                "Đã từ chối lời mời này.");
    }

    /** Toan bo loi moi (moi trang thai) ma chinh Tasker dang goi da nhan duoc - man "Loi moi ban nhan duoc". */
    @GetMapping("/invites/mine")
    @PreAuthorize("hasRole('TASKER')")
    public ApiResponse<List<MyInviteResponse>> getMyInvites(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.ok(inviteService.listMyInvites(principal.accountId()));
    }
}
