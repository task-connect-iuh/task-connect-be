package vn.taskconnect.task.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.task.api.TaskFacade;
import vn.taskconnect.task.api.TaskStatus;
import vn.taskconnect.task.api.TaskApplicationStatus;
import vn.taskconnect.task.api.dto.TaskSummary;
import vn.taskconnect.task.entity.Task;
import vn.taskconnect.task.entity.TaskApplication;
import vn.taskconnect.task.repository.TaskApplicationRepository;
import vn.taskconnect.task.repository.TaskRepository;

/** Trien khai duy nhat cua TaskFacade - khong module nao khac trong task duoc implements interface nay. */
@Service
class TaskFacadeImpl implements TaskFacade {

    private final TaskRepository taskRepository;
    private final TaskApplicationRepository applicationRepository;
    private final Clock clock;

    TaskFacadeImpl(TaskRepository taskRepository, TaskApplicationRepository applicationRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.applicationRepository = applicationRepository;
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
}
