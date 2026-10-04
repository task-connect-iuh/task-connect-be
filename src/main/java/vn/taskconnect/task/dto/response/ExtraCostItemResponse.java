package vn.taskconnect.task.dto.response;

import java.util.UUID;

/** 1 khoan trong 1 batch chi phi phat sinh, dung hien thi trong ExtraCostBatchResponse. */
public record ExtraCostItemResponse(UUID id, String name, long amount, String photoUrl) {
}
