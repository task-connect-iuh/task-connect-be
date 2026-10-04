-- Cot danh dau 1 dong task_applications duoc tao TU DONG boi module Matching khi Tasker chap
-- nhan loi moi AI goi y (TaskerInviteService.accept()), chi de lam "vo chua" cho chat_channels
-- (FK application_id) - KHONG phai mot don ung tuyen/loi moi thuc su qua TaskApplicationService.
-- NULL o moi dong binh thuong. Dung de loc cac dong nay ra khoi "Viec da nhan" cua Tasker va
-- danh sach ung vien cua Poster (xem TaskApplicationRepository/TaskApplicationService).
ALTER TABLE `task_applications`
  ADD COLUMN `external_invite_ref` BINARY(16) NULL AFTER `initiated_by`;
