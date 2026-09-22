package vn.taskconnect.task.api.dto;

import java.util.UUID;
import vn.taskconnect.task.api.ChangeType;

/**
 * Ket qua tao 1 dong task_price_history moi, tra ve cho Chat de gan vao chat_messages
 * (ref_price_history_id) - xem TaskFacade.proposePrice().
 */
public record PriceProposalCreated(UUID priceHistoryId, ChangeType changeType) {
}
