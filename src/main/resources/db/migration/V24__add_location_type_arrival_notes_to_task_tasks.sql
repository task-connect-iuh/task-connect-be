-- Module Task. Them loai dia diem va luu y khi toi noi cho tung cong viec, dung cho phan
-- "Noi lam viec" tren PostTaskPage.tsx. FE dien san 2 truong nay tu ho so Poster
-- (user_profiles.location_type/arrival_notes, xem V22) khi mo form dang viec, nhung sau do
-- luu doc lap hoan toan tren task_tasks - sua o day chi ap dung cho cong viec nay, khong ghi
-- nguoc lai ho so, cung nguyen tac voi address_text/lat/lng da co (xem Javadoc Task.java).
-- Ca hai NULL nghia la khong khai bao, khong bat buoc.
ALTER TABLE `task_tasks`
  ADD COLUMN `location_type` VARCHAR(20) NULL AFTER `lng`,
  ADD COLUMN `arrival_notes` VARCHAR(500) NULL AFTER `location_type`,
  ADD CONSTRAINT `ck_task_tasks_location_type`
    CHECK (`location_type` IS NULL
      OR `location_type` IN ('NHA_RIENG', 'CAN_HO_CHUNG_CU', 'CUA_HANG', 'VAN_PHONG'));
