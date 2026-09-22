package vn.taskconnect.chat.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import vn.taskconnect.chat.api.InboxTab;
import vn.taskconnect.chat.dto.request.CreatePriceProposalRequest;
import vn.taskconnect.chat.dto.request.CreateRescheduleProposalRequest;
import vn.taskconnect.chat.dto.request.SendTextMessageRequest;
import vn.taskconnect.chat.dto.response.ChatInboxItemResponse;
import vn.taskconnect.chat.dto.response.ChatMessageResponse;
import vn.taskconnect.chat.service.ChatService;
import vn.taskconnect.common.response.ApiResponse;
import vn.taskconnect.security.jwt.AuthenticatedPrincipal;

/**
 * Endpoint chat (UC16) - khong gioi han theo role qua @PreAuthorize vi ca Poster va Tasker
 * deu dung chung, quyen xem/gui thuc te kiem tra o ChatService (phai la dung Poster/Tasker
 * cua application, xem dac ta muc 11).
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** Gui 1 tin nhan TEXT trong kenh cua 1 application - lazy-create kenh neu day la lan gui dau tien. */
    @PostMapping("/applications/{applicationId}/messages")
    public ApiResponse<ChatMessageResponse> sendTextMessage(@AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID applicationId, @Valid @RequestBody SendTextMessageRequest request) {
        return ApiResponse.ok(chatService.sendTextMessage(applicationId, principal.accountId(), request.text()));
    }

    /** Toan bo lich su tin nhan cua kenh thuoc 1 application - rong neu kenh chua ton tai. */
    @GetMapping("/applications/{applicationId}/messages")
    public ApiResponse<List<ChatMessageResponse>> listMessages(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId) {
        return ApiResponse.ok(chatService.listMessages(applicationId, principal.accountId()));
    }

    /** Danh sach Inbox cua tai khoan dang goi, loc theo 1 trong 4 tab (dac ta muc 10). */
    @GetMapping("/inbox")
    public ApiResponse<List<ChatInboxItemResponse>> listInbox(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestParam(defaultValue = "ALL") InboxTab tab) {
        return ApiResponse.ok(chatService.listInbox(principal.accountId(), tab));
    }

    /** Tao 1 de xuat gia moi trong kenh cua 1 application (UC16 muc 3) - chi gui duoc tu trong khung chat. */
    @PostMapping("/applications/{applicationId}/price-proposals")
    public ApiResponse<ChatMessageResponse> createPriceProposal(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId,
            @Valid @RequestBody CreatePriceProposalRequest request) {
        return ApiResponse.ok(chatService.createPriceProposal(applicationId, principal.accountId(),
                request.amount(), request.note()), "Đã gửi đề xuất giá.");
    }

    /** Ben khong phai nguoi tao Dong y 1 de xuat gia con PROPOSED. */
    @PostMapping("/applications/{applicationId}/price-proposals/{messageId}/accept")
    public ApiResponse<ChatMessageResponse> acceptPriceProposal(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId,
            @PathVariable UUID messageId) {
        return ApiResponse.ok(chatService.acceptPriceProposal(applicationId, messageId, principal.accountId()),
                "Đã đồng ý mức giá.");
    }

    /** Ben khong phai nguoi tao Tu choi 1 de xuat gia con PROPOSED. */
    @PostMapping("/applications/{applicationId}/price-proposals/{messageId}/reject")
    public ApiResponse<ChatMessageResponse> rejectPriceProposal(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId,
            @PathVariable UUID messageId) {
        return ApiResponse.ok(chatService.rejectPriceProposal(applicationId, messageId, principal.accountId()),
                "Đã từ chối đề xuất giá.");
    }

    /** Chinh nguoi tao Thu hoi 1 de xuat gia con PROPOSED. */
    @PostMapping("/applications/{applicationId}/price-proposals/{messageId}/withdraw")
    public ApiResponse<ChatMessageResponse> withdrawPriceProposal(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId,
            @PathVariable UUID messageId) {
        return ApiResponse.ok(chatService.withdrawPriceProposal(applicationId, messageId, principal.accountId()),
                "Đã thu hồi đề xuất giá.");
    }

    /** Tao 1 de xuat doi lich lam viec moi (UC16 muc 9, Round B6) - chi hop le khi task dang ASSIGNED. */
    @PostMapping("/applications/{applicationId}/reschedule-proposals")
    public ApiResponse<ChatMessageResponse> createRescheduleProposal(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId,
            @Valid @RequestBody CreateRescheduleProposalRequest request) {
        return ApiResponse.ok(chatService.createRescheduleProposal(applicationId, principal.accountId(),
                request.proposedTime(), request.note()), "Đã gửi đề xuất đổi lịch.");
    }

    /** Ben khong phai nguoi tao Dong y 1 de xuat doi lich con PROPOSED. */
    @PostMapping("/applications/{applicationId}/reschedule-proposals/{messageId}/accept")
    public ApiResponse<ChatMessageResponse> acceptRescheduleProposal(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId,
            @PathVariable UUID messageId) {
        return ApiResponse.ok(chatService.acceptRescheduleProposal(applicationId, messageId, principal.accountId()),
                "Đã đồng ý đổi lịch.");
    }

    /** Ben khong phai nguoi tao Tu choi 1 de xuat doi lich con PROPOSED. */
    @PostMapping("/applications/{applicationId}/reschedule-proposals/{messageId}/reject")
    public ApiResponse<ChatMessageResponse> rejectRescheduleProposal(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId,
            @PathVariable UUID messageId) {
        return ApiResponse.ok(chatService.rejectRescheduleProposal(applicationId, messageId, principal.accountId()),
                "Đã từ chối đề xuất đổi lịch.");
    }

    /** Chinh nguoi tao Thu hoi 1 de xuat doi lich con PROPOSED. */
    @PostMapping("/applications/{applicationId}/reschedule-proposals/{messageId}/withdraw")
    public ApiResponse<ChatMessageResponse> withdrawRescheduleProposal(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID applicationId,
            @PathVariable UUID messageId) {
        return ApiResponse.ok(chatService.withdrawRescheduleProposal(applicationId, messageId, principal.accountId()),
                "Đã thu hồi đề xuất đổi lịch.");
    }
}
