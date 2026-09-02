-- Seed 1 tai khoan super-admin bootstrap - khong co cach hop le nao khac de co duoc tai
-- khoan mang role ADMIN (AuthService.register() chan cung tu gan ADMIN qua API cong khai,
-- xem docs/PROGRESS-ADMIN-MODULE.md). Dung placeholder Flyway ${adminEmail}/${adminPasswordHash}
-- (spring.flyway.placeholders.* trong application.yml, nap tu bien moi truong ADMIN_SEED_EMAIL/
-- ADMIN_SEED_PASSWORD_HASH) - KHONG hardcode email/mat khau that vao migration, vi migration
-- nam trong git history vinh vien. adminPasswordHash la hash bcrypt tinh san ngoai band,
-- khong phai mat khau thuan.
--
-- Tai khoan nay duoc AuthService nhan dien la super-admin duy nhat bang cach so khop email
-- voi app.admin.super-admin-email (cung bien ADMIN_SEED_EMAIL) o tang service, khong dung
-- gia tri role rieng trong schema - xem AdminProperties.java va
-- docs/PROGRESS-ADMIN-MODULE.md "Cac quyet dinh da chot".
INSERT INTO `auth_accounts`
  (`id`, `email`, `phone`, `password_hash`, `status`, `failed_login_count`, `created_at`, `updated_at`)
VALUES
  (UNHEX(REPLACE(UUID(), '-', '')), '${adminEmail}', NULL, '${adminPasswordHash}', 'ACTIVE', 0, NOW(3), NOW(3));

INSERT INTO `auth_account_roles`
  (`id`, `account_id`, `role`, `granted_at`)
SELECT UNHEX(REPLACE(UUID(), '-', '')), `id`, 'ADMIN', NOW(3)
FROM `auth_accounts`
WHERE `email` = '${adminEmail}';
