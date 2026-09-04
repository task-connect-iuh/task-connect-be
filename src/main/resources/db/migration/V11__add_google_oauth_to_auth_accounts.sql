-- Ho tro dang nhap/dang ky bang tai khoan Google. Tai khoan tao qua Google khong co mat khau
-- (password_hash NULL), nen bo rang buoc NOT NULL cu. google_id (Google "sub" claim) dinh
-- danh duy nhat tai khoan Google, dung de tim lai tai khoan da tung dang nhap Google truoc do.
ALTER TABLE `auth_accounts`
  MODIFY COLUMN `password_hash` VARCHAR(72) NULL COMMENT 'bcrypt, NULL neu tai khoan chi dang nhap qua Google',
  ADD COLUMN `google_id` VARCHAR(255) NULL AFTER `phone`,
  ADD UNIQUE KEY `uq_auth_accounts_google_id` (`google_id`);
