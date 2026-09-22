-- Module Admin, khoi tao toi gian - chi bang admin_system_parameters de Chat/Task/Booking
-- doc nguong nghiep vu (khong hardcode trong Java, xem .claude/rules/02-source-of-truth.md).
-- Cac phan con lai cua module Admin (duyet ho so, khieu nai, dashboard) chua lam o dot nay.

CREATE TABLE `admin_system_parameters` (
  `id` BINARY(16) NOT NULL,
  `param_key` VARCHAR(100) NOT NULL,
  `param_value` VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) NULL,
  `created_at` DATETIME(3) NOT NULL,
  `updated_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `uq_admin_system_parameters_key` UNIQUE (`param_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Nguong van hanh doc dong, xem .claude/rules/02-source-of-truth.md';

-- Seed 3 tham so Round B0 can ngay: ty le phi nen tang (da chot 8% trong 02-source-of-truth.md,
-- lan dau duoc dua vao bang tham so - truoc day chua co bang nay de hardcode), gioi han moi
-- dong thoi/task va thoi han het han loi moi (dac ta muc 1 va 8).
INSERT INTO `admin_system_parameters`
  (`id`, `param_key`, `param_value`, `description`, `created_at`, `updated_at`)
VALUES
  (UNHEX(REPLACE('00000000-0000-4000-b000-000000000001', '-', '')), 'platform_fee_rate', '0.08',
   'Ty le phi nen tang tren fee_base, tru truc tiep luc giai ngan cho Tasker', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  (UNHEX(REPLACE('00000000-0000-4000-b000-000000000002', '-', '')), 'max_concurrent_invites_per_task', '5',
   'So don INVITED toi da dong thoi cho 1 cong viec (UC09 chong spam loi moi)', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  (UNHEX(REPLACE('00000000-0000-4000-b000-000000000003', '-', '')), 'invite_expiry_hours', '24',
   'So gio truoc khi 1 loi moi (INVITED) tu dong chuyen INVITE_EXPIRED neu Tasker im lang', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3));
