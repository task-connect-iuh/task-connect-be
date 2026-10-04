-- Chi phi phat sinh (quyet dinh nguoi dung 2026-10-02): Tasker da ung tien mua vat tu/cong lam
-- phat sinh, dang len lam bang chung minh bach de Poster tra lai sau khi hoan thanh - CHI ap dung
-- khi booking chon FULL_ESCROW (xem TaskExtraCostService.submit). Day la co che RIENG, KHONG tai
-- su dung task_price_history/ChangeType: price_history chi luu 1 so tien THAY THE (renegotiate gia
-- chot), con o day la danh sach nhieu khoan DOC LAP cong don, moi khoan co ten/anh rieng. Cau truc
-- header+con giong task_edit_events/task_edit_event_changes (V45) - status/note/nguoi dang-nguoi
-- duyet la thuoc tinh cua CA BATCH, khong phai tung khoan.

CREATE TABLE `task_extra_cost_batches` (
  `id` BINARY(16) NOT NULL,
  `batch_no` INT UNSIGNED NOT NULL COMMENT 'so thu tu hien thi (vd "Phat sinh #2") tinh rieng theo tung application, khong phai ma toan he thong',
  `application_id` BINARY(16) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `total_amount` BIGINT UNSIGNED NOT NULL COMMENT 'cache = SUM(items.amount) luc dang, khong tinh lai sau do',
  `note` VARCHAR(500) NULL COMMENT 'ghi chu CHUNG cho ca batch, khong phai tung khoan',
  `submitted_by_account_id` BINARY(16) NOT NULL,
  `submitted_at` DATETIME(3) NOT NULL,
  `reviewed_by_account_id` BINARY(16) NULL,
  `reviewed_at` DATETIME(3) NULL,
  `withdrawn_at` DATETIME(3) NULL,
  `created_at` DATETIME(3) NOT NULL,
  `updated_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_task_extra_cost_batches_status`
    CHECK (`status` IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
  CONSTRAINT `fk_task_extra_cost_batches_application`
    FOREIGN KEY (`application_id`) REFERENCES `task_applications` (`id`),
  CONSTRAINT `fk_task_extra_cost_batches_submitted_by`
    FOREIGN KEY (`submitted_by_account_id`) REFERENCES `auth_accounts` (`id`),
  CONSTRAINT `fk_task_extra_cost_batches_reviewed_by`
    FOREIGN KEY (`reviewed_by_account_id`) REFERENCES `auth_accounts` (`id`),
  KEY `idx_task_extra_cost_batches_application` (`application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='1 lan Tasker dang chi phi phat sinh (co the gom nhieu khoan) - chi Tasker dang/thu hoi, chi Poster dong y/tu choi';

CREATE TABLE `task_extra_cost_items` (
  `id` BINARY(16) NOT NULL,
  `batch_id` BINARY(16) NOT NULL,
  `name` VARCHAR(255) NOT NULL COMMENT 'ten khoan, vd "Cum voi rua gan tuong moi"',
  `amount` BIGINT UNSIGNED NOT NULL,
  `photo_url` VARCHAR(500) NULL COMMENT 'anh minh chung, tuy chon - public-read S3 giong task_images',
  `sort_order` INT UNSIGNED NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_task_extra_cost_items_batch`
    FOREIGN KEY (`batch_id`) REFERENCES `task_extra_cost_batches` (`id`),
  KEY `idx_task_extra_cost_items_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tung khoan trong 1 batch chi phi phat sinh';
