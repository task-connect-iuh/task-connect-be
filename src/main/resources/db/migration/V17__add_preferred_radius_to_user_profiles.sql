-- Module User. Them ban kinh lam viec uu tien (km) cua Tasker quanh khu vuc hoat dong -
-- FE dung de Tasker tu khai bao pham vi minh muon nhan viec, hien canh Lich lam viec o
-- ProfilePage.tsx. NULL nghia la chua khai bao (khong bat buoc). Khong lien quan OQ-02
-- (ban kinh ghep viec mac dinh cua he thong o admin.system_parameters, van chua chot) -
-- day la tuy chon rieng cua tung Tasker, khong phai tham so van hanh toan he thong.
ALTER TABLE `user_profiles`
  ADD COLUMN `preferred_radius_km` INT NULL AFTER `location_lng`;
