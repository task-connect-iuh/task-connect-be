-- UC07 (Poster huy/sua viec da dang). Hai gia tri status moi cho task_applications:
--   CANCELLED                    - Poster huy ca cong viec khi con OPEN/PENDING_REVIEW (khac
--                                  REJECTED_AUTO: khong ai thang, cong viec khong con ton tai).
--   TIME_CHANGED_NEEDS_RECONFIRM - Poster doi "Thoi gian mong muon" khi don dang PENDING; Tasker
--                                  phai xac nhan lai hoac rut. KHONG tai su dung NEEDS_RECONFIRM
--                                  (gia tri chet tu Round B4, se xoa - xem Javadoc TaskApplicationStatus).
--
-- Cot status dang VARCHAR(20) (V19) - TIME_CHANGED_NEEDS_RECONFIRM dai 28 ky tu nen phai noi
-- rong cot TRUOC khi them vao CHECK, neu khong moi lan ghi se bi cat chuoi/loi.
-- Tach DROP va ADD CHECK thanh 2 cau ALTER TABLE rieng - xem
-- V15__fix_cancelled_status_check_constraints.sql va V27: MariaDB am tham bo qua phan ADD neu
-- DROP/ADD cung ten constraint trong 1 cau lenh.

ALTER TABLE `task_applications`
  MODIFY COLUMN `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING';

ALTER TABLE `task_applications`
  DROP CONSTRAINT `ck_task_applications_status`;

ALTER TABLE `task_applications`
  ADD CONSTRAINT `ck_task_applications_status`
    CHECK (`status` IN ('PENDING', 'ACCEPTED', 'REJECTED', 'NEEDS_RECONFIRM',
                        'INQUIRING', 'INVITED', 'WITHDRAWN', 'REJECTED_AUTO',
                        'DECLINED', 'INVITE_EXPIRED',
                        'CANCELLED', 'TIME_CHANGED_NEEDS_RECONFIRM'));
