package vn.taskconnect.task.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.task.api.TaskApplicationStatus;
import vn.taskconnect.task.api.TaskFacade;
import vn.taskconnect.task.api.TaskStatus;
import vn.taskconnect.task.api.dto.PriceProposalCreated;
import vn.taskconnect.task.api.dto.TaskApplicationParties;
import vn.taskconnect.task.api.dto.TaskSummary;
import vn.taskconnect.task.entity.Task;
import vn.taskconnect.task.entity.TaskApplication;
import vn.taskconnect.task.repository.TaskApplicationRepository;
import vn.taskconnect.task.repository.TaskRepository;
import vn.taskconnect.user.api.UserFacade;
import vn.taskconnect.user.api.dto.UserProfileSummary;

/**
 * Trien khai duy nhat cua TaskFacade - khong module nao khac trong task duoc implements
 * interface nay. Cac method thuong luong gia uy quyen cho TaskPriceNegotiationService (cung
 * package, xem Javadoc class do).
 */
@Service
class TaskFacadeImpl implements TaskFacade {

    private final TaskRepository taskRepository;
    private final TaskApplicationRepository applicationRepository;
    private final UserFacade userFacade;
    private final TaskPriceNegotiationService priceNegotiationService;
    private final Clock clock;

    TaskFacadeImpl(TaskRepository taskRepository, TaskApplicationRepository applicationRepository,
            UserFacade userFacade, TaskPriceNegotiationService priceNegotiationService, Clock clock) {
        this.taskRepository = taskRepository;
        this.applicationRepository = applicationRepository;
        this.userFacade = userFacade;
        this.priceNegotiationService = priceNegotiationService;
        this.clock = clock;
    }

    /** Doc task theo id va anh xa sang TaskSummary de tra cho module khac qua TaskFacade. */
    @Override
    public Optional<TaskSummary> findTask(UUID taskId) {
        return taskRepository.findById(taskId).map(this::toSummary);
    }

    /**
     * Chuyen Task sang ASSIGNED khi mot Tasker duoc chap nhan (qua don ung tuyen UC10/11 hoac
     * qua loi moi cua module Matching). Cac don ung tuyen PENDING con lai cua cung task cung
     * duoc chuyen NEEDS_RECONFIRM, giu dung nhat quan OQ-08 (1 Tasker/task) du Tasker duoc
     * chon den tu kenh nao - mirror dung logic TaskApplicationService.confirm() da co.
     */
    @Override
    @Transactional
    public void assignTask(UUID taskId, UUID posterId) {
        Task task = taskRepository.findById(taskId)
                .filter(candidate -> candidate.getPosterId().equals(posterId))
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_ASSIGNED);
        }
        Instant now = clock.instant();
        task.assignTo(now);
        for (TaskApplication application : applicationRepository.findByTaskId(taskId)) {
            if (application.getStatus() == TaskApplicationStatus.PENDING) {
                application.markNeedsReconfirm(now);
            }
        }
    }

    /** Anh xa Task sang TaskSummary voi day du truong Matching can (xem Javadoc TaskSummary). */
    private TaskSummary toSummary(Task task) {
        return new TaskSummary(task.getId(), task.getPosterId(), task.getCategoryId(), task.getStatus(),
                task.getTitle(), task.getDescription(), task.getAddressText(), task.getLat(), task.getLng(),
                task.getBudgetAmount(), task.getScheduledAt());
    }

    /**
     * Doc don ung tuyen + task tuong ung, anh xa sang TaskApplicationParties kem ten/anh dai
     * dien 2 ben (goi UserFacade, Task da san co phu thuoc nay) - dung boi Chat.
     */
    @Override
    public Optional<TaskApplicationParties> getApplicationParties(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .flatMap(application -> taskRepository.findById(application.getTaskId())
                        .map(task -> toParties(application, task)));
    }

    /** Uy quyen truy van JOIN noi bo (task_applications JOIN task_tasks) cho repository - xem Javadoc query. */
    @Override
    public List<UUID> listApplicationIdsForAccount(UUID accountId) {
        return applicationRepository.findApplicationIdsInvolvingAccount(accountId);
    }

    @Override
    public PriceProposalCreated proposePrice(UUID applicationId, UUID proposerAccountId, long amount, String note) {
        return priceNegotiationService.proposePrice(applicationId, proposerAccountId, amount, note);
    }

    @Override
    public long acceptPriceProposal(UUID applicationId, UUID priceHistoryId, UUID accepterAccountId) {
        return priceNegotiationService.acceptPriceProposal(applicationId, priceHistoryId, accepterAccountId);
    }

    @Override
    public Optional<Long> findPriceHistoryAmount(UUID priceHistoryId) {
        return priceNegotiationService.findPriceHistoryAmount(priceHistoryId);
    }

    /**
     * Doc thang applicationRepository (khong qua TaskApplicationService, class do khong phai
     * mot phan cua module nay) - an toan ve vong tron bean: TaskFacadeImpl chi phu thuoc
     * repository noi bo o day, khong phu thuoc ChatFacade (xem quy tac vong tron da phat hien o
     * Round B3, docs/PROGRESS-CHAT-MODULE.md).
     */
    @Override
    public void clearInviteExpiryIfInvited(UUID applicationId, UUID actingAccountId) {
        applicationRepository.findById(applicationId).ifPresent(application -> {
            if (application.getStatus() == TaskApplicationStatus.INVITED
                    && application.getTaskerId().equals(actingAccountId)) {
                application.clearInviteExpiry();
            }
        });
    }

    /** Anh xa entity TaskApplication + Task sang DTO cong khai TaskApplicationParties, giai quyet ten qua UserFacade. */
    private TaskApplicationParties toParties(TaskApplication application, Task task) {
        UserProfileSummary poster = userFacade.findProfile(task.getPosterId()).orElse(null);
        UserProfileSummary tasker = userFacade.findProfile(application.getTaskerId()).orElse(null);
        return new TaskApplicationParties(
                application.getId(), task.getId(), task.getTitle(),
                task.getPosterId(), poster != null ? poster.fullName() : null,
                poster != null ? poster.avatarUrl() : null,
                application.getTaskerId(), tasker != null ? tasker.fullName() : null,
                tasker != null ? tasker.avatarUrl() : null,
                application.getStatus(), application.getCreatedAt());
    }
}
