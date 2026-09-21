package vn.taskconnect.matching.service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.matching.api.TaskerInviteStatus;
import vn.taskconnect.matching.api.event.TaskerInviteCreatedEvent;
import vn.taskconnect.matching.dto.response.MyInviteResponse;
import vn.taskconnect.matching.dto.response.TaskerInviteResponse;
import vn.taskconnect.matching.entity.TaskerInvite;
import vn.taskconnect.matching.repository.TaskerInviteRepository;
import vn.taskconnect.task.api.TaskFacade;
import vn.taskconnect.task.api.TaskStatus;
import vn.taskconnect.task.api.dto.TaskSummary;
import vn.taskconnect.user.api.UserFacade;
import vn.taskconnect.user.api.dto.ServiceCategorySummary;
import vn.taskconnect.user.api.dto.UserProfileSummary;

/**
 * Nghiep vu luong "Poster moi Tasker" - mirror TaskApplicationService.confirm()/reject()
 * nhung theo huong nguoc lai (Poster chu dong moi, khong cho Tasker tu ung tuyen). Khong tao
 * Booking/escrow that o day - accept() chi doi trang thai Task/Invite, dung nguyen tac voi
 * TaskApplicationService.confirm() (xem docs/TASK-MODULE-SPLIT.md, plan da duyet muc "Chua
 * lam o vong nay").
 */
@Service
public class TaskerInviteService {

    private final TaskerInviteRepository inviteRepository;
    private final TaskFacade taskFacade;
    private final UserFacade userFacade;
    private final AiSuggestionService aiSuggestionService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public TaskerInviteService(TaskerInviteRepository inviteRepository, TaskFacade taskFacade, UserFacade userFacade,
            AiSuggestionService aiSuggestionService, ApplicationEventPublisher eventPublisher, Clock clock) {
        this.inviteRepository = inviteRepository;
        this.taskFacade = taskFacade;
        this.userFacade = userFacade;
        this.aiSuggestionService = aiSuggestionService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Poster tao loi moi truc tiep cho mot Tasker. Chan: task khong ton tai/khong thuoc ve
     * Poster nay (TASK_NOT_FOUND), task khong con OPEN (TASK_NOT_OPEN), da moi Tasker nay roi
     * (ALREADY_INVITED - kiem tra truoc de thong bao ro, du DB co UNIQUE constraint chan lai
     * lan nua). Xoa cache goi y cua task nay (AiSuggestionService) va publish
     * TaskerInviteCreatedEvent trong CHINH transaction dang mo, mirror dung cach
     * AuthService.issueAndDispatchVerificationOtp() publish EmailVerificationRequestedEvent.
     */
    @Transactional
    public TaskerInviteResponse create(UUID posterId, UUID taskId, UUID taskerId) {
        TaskSummary task = taskFacade.findTask(taskId)
                .filter(t -> t.posterId().equals(posterId))
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        if (task.status() != TaskStatus.OPEN) {
            throw new BusinessException(ErrorCode.TASK_NOT_OPEN);
        }
        if (taskerId.equals(posterId)) {
            throw new BusinessException(ErrorCode.CANNOT_INVITE_SELF);
        }
        if (inviteRepository.findByTaskIdAndTaskerId(taskId, taskerId).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_INVITED);
        }

        TaskerInvite invite = TaskerInvite.create(UUID.randomUUID(), taskId, taskerId, clock.instant());
        try {
            inviteRepository.saveAndFlush(invite);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ALREADY_INVITED);
        }

        aiSuggestionService.invalidateCache(taskId);
        eventPublisher.publishEvent(new TaskerInviteCreatedEvent(invite.getId(), taskId, taskerId, posterId));
        return toResponse(invite);
    }

    /**
     * Tasker chap nhan mot loi moi - invite chuyen ACCEPTED, Task chuyen ASSIGNED qua
     * TaskFacade.assignTask() (khong tu doi trang thai Task truc tiep - module Matching
     * khong duoc sua entity cua module Task, xem ranh gioi module).
     */
    @Transactional
    public TaskerInviteResponse accept(UUID taskerId, UUID taskId, UUID inviteId) {
        TaskerInvite invite = requireOwnedPendingInvite(taskerId, taskId, inviteId);
        TaskSummary task = taskFacade.findTask(taskId).orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

        invite.accept(clock.instant());
        taskFacade.assignTask(taskId, task.posterId());
        return toResponse(invite);
    }

    /** Tasker tu choi mot loi moi - chuyen DECLINED, khong dong den Task hay cac loi moi khac. */
    @Transactional
    public TaskerInviteResponse decline(UUID taskerId, UUID taskId, UUID inviteId) {
        TaskerInvite invite = requireOwnedPendingInvite(taskerId, taskId, inviteId);
        invite.decline(clock.instant());
        return toResponse(invite);
    }

    /** Toan bo loi moi cua mot Task, dung cho Poster xem lai da moi ai (khong bat buoc trong scope controller hien tai nhung giu san cho UI sau). */
    @Transactional(readOnly = true)
    public List<TaskerInviteResponse> listInvitesForTask(UUID taskId) {
        return inviteRepository.findByTaskId(taskId).stream().map(this::toResponse).toList();
    }

    /**
     * Toan bo loi moi (moi trang thai) ma chinh Tasker dang goi da nhan duoc - dung cho man
     * "Loi moi ban nhan duoc" (FE: getMyInvites(), xem api/matching.ts). Bo qua loi moi tro toi
     * mot Task da bi xoa/khong con doc duoc qua TaskFacade (khong nen xay ra o quy mo hien tai,
     * nhung khong de crash ca danh sach vi mot ban ghi le).
     */
    @Transactional(readOnly = true)
    public List<MyInviteResponse> listMyInvites(UUID taskerId) {
        List<TaskerInvite> invites = inviteRepository.findByTaskerIdOrderByCreatedAtDesc(taskerId);
        if (invites.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> categoryNameById = userFacade.listActiveServiceCategories().stream()
                .collect(Collectors.toMap(ServiceCategorySummary::id, ServiceCategorySummary::name));
        Map<UUID, String> posterNameById = new HashMap<>();
        return invites.stream()
                .map(invite -> taskFacade.findTask(invite.getTaskId()).map(task -> {
                    String posterName = posterNameById.computeIfAbsent(task.posterId(),
                            id -> userFacade.findProfile(id).map(UserProfileSummary::fullName).orElse(null));
                    return new MyInviteResponse(invite.getId(), invite.getStatus(), invite.getCreatedAt(),
                            invite.getRespondedAt(), task.id(), task.title(), task.addressText(),
                            task.budgetAmount(), task.scheduledAt(), task.status(), task.categoryId(),
                            categoryNameById.get(task.categoryId()), posterName, List.of());
                }).orElse(null))
                .filter(response -> response != null)
                .toList();
    }

    /** Kiem tra loi moi ton tai, thuoc dung Task, thuoc dung Tasker dang goi, va con PENDING. */
    private TaskerInvite requireOwnedPendingInvite(UUID taskerId, UUID taskId, UUID inviteId) {
        TaskerInvite invite = inviteRepository.findByIdAndTaskId(inviteId, taskId)
                .filter(i -> i.getTaskerId().equals(taskerId))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_NOT_FOUND));
        if (invite.getStatus() != TaskerInviteStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVITE_NOT_PENDING);
        }
        return invite;
    }

    private TaskerInviteResponse toResponse(TaskerInvite invite) {
        UserProfileSummary profile = userFacade.findProfile(invite.getTaskerId()).orElse(null);
        return new TaskerInviteResponse(invite.getId(), invite.getTaskerId(),
                profile != null ? profile.fullName() : null, profile != null ? profile.avatarUrl() : null,
                invite.getStatus(), invite.getCreatedAt(), invite.getRespondedAt());
    }
}
