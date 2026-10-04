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
import vn.taskconnect.task.api.TaskEditableField;
import vn.taskconnect.task.api.dto.ExtraCostBatchSummary;
import vn.taskconnect.task.api.dto.PriceProposalCreated;
import vn.taskconnect.task.api.dto.TaskApplicationParties;
import vn.taskconnect.task.api.dto.TaskEditSummary;
import vn.taskconnect.task.api.dto.TaskSummary;
import vn.taskconnect.task.entity.Task;
import vn.taskconnect.task.entity.TaskApplication;
import vn.taskconnect.task.entity.TaskEditChange;
import vn.taskconnect.task.entity.TaskEditEvent;
import vn.taskconnect.task.entity.TaskExtraCostBatch;
import vn.taskconnect.task.repository.TaskApplicationRepository;
import vn.taskconnect.task.repository.TaskEditChangeRepository;
import vn.taskconnect.task.repository.TaskEditEventRepository;
import vn.taskconnect.task.repository.TaskExtraCostBatchRepository;
import vn.taskconnect.task.repository.TaskExtraCostItemRepository;
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
    private final TaskEditEventRepository editEventRepository;
    private final TaskEditChangeRepository editChangeRepository;
    private final TaskExtraCostBatchRepository extraCostBatchRepository;
    private final TaskExtraCostItemRepository extraCostItemRepository;
    private final UserFacade userFacade;
    private final TaskPriceNegotiationService priceNegotiationService;
    private final Clock clock;

    TaskFacadeImpl(TaskRepository taskRepository, TaskApplicationRepository applicationRepository,
            TaskEditEventRepository editEventRepository, TaskEditChangeRepository editChangeRepository,
            TaskExtraCostBatchRepository extraCostBatchRepository, TaskExtraCostItemRepository extraCostItemRepository,
            UserFacade userFacade, TaskPriceNegotiationService priceNegotiationService, Clock clock) {
        this.taskRepository = taskRepository;
        this.applicationRepository = applicationRepository;
        this.editEventRepository = editEventRepository;
        this.editChangeRepository = editChangeRepository;
        this.extraCostBatchRepository = extraCostBatchRepository;
        this.extraCostItemRepository = extraCostItemRepository;
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

    /**
     * Chi tiet 1 lan Poster sua cong viec (UC07) - dung boi Chat khi nguoi dung bam "Xem chi
     * tiet thay doi" tren SYSTEM message. Khong tu kiem tra quyen xem o day - Chat da tu xac
     * dinh nguoi goi la 1 trong 2 ben cua channel truoc khi goi toi day.
     */
    @Override
    public Optional<TaskEditSummary> findTaskEdit(UUID taskEditEventId) {
        return editEventRepository.findById(taskEditEventId).map(event -> {
            List<TaskEditSummary.Change> changes = editChangeRepository
                    .findByEventIdOrderBySortOrderAsc(event.getId()).stream()
                    .map(this::toChange)
                    .toList();
            return new TaskEditSummary(event.getId(), event.getTaskId(), event.getCreatedAt(), changes);
        });
    }

    /**
     * Dong bo scheduledAt cua Task sau khi Booking chap nhan 1 RESCHEDULE_PROPOSAL (UC16 muc 9) -
     * xem Javadoc TaskFacade.syncScheduledAt(). No-op an toan neu taskId khong ton tai (khong nem
     * loi - Booking la nguon goi, khong can Task xac nhan lai su ton tai truoc do).
     */
    @Override
    @Transactional
    public void syncScheduledAt(UUID taskId, Instant scheduledAt) {
        taskRepository.findById(taskId)
                .ifPresent(task -> task.syncScheduledAtFromBooking(scheduledAt, clock.instant()));
    }

    /**
     * Doc 1 batch "Chi phi phat sinh" + danh sach khoan cua no, anh xa sang DTO cong khai -
     * dung boi Chat de hien tin nhan EXTRA_COST_BATCH (bo sung 2026-10-03). Khong tu kiem tra
     * quyen xem, cung convention voi findTaskEdit()/findPriceHistoryAmount() o tren.
     */
    @Override
    public Optional<ExtraCostBatchSummary> findExtraCostBatch(UUID batchId) {
        return extraCostBatchRepository.findById(batchId).map(this::toExtraCostBatchSummary);
    }

    /** Anh xa TaskExtraCostBatch + danh sach khoan cua no sang ExtraCostBatchSummary cong khai. */
    private ExtraCostBatchSummary toExtraCostBatchSummary(TaskExtraCostBatch batch) {
        List<ExtraCostBatchSummary.Item> items = extraCostItemRepository
                .findByBatchIdOrderBySortOrderAsc(batch.getId()).stream()
                .map(item -> new ExtraCostBatchSummary.Item(item.getName(), item.getAmount(), item.getPhotoUrl()))
                .toList();
        return new ExtraCostBatchSummary(batch.getId(), batch.getBatchNo(), batch.getStatus(), batch.getNote(),
                items, batch.getTotalAmount(), batch.getSubmittedAt(), batch.getReviewedAt());
    }

    /** Anh xa 1 dong TaskEditChange sang DTO cong khai. */
    private TaskEditSummary.Change toChange(TaskEditChange change) {
        TaskEditableField field = change.getField();
        return new TaskEditSummary.Change(field, change.getOldValue(), change.getNewValue());
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
                application.getStatus(), application.getCreatedAt(), task.getStatus());
    }
}
