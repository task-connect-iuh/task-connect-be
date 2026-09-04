-- Chong CCCD trung giua cac tai khoan khac nhau. id_number_enc ma hoa AES-GCM
-- non-deterministic nen khong the dat UNIQUE truc tiep tren no; them id_number_hash
-- (SHA-256 cua so CCCD, deterministic) de tra cuu/doi chieu.
ALTER TABLE `user_kyc_verifications`
  ADD COLUMN `id_number_hash` BINARY(32) NULL AFTER `id_number_enc`,
  ADD KEY `idx_user_kyc_verifications_id_number_hash` (`id_number_hash`);

-- Bang "khoa" giu CCCD nao dang duoc mot tai khoan nao do "chiem" (dang VERIFYING hoac da
-- VERIFIED) - PRIMARY KEY tren id_number_hash la lop chan cuoi cung o muc DB chong 2 tai
-- khoan khac nhau cung dung 1 CCCD tai thoi diem gan nhu dong thoi (xem
-- KycVerificationService.submitKyc). Khac user_kyc_verifications (moi lan nop la 1 dong,
-- khong UNIQUE), bang nay chi giu duy nhat 1 dong "dang hieu luc" cho moi CCCD: bi xoa khi
-- ho so lien quan bi tu choi/huy (giai phong CCCD do), giu lai khi VERIFIED (danh tinh da
-- xac nhan vinh vien).
CREATE TABLE `user_kyc_id_number_locks` (
  `id_number_hash` BINARY(32) NOT NULL,
  `account_id` BINARY(16) NOT NULL,
  `kyc_verification_id` BINARY(16) NOT NULL,
  `claimed_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id_number_hash`),
  KEY `idx_user_kyc_id_number_locks_account` (`account_id`),
  CONSTRAINT `fk_user_kyc_id_number_locks_account`
    FOREIGN KEY (`account_id`) REFERENCES `auth_accounts` (`id`),
  CONSTRAINT `fk_user_kyc_id_number_locks_verification`
    FOREIGN KEY (`kyc_verification_id`) REFERENCES `user_kyc_verifications` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
