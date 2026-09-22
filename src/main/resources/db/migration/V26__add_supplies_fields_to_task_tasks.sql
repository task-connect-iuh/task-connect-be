-- Module Task. Them thong tin vat tu khi dang viec: supplies_status (bat buoc - Poster da co
-- san vat tu hay chua luc dang tin, khong co gia tri mac dinh o FE) va supplies_note (mo ta
-- them, LUON tuy chon du supplies_status la FULL/PARTIAL/UNKNOWN - kiem tra o
-- CreateTaskRequest, khong rang buoc NOT NULL o DB). Cung quy uoc enum-nhu-varchar+CHECK da
-- dung cho status (V18) va location_type (V24), khong dung ENUM native cua MariaDB. DEFAULT
-- 'UNKNOWN' chi de cac dong da co truoc migration nay hop le voi constraint NOT NULL, request
-- tao moi luon phai gui gia tri ro rang (xem CreateTaskRequest.suppliesStatus @NotNull).
ALTER TABLE `task_tasks`
  ADD COLUMN `supplies_status` VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN' AFTER `arrival_notes`,
  ADD COLUMN `supplies_note` TEXT NULL AFTER `supplies_status`,
  ADD CONSTRAINT `ck_task_tasks_supplies_status`
    CHECK (`supplies_status` IN ('FULL', 'PARTIAL', 'UNKNOWN'));
