package vn.taskconnect.task.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.taskconnect.task.api.TaskFacade;
import vn.taskconnect.task.api.dto.TaskSummary;
import vn.taskconnect.task.entity.Task;
import vn.taskconnect.task.repository.TaskRepository;

/** Trien khai duy nhat cua TaskFacade - khong module nao khac trong task duoc implements interface nay. */
@Service
class TaskFacadeImpl implements TaskFacade {

    private final TaskRepository taskRepository;

    TaskFacadeImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /** Doc task theo id va anh xa sang TaskSummary de tra cho module khac qua TaskFacade. */
    @Override
    public Optional<TaskSummary> findTask(UUID taskId) {
        return taskRepository.findById(taskId)
                .map(task -> new TaskSummary(task.getId(), task.getPosterId(), task.getCategoryId(), task.getStatus()));
    }
}
