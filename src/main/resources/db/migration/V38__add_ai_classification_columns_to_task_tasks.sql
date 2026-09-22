-- Module Task + AI. Them cot luu ket qua AI phan loai category luc dang viec va co hau kiem
-- cho Admin (xem .claude/rules/15-ai-module.md). Quyet dinh da chot: hau kiem hoan toan,
-- khong tien kiem - task_tasks.status KHONG doi vi dieu nay (van luon OPEN ngay tu
-- Task.createOpen()), cac cot duoi day chi de Admin xem lai sau, khong chan luong dang viec.
-- ai_flag_reason con MO gia tri nguong do tin cay cu the (OQ-03, docs/OPEN-QUESTIONS.md) -
-- hien tai TaskService chi so sanh danh muc AI de xuat voi lua chon cua Poster.
ALTER TABLE `task_tasks`
  ADD COLUMN `ai_suggested_category_id` BINARY(16) NULL AFTER `status`,
  ADD COLUMN `ai_confidence` TINYINT UNSIGNED NULL AFTER `ai_suggested_category_id`,
  ADD COLUMN `needs_admin_review` TINYINT(1) NOT NULL DEFAULT 0 AFTER `ai_confidence`,
  ADD COLUMN `ai_flag_reason` VARCHAR(30) NULL AFTER `needs_admin_review`,
  ADD CONSTRAINT `ck_task_tasks_ai_flag_reason`
    CHECK (`ai_flag_reason` IS NULL
      OR `ai_flag_reason` IN ('OTHER_CATEGORY', 'SUSPICIOUS_UNSAFE', 'SUSPICIOUS_SPAM',
        'SUSPICIOUS_HARASSMENT', 'POSTER_OVERRIDE', 'CLASSIFICATION_FAILED')),
  ADD CONSTRAINT `fk_task_tasks_ai_suggested_category`
    FOREIGN KEY (`ai_suggested_category_id`) REFERENCES `user_service_categories` (`id`);

CREATE INDEX `idx_task_tasks_needs_admin_review` ON `task_tasks` (`needs_admin_review`);
