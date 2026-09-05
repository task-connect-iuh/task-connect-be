-- Doi email cua chinh minh - luong 2 buoc: xac minh quyen so huu email HIEN TAI bang OTP,
-- roi xac minh quyen so huu email MOI bang OTP rieng truoc khi thuc su doi
-- auth_accounts.email. Mo hinh 1 dong "dang hieu luc" cho moi tai khoan (khac append-only
-- cua auth_email_verification_tokens) - AuthService luon xoa dong cu roi tao dong moi khi
-- nguoi dung bam "Doi email" lai tu dau, xem AuthService.requestEmailChange().
CREATE TABLE `auth_email_change_tokens` (
  `id` BINARY(16) NOT NULL,
  `account_id` BINARY(16) NOT NULL,
  `old_otp_hash` VARCHAR(255) NOT NULL,
  `old_otp_expires_at` DATETIME(3) NOT NULL,
  `old_verified_at` DATETIME(3) NULL,
  `old_attempt_count` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `new_email` VARCHAR(255) NULL,
  `new_otp_hash` VARCHAR(255) NULL,
  `new_otp_expires_at` DATETIME(3) NULL,
  `new_attempt_count` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_auth_email_change_tokens_account` (`account_id`),
  CONSTRAINT `fk_auth_email_change_tokens_account`
    FOREIGN KEY (`account_id`) REFERENCES `auth_accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
