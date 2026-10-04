-- UC07: SYSTEM message "Poster vua cap nhat thong tin cong viec." mang kem id cua su kien sua
-- de FE hien nut "Xem chi tiet thay doi" - cung co che voi ref_price_history_id (V32), KHONG
-- them message_type moi (van la SYSTEM, chi la SYSTEM co dinh kem).
ALTER TABLE `chat_messages`
  ADD COLUMN `ref_task_edit_id` BINARY(16) NULL
    COMMENT 'chi co khi SYSTEM message sinh ra tu 1 lan Poster sua cong viec (UC07)'
    AFTER `ref_price_history_id`;

ALTER TABLE `chat_messages`
  ADD CONSTRAINT `fk_chat_messages_task_edit`
    FOREIGN KEY (`ref_task_edit_id`) REFERENCES `task_edit_events` (`id`);
