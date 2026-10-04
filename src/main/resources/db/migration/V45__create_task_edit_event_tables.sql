-- UC07: anh chup "truoc -> sau" cua 1 lan Poster sua cong viec, CHI de phuc vu nut
-- "Xem chi tiet thay doi" tren SYSTEM message trong kenh chat. KHONG phai audit log tong quat:
-- chi ghi cac truong SUA DUOC (xem UpdateTaskRequest), chi ghi lan nao that su co thay doi,
-- khong bao gio UPDATE/DELETE dong da ghi (append-only, cung dang voi task_price_history -
-- xem V30__create_task_price_history_table.sql).

CREATE TABLE `task_edit_events` (
  `id` BINARY(16) NOT NULL,
  `task_id` BINARY(16) NOT NULL,
  `edited_by_account_id` BINARY(16) NOT NULL COMMENT 'luon la poster_id cua task - UC07 khong cho ai khac sua',
  `created_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_task_edit_events_task`
    FOREIGN KEY (`task_id`) REFERENCES `task_tasks` (`id`),
  CONSTRAINT `fk_task_edit_events_editor`
    FOREIGN KEY (`edited_by_account_id`) REFERENCES `auth_accounts` (`id`),
  KEY `idx_task_edit_events_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='1 lan bam Luu o form Sua viec (UC07) - xem task_edit_event_changes';

CREATE TABLE `task_edit_event_changes` (
  `id` BINARY(16) NOT NULL,
  `event_id` BINARY(16) NOT NULL,
  `field` VARCHAR(20) NOT NULL COMMENT 'khop TaskEditableField, KHONG phai ten cot DB',
  `old_value` TEXT NULL COMMENT 'gia tri THO (so dong, ten enum, ISO-8601) - FE tu dinh dang/dich nhan',
  `new_value` TEXT NULL,
  `sort_order` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_task_edit_event_changes_field`
    CHECK (`field` IN ('LOCATION_TYPE', 'ARRIVAL_NOTES', 'BUDGET_AMOUNT',
                       'SUPPLIES_STATUS', 'SUPPLIES_NOTE', 'SCHEDULED_AT')),
  CONSTRAINT `fk_task_edit_event_changes_event`
    FOREIGN KEY (`event_id`) REFERENCES `task_edit_events` (`id`) ON DELETE CASCADE,
  KEY `idx_task_edit_event_changes_event` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tung truong da doi trong 1 lan sua - chi ghi truong THAT SU doi gia tri';
