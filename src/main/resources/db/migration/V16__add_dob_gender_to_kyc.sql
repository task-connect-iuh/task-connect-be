-- Them ngay sinh va gioi tinh vao ho so xac minh danh tinh (KYC) - thu thap cung luc voi
-- ho ten/so CCCD tren form nop. NULL-able vi cac ban ghi da nop truoc migration nay chua co
-- du lieu (khong backfill duoc) - tang cuong bat buoc o tang ung dung (SubmitKycRequest) cho
-- moi lan nop moi.
ALTER TABLE `user_kyc_verifications`
  ADD COLUMN `date_of_birth` DATE NULL AFTER `full_name_on_id`,
  ADD COLUMN `gender` VARCHAR(10) NULL AFTER `date_of_birth`,
  ADD CONSTRAINT `ck_user_kyc_verifications_gender`
    CHECK (`gender` IS NULL OR `gender` IN ('MALE', 'FEMALE', 'OTHER'));
