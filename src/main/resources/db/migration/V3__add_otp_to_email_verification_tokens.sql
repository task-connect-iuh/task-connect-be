-- Module Auth. Doi co che xac minh email tu token dai gui qua lien ket sang ma OTP
-- 6 chu so gui qua email that.
-- Xem .claude/rules/12-database.md.

-- 1. Vo hieu hoa moi ma cu con hieu luc. Chung sinh theo co che cu (base64url 32 byte,
--    hash SHA-256) nen khong bao gio doi chieu duoc voi OTP 6 chu so.
--    UTC_TIMESTAMP(3) chu khong phai NOW(3): toan he thong luu UTC
--    (spring.jpa.properties.hibernate.jdbc.time_zone=UTC).
UPDATE `auth_email_verification_tokens`
   SET `used_at` = UTC_TIMESTAMP(3)
 WHERE `used_at` IS NULL;

-- 2. Dem so lan nhap sai tren tung ma. Het nguong thi ma bi vo hieu hoa bang used_at
--    va nguoi dung phai yeu cau ma moi.
ALTER TABLE `auth_email_verification_tokens`
  ADD COLUMN `attempt_count` TINYINT UNSIGNED NOT NULL DEFAULT 0
    COMMENT 'so lan nhap sai ma nay' AFTER `token_hash`;

-- 3. Chi so phuc vu truy van "ma con hieu luc moi nhat cua mot tai khoan".
--    Phai them TRUOC khi bo chi so cu: chi so cu dang do khoa ngoai
--    fk_auth_email_verification_tokens_account, MariaDB khong cho bo neu khong con
--    chi so nao bat dau bang account_id (errno 150).
ALTER TABLE `auth_email_verification_tokens`
  ADD KEY `idx_auth_email_verification_tokens_account_active`
    (`account_id`, `used_at`, `created_at`);

ALTER TABLE `auth_email_verification_tokens`
  DROP INDEX `idx_auth_email_verification_tokens_account`;

-- 4. Bo UNIQUE tren token_hash: khong con tra cuu toan cuc theo hash nua, tra cuu gio
--    day dinh pham vi theo account_id (xem AuthEmailVerificationTokenRepository).
--    BCrypt co salt rieng moi dong nen rang buoc nay khong bao gio bi vi pham, nhung
--    de lai se khien nguoi doc tuong van con tra cuu theo hash.
ALTER TABLE `auth_email_verification_tokens`
  DROP INDEX `uq_auth_email_verification_tokens_token_hash`;
