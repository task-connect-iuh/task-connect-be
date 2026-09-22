-- Bang lich su gia thuong luong (module Task, tien to task_) - CHI GHI THEM, khong UPDATE/DELETE
-- dong da co, tru dung 1 UPDATE hep duoc phep tren accepted_by_account_id/accepted_at (NULL ->
-- gia tri, khong dao nguoc) khi de xuat duoc dong y - xem
-- TaskConnect_Chat_ImplementationSpec.md muc 3 va 12, va ke hoach o docs/PROGRESS-CHAT-MODULE.md
-- muc 0.4 ve ly do 2 cot nay ton tai du bang duoc goi la "append-only".
--
-- change_type xac dinh tu dong theo task.status tai thoi diem tao (INITIAL_AGREEMENT truoc
-- ASSIGNED, SCOPE_CHANGE sau ASSIGNED) - logic o TaskApplicationService, khong o day.

CREATE TABLE `task_price_history` (
  `id` BINARY(16) NOT NULL,
  `application_id` BINARY(16) NOT NULL,
  `change_type` VARCHAR(20) NOT NULL,
  `amount` BIGINT UNSIGNED NOT NULL COMMENT 'don vi dong',
  `note` TEXT NULL,
  `created_by_account_id` BINARY(16) NOT NULL,
  `created_at` DATETIME(3) NOT NULL,
  `accepted_by_account_id` BINARY(16) NULL,
  `accepted_at` DATETIME(3) NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_task_price_history_change_type`
    CHECK (`change_type` IN ('INITIAL_AGREEMENT', 'SCOPE_CHANGE')),
  CONSTRAINT `fk_task_price_history_application`
    FOREIGN KEY (`application_id`) REFERENCES `task_applications` (`id`),
  CONSTRAINT `fk_task_price_history_created_by`
    FOREIGN KEY (`created_by_account_id`) REFERENCES `auth_accounts` (`id`),
  CONSTRAINT `fk_task_price_history_accepted_by`
    FOREIGN KEY (`accepted_by_account_id`) REFERENCES `auth_accounts` (`id`),
  KEY `idx_task_price_history_application` (`application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Lich su de xuat gia (append-only, xem TaskConnect_Chat_ImplementationSpec.md muc 3)';
