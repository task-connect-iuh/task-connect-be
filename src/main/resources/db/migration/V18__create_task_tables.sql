-- Module Task. Nhom bang tien to `task_` trong database `taskconnect`.
-- Dot 1 (dang viec toi gian, UC06): chi tao + xem danh sach cua minh + xem chi tiet. Sua/huy
-- (UC07), lich su chuyen trang thai (UC08), va ung tuyen (UC10/UC11) lam dot sau - xem
-- docs/PROGRESS-TASK-POSTER-MODULE.md. Status mac dinh OPEN ngay khi tao (khong qua
-- PENDING_REVIEW/duyet AI hay Admin dot nay - quyet dinh da chot voi nguoi dung), nhung van
-- giu du 7 gia tri trong CHECK constraint theo dung state machine da chot trong
-- .claude/rules/01-domain-glossary.md, tranh phai sua schema khi dot sau them chuyen trang thai.
-- Xem .claude/rules/00-architecture.md, .claude/rules/12-database.md.

CREATE TABLE `task_tasks` (
  `id` BINARY(16) NOT NULL,
  `poster_id` BINARY(16) NOT NULL,
  `category_id` BINARY(16) NOT NULL,
  `title` VARCHAR(150) NOT NULL,
  `description` TEXT NOT NULL,
  `address_text` VARCHAR(500) NOT NULL COMMENT 'dia diem can thuc hien cong viec, doc lap voi user_profiles.address_text',
  `lat` DECIMAL(10,7) NOT NULL,
  `lng` DECIMAL(10,7) NOT NULL,
  `budget_amount` BIGINT UNSIGNED NULL COMMENT 'don vi dong, NULL nghia la thoa thuan',
  `scheduled_at` DATETIME(3) NULL,
  `estimated_workers_needed` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'chi de hien thi, khong co nghia he thong, xem OQ-08',
  `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  `created_at` DATETIME(3) NOT NULL,
  `updated_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_task_tasks_status`
    CHECK (`status` IN ('PENDING_REVIEW', 'OPEN', 'ASSIGNED', 'COMPLETED', 'CLOSED', 'CANCELLED', 'REJECTED')),
  CONSTRAINT `fk_task_tasks_poster`
    FOREIGN KEY (`poster_id`) REFERENCES `auth_accounts` (`id`),
  CONSTRAINT `fk_task_tasks_category`
    FOREIGN KEY (`category_id`) REFERENCES `user_service_categories` (`id`),
  KEY `idx_task_tasks_poster` (`poster_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Dot 1 dang viec toi gian - xem docs/PROGRESS-TASK-POSTER-MODULE.md';

CREATE TABLE `task_task_images` (
  `id` BINARY(16) NOT NULL,
  `task_id` BINARY(16) NOT NULL,
  `image_url` VARCHAR(500) NOT NULL,
  `display_order` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_task_task_images_task`
    FOREIGN KEY (`task_id`) REFERENCES `task_tasks` (`id`) ON DELETE CASCADE,
  KEY `idx_task_task_images_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Toi da 5 anh/cong viec, kiem tra o tang service (TaskService), khong o DB';
