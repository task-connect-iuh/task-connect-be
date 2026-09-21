-- Module Task + AI. Quyet dinh moi: Poster chon danh muc hoan toan tu do, khong con so sanh
-- voi danh muc AI de xuat - bo nhom ly do hau kiem POSTER_OVERRIDE va 2 cot luu ket qua so
-- sanh (ai_suggested_category_id/ai_confidence, xem V30). AI chi con phat hien OTHER_CATEGORY
-- va 3 nhom SUSPICIOUS.
--
-- Don du lieu test cu (neu co) truoc khi sua CHECK constraint, tranh vi pham rang buoc.
UPDATE `task_tasks`
  SET `needs_admin_review` = 0, `ai_flag_reason` = NULL
  WHERE `ai_flag_reason` = 'POSTER_OVERRIDE';

-- DROP va ADD CONSTRAINT CUNG TEN trong CUNG mot cau ALTER TABLE bi MariaDB coi la "khong doi
-- gi" va am tham bo qua ADD (da gap loi nay o V15__fix_cancelled_status_check_constraints.sql)
-- - bat buoc tach thanh hai cau ALTER TABLE rieng.
ALTER TABLE `task_tasks`
  DROP CONSTRAINT `ck_task_tasks_ai_flag_reason`;
ALTER TABLE `task_tasks`
  ADD CONSTRAINT `ck_task_tasks_ai_flag_reason`
    CHECK (`ai_flag_reason` IS NULL
      OR `ai_flag_reason` IN ('OTHER_CATEGORY', 'SUSPICIOUS_UNSAFE', 'SUSPICIOUS_SPAM',
        'SUSPICIOUS_HARASSMENT', 'CLASSIFICATION_FAILED'));

ALTER TABLE `task_tasks`
  DROP FOREIGN KEY `fk_task_tasks_ai_suggested_category`,
  DROP COLUMN `ai_suggested_category_id`,
  DROP COLUMN `ai_confidence`;
