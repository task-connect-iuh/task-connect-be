-- Module User. Them truong "che do lich lam viec" cho Tasker: FLEXIBLE (gio linh hoat,
-- nhan viec bat cu luc nao) hoac CUSTOM (tu chon khung gio ranh cu the, xem
-- user_tasker_availability tu V2). NULL nghia la chua khai bao (tai khoan cu, FE coi nhu
-- CUSTOM). Khong dong bo/xoa du lieu user_tasker_availability khi doi mode - FE tu an/khoa
-- phan khai bao khung gio, du lieu cu van giu nguyen de hien lai neu chuyen ve CUSTOM.
ALTER TABLE `user_profiles`
  ADD COLUMN `availability_mode` VARCHAR(20) NULL AFTER `arrival_notes`,
  ADD CONSTRAINT `ck_user_profiles_availability_mode`
    CHECK (`availability_mode` IS NULL OR `availability_mode` IN ('FLEXIBLE', 'CUSTOM'));
