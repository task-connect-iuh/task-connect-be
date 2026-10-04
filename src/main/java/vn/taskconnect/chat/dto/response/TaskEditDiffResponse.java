package vn.taskconnect.chat.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.taskconnect.task.api.TaskEditableField;

/**
 * Chi tiet "truoc -> sau" cua 1 lan Poster sua cong viec (UC07), tra ve tu
 * GET .../messages/{messageId}/task-edit khi FE bam nut "Xem chi tiet thay doi" tren 1 SYSTEM
 * message co refTaskEditId. Anh xa truc tiep tu TaskEditSummary (TaskFacade), chi doi ten
 * package cho dung quy uoc response DTO cua module Chat.
 */
public record TaskEditDiffResponse(UUID id, Instant editedAt, List<Change> changes) {

    public record Change(TaskEditableField field, String oldValue, String newValue) {
    }
}
