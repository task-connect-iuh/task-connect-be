package vn.taskconnect.task.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Yeu cau Tasker dang 1 batch chi phi phat sinh (co the gom nhieu khoan) - dung cho
 * POST /tasks/applications/{applicationId}/extra-costs. note la ghi chu CHUNG cho ca batch, tuy
 * chon.
 */
public record SubmitExtraCostBatchRequest(String note, @NotEmpty @Valid List<ExtraCostItemInput> items) {
}
