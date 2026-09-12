-- Module User. Them 2 truong "Gioi thieu ngan" danh rieng cho Task Poster: loai dia diem
-- va luu y khi Tasker toi nha - phan thay the "Nang luc & lich lam viec" (chi Tasker co,
-- xem V2/V17) tren ProfilePage.tsx khi xem o vai tro Poster. Ca hai NULL nghia la chua
-- khai bao, khong bat buoc voi ca hai vai tro (mot tai khoan co the vua la Poster vua la
-- Tasker - xem 01-domain-glossary.md - nen khong dat NOT NULL du chi Poster dung toi).
ALTER TABLE `user_profiles`
  ADD COLUMN `location_type` VARCHAR(20) NULL AFTER `preferred_radius_km`,
  ADD COLUMN `arrival_notes` VARCHAR(500) NULL AFTER `location_type`,
  ADD CONSTRAINT `ck_user_profiles_location_type`
    CHECK (`location_type` IS NULL
      OR `location_type` IN ('NHA_RIENG', 'CAN_HO_CHUNG_CU', 'CUA_HANG', 'VAN_PHONG'));
