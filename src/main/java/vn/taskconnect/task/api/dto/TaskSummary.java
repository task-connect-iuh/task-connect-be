package vn.taskconnect.task.api.dto;

import java.util.UUID;
import vn.taskconnect.task.api.TaskStatus;

/**
 * Thong tin toi thieu cua mot cong viec, dung khi module khac (Matching, Booking...) can doc
 * qua {@link vn.taskconnect.task.api.TaskFacade} sau nay. Dot 1 chua module nao goi toi -
 * tao truoc theo dung convention moi module co mot facade (mirror AuthFacade/UserFacade).
 */
public record TaskSummary(UUID id, UUID posterId, UUID categoryId, TaskStatus status) {
}
