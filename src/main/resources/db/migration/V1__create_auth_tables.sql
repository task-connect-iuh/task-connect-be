-- Module Auth. Nhom bang tien to `auth_` trong database `taskconnect`.
-- Xem .claude/rules/00-architecture.md, .claude/rules/01-domain-glossary.md,
-- .claude/rules/12-database.md.

CREATE TABLE `auth_accounts` (
  `id` BINARY(16) NOT NULL,
  `email` VARCHAR(255) NOT NULL,
  `phone` VARCHAR(20) NULL,
  `password_hash` VARCHAR(72) NOT NULL COMMENT 'bcrypt',
  `status` VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
  `failed_login_count` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `locked_until` DATETIME(3) NULL COMMENT 'khoa 15 phut sau 5 lan dang nhap sai',
  `last_login_at` DATETIME(3) NULL,
  `created_at` DATETIME(3) NOT NULL,
  `updated_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_auth_accounts_email` (`email`),
  UNIQUE KEY `uq_auth_accounts_phone` (`phone`),
  KEY `idx_auth_accounts_status` (`status`),
  CONSTRAINT `ck_auth_accounts_status`
    CHECK (`status` IN ('UNVERIFIED', 'ACTIVE', 'LOCKED', 'SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Bang goc dinh danh. Moi module khac tham chieu qua account_id, co the la FK vat ly.';

CREATE TABLE `auth_account_roles` (
  `id` BINARY(16) NOT NULL,
  `account_id` BINARY(16) NOT NULL,
  `role` VARCHAR(20) NOT NULL,
  `granted_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_auth_account_roles_account_role` (`account_id`, `role`),
  CONSTRAINT `ck_auth_account_roles_role`
    CHECK (`role` IN ('TASK_POSTER', 'TASKER', 'ADMIN')),
  CONSTRAINT `fk_auth_account_roles_account`
    FOREIGN KEY (`account_id`) REFERENCES `auth_accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='1 tai khoan co the mang dong thoi TASK_POSTER va TASKER';

CREATE TABLE `auth_refresh_tokens` (
  `id` BINARY(16) NOT NULL,
  `account_id` BINARY(16) NOT NULL,
  `token_hash` VARCHAR(255) NOT NULL,
  `expires_at` DATETIME(3) NOT NULL,
  `revoked_at` DATETIME(3) NULL,
  `created_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_auth_refresh_tokens_token_hash` (`token_hash`),
  KEY `idx_auth_refresh_tokens_account` (`account_id`),
  CONSTRAINT `fk_auth_refresh_tokens_account`
    FOREIGN KEY (`account_id`) REFERENCES `auth_accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `auth_email_verification_tokens` (
  `id` BINARY(16) NOT NULL,
  `account_id` BINARY(16) NOT NULL,
  `token_hash` VARCHAR(255) NOT NULL,
  `expires_at` DATETIME(3) NOT NULL,
  `used_at` DATETIME(3) NULL,
  `created_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_auth_email_verification_tokens_token_hash` (`token_hash`),
  KEY `idx_auth_email_verification_tokens_account` (`account_id`),
  CONSTRAINT `fk_auth_email_verification_tokens_account`
    FOREIGN KEY (`account_id`) REFERENCES `auth_accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
