-- Module Task. Them cot phuc vu man hang doi hau kiem cua Admin (xem
-- .claude/rules/15-ai-module.md) - Admin xac nhan "khong vi pham" (chi go needs_admin_review,
-- khong doi cot nao khac) hoac tu choi (chuyen status sang REJECTED, bat buoc kem ly do).
-- Cung mau reviewed_by_admin_id/reviewed_at/rejection_reason da dung o user_kyc_verifications
-- (V2 migration).
ALTER TABLE `task_tasks`
  ADD COLUMN `reviewed_by_admin_id` BINARY(16) NULL AFTER `ai_flag_reason`,
  ADD COLUMN `reviewed_at` DATETIME(3) NULL AFTER `reviewed_by_admin_id`,
  ADD COLUMN `rejection_reason` VARCHAR(500) NULL AFTER `reviewed_at`,
  ADD CONSTRAINT `fk_task_tasks_reviewed_by_admin`
    FOREIGN KEY (`reviewed_by_admin_id`) REFERENCES `auth_accounts` (`id`);
