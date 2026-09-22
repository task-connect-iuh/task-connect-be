-- TaskConnect_Chat_ImplementationSpec.md muc 8/8.1 (ban cap nhat 2026-09-17) doi mo hinh:
-- truoc day 1 cap (task_id, tasker_id) chi co DUNG 1 dong task_applications ton tai mai mai
-- (UNIQUE trong V19__create_task_application_table.sql), moi lai sau INVITE_EXPIRED phai tai
-- su dung chinh dong cu. Dac ta cap nhat noi ro: chi DECLINED (va REJECTED thu cong - quyet
-- dinh nguoi dung 2026-09-17, coi tuong duong DECLINED) moi chan vinh vien; WITHDRAWN/
-- REJECTED_AUTO/INVITE_EXPIRED KHONG chan gi - ung tuyen lai/moi lai phai tao MOT DONG MOI
-- HOAN TOAN, khong tai su dung dong hay kenh chat cu. Vi vay bo UNIQUE(task_id, tasker_id):
-- 1 cap co the co NHIEU dong task_applications qua thoi gian (moi dong ung voi 1 lan quan he).
-- Dieu kien "khong qua 1 don ACTIVE dong thoi cho 1 cap" chuyen xuong kiem tra o tang service
-- (TaskApplicationService), khong con DB constraint nao giu vai tro nay - xem
-- docs/PROGRESS-CHAT-MODULE.md.
ALTER TABLE `task_applications`
  DROP INDEX `uq_task_applications_task_tasker`;
