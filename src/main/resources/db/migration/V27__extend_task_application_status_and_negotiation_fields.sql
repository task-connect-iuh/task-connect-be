-- Mo rong task_applications cho module Chat & Thuong luong gia (UC09/UC11/UC16, xem
-- TaskConnect_Chat_ImplementationSpec.md muc 1 va 5). Giu nguyen 4 gia tri status cu
-- (PENDING, ACCEPTED, REJECTED, NEEDS_RECONFIRM) de tuong thich nguoc voi du lieu/code hien
-- co (ACCEPTED/NEEDS_RECONFIRM tro thanh gia tri "chet" tu Round B4 tro di - khong con code
-- path nao set nua, xem docs/PROGRESS-CHAT-MODULE.md), them 6 gia tri moi theo dac ta.
--
-- Tach DROP va ADD CHECK thanh 2 cau ALTER TABLE rieng (khong gop chung 1 cau bang dau phay)
-- - xem bai hoc trong V15__fix_cancelled_status_check_constraints.sql: MariaDB am tham bo
-- qua phan ADD neu DROP/ADD cung ten constraint trong 1 cau lenh.

ALTER TABLE `task_applications`
  DROP CONSTRAINT `ck_task_applications_status`;
ALTER TABLE `task_applications`
  ADD CONSTRAINT `ck_task_applications_status`
    CHECK (`status` IN ('PENDING', 'ACCEPTED', 'REJECTED', 'NEEDS_RECONFIRM',
                         'INQUIRING', 'INVITED', 'WITHDRAWN', 'REJECTED_AUTO',
                         'DECLINED', 'INVITE_EXPIRED'));

ALTER TABLE `task_applications`
  ADD COLUMN `proposed_price` BIGINT UNSIGNED NULL
    COMMENT 'don vi dong, muc gia gan nhat ca 2 ben da dong y qua PRICE_PROPOSAL (chat module)'
    AFTER `status`;

ALTER TABLE `task_applications`
  ADD COLUMN `initiated_by` VARCHAR(10) NOT NULL DEFAULT 'TASKER'
    COMMENT 'ai khoi tao don nay - TASKER (tu ung tuyen/hoi them) hay POSTER (moi truc tiep, UC09)'
    AFTER `proposed_price`;
ALTER TABLE `task_applications`
  ADD CONSTRAINT `ck_task_applications_initiated_by`
    CHECK (`initiated_by` IN ('TASKER', 'POSTER'));

ALTER TABLE `task_applications`
  ADD COLUMN `expires_at` DATETIME(3) NULL
    COMMENT 'chi dung khi status = INVITED, xoa ve NULL ngay khi Tasker phan hoi trong channel (UC09)'
    AFTER `initiated_by`;
