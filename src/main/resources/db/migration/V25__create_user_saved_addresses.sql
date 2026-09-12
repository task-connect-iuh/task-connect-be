-- Module User. So dia chi Poster tu luu (vd "Nha", "Cong ty") de chon nhanh moi lan dang
-- viec, giong so dia chi giao hang cua Shopee - dung cho phan "Noi lam viec" tren
-- PostTaskPage.tsx. Hoan toan doc lap voi user_profiles.address_text va task_tasks.address_text
-- (xem V22/V24) - chi la danh sach goi y de dien nhanh, khong dong bo nguoc lai. Moi dong luu
-- ca dia diem lan loai dia diem/luu y de chon 1 lan la dien du ca 3 truong.
CREATE TABLE `user_saved_addresses` (
  `id` BINARY(16) NOT NULL,
  `account_id` BINARY(16) NOT NULL,
  `label` VARCHAR(100) NOT NULL COMMENT 'vd Nha, Cong ty - nguoi dung tu dat luc luu',
  `address_text` VARCHAR(500) NOT NULL,
  `lat` DECIMAL(10,7) NOT NULL,
  `lng` DECIMAL(10,7) NOT NULL,
  `location_type` VARCHAR(20) NULL,
  `arrival_notes` VARCHAR(500) NULL,
  `created_at` DATETIME(3) NOT NULL,
  `updated_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_saved_addresses_account` (`account_id`),
  CONSTRAINT `ck_user_saved_addresses_location_type`
    CHECK (`location_type` IS NULL
      OR `location_type` IN ('NHA_RIENG', 'CAN_HO_CHUNG_CU', 'CUA_HANG', 'VAN_PHONG')),
  CONSTRAINT `fk_user_saved_addresses_account`
    FOREIGN KEY (`account_id`) REFERENCES `auth_accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='So dia chi tu luu de chon nhanh, doc lap voi user_profiles va task_tasks';
