-- Module Auth. Bang ma OTP dat lai mat khau, cung hinh dang voi
-- auth_email_verification_tokens SAU KHI da ap V3 (attempt_count co san, khong UNIQUE
-- tren token_hash, chi so ghep (account_id, used_at, created_at)).
-- Xem .claude/rules/12-database.md.

CREATE TABLE `auth_password_reset_tokens` (
  `id` BINARY(16) NOT NULL,
  `account_id` BINARY(16) NOT NULL,
  `token_hash` VARCHAR(255) NOT NULL,
  `attempt_count` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'so lan nhap sai ma nay',
  `expires_at` DATETIME(3) NOT NULL,
  `used_at` DATETIME(3) NULL,
  `created_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_auth_password_reset_tokens_account_active` (`account_id`, `used_at`, `created_at`),
  CONSTRAINT `fk_auth_password_reset_tokens_account`
    FOREIGN KEY (`account_id`) REFERENCES `auth_accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Khong UNIQUE tren token_hash: tra cuu luon dinh pham vi theo account_id (OTP 6 chu so tra cuu toan cuc se cho phep doan trung ma cua tai khoan bat ky), xem AuthPasswordResetTokenRepository.';
