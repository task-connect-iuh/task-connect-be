-- proposed_arrival_text ("Thoi gian ban co the toi") bi bo khoi form ung tuyen theo yeu cau
-- nguoi dung (xem docs/PROGRESS-TASK-TASKER-MODULE.md) - doi tu bat buoc sang tuy chon. Migration
-- rieng vi V19 da duoc ap dung truoc do, khong sua lai V19 (pha checksum Flyway da ap dung).

ALTER TABLE `task_applications`
  MODIFY COLUMN `proposed_arrival_text` VARCHAR(200) NULL
    COMMENT 'thoi gian Tasker de xuat toi, van ban tu do (khong phai DATETIME that) - da bo khoi form ung tuyen, giu cot de tuong thich nguoc';
