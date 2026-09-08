-- Module Task, phan Tasker ung tuyen (UC10) va Poster xac nhan (UC11, gioi han doi trang thai
-- Task/Application - chua tao Booking that, xem task-connect-claude/docs/TASK-MODULE-SPLIT.md).
-- Nhom bang tien to `task_` trong database `taskconnect`, tiep noi V18__create_task_tables.sql.

CREATE TABLE `task_applications` (
  `id` BINARY(16) NOT NULL,
  `task_id` BINARY(16) NOT NULL,
  `tasker_id` BINARY(16) NOT NULL,
  `proposed_arrival_text` VARCHAR(200) NOT NULL COMMENT 'thoi gian Tasker de xuat toi, van ban tu do (khong phai DATETIME that)',
  `message` TEXT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `created_at` DATETIME(3) NOT NULL,
  `responded_at` DATETIME(3) NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_task_applications_status`
    CHECK (`status` IN ('PENDING', 'ACCEPTED', 'REJECTED', 'NEEDS_RECONFIRM')),
  CONSTRAINT `uq_task_applications_task_tasker` UNIQUE (`task_id`, `tasker_id`),
  CONSTRAINT `fk_task_applications_task`
    FOREIGN KEY (`task_id`) REFERENCES `task_tasks` (`id`),
  CONSTRAINT `fk_task_applications_tasker`
    FOREIGN KEY (`tasker_id`) REFERENCES `auth_accounts` (`id`),
  KEY `idx_task_applications_task` (`task_id`),
  KEY `idx_task_applications_tasker` (`tasker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Ung tuyen cong viec (UC10/UC11 gioi han) - xem docs/TASK-MODULE-SPLIT.md';
