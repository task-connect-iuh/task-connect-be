-- Tin nhan "Chi phi phat sinh" hien truc tiep trong khung chat nhu 1 the rieng (giong
-- PRICE_PROPOSAL), thay vi chi 1 dong SYSTEM text - yeu cau nguoi dung 2026-10-03. Chi luu
-- THAM CHIEU toi task_extra_cost_batches (khong luu lai status/items/totalAmount o day) de
-- tranh 2 nguon su that, dung co che voi ref_price_history_id (V32)/ref_task_edit_id (V46) -
-- Chat doc tuoi qua TaskFacade.findExtraCostBatch() moi lan hien tin nhan.
ALTER TABLE `chat_messages`
  ADD COLUMN `ref_extra_cost_batch_id` BINARY(16) NULL
    COMMENT 'chi co khi message_type = EXTRA_COST_BATCH'
    AFTER `ref_task_edit_id`;

ALTER TABLE `chat_messages`
  ADD CONSTRAINT `fk_chat_messages_extra_cost_batch`
    FOREIGN KEY (`ref_extra_cost_batch_id`) REFERENCES `task_extra_cost_batches` (`id`);

ALTER TABLE `chat_messages`
  DROP CONSTRAINT `ck_chat_messages_message_type`,
  ADD CONSTRAINT `ck_chat_messages_message_type`
    CHECK (`message_type` IN ('TEXT', 'SYSTEM', 'PRICE_PROPOSAL', 'RESCHEDULE_PROPOSAL', 'IMAGE', 'FILE', 'VIDEO', 'VOICE', 'EXTRA_COST_BATCH'));
