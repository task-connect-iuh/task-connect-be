-- Module User. Nhom dich vu Task Poster thuong thue ("Ban thuong thue viec gi" tren
-- ProfilePage.tsx, phan "Gioi thieu ngan" cua Poster - xem V22). Dung lai danh muc
-- user_service_categories hien co (5 nhom dien-nuoc da chot theo GUARDRAIL #4
-- CLAUDE.md) thay vi tao danh muc rieng. Chi mang tinh mo ta boi canh cho Tasker doc
-- truoc khi ung tuyen, KHONG dung cho Matching/AI phan loai (khac category_id tren
-- task_tasks). Cau truc giong het user_tasker_skill_profiles (surrogate id + UNIQUE
-- account+category) nhung khong co trang thai xac minh vi day khong phai ho so ky nang.
CREATE TABLE `user_poster_job_categories` (
  `id` BINARY(16) NOT NULL,
  `account_id` BINARY(16) NOT NULL,
  `category_id` BINARY(16) NOT NULL,
  `created_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_poster_job_categories_account_category` (`account_id`, `category_id`),
  CONSTRAINT `fk_user_poster_job_categories_account`
    FOREIGN KEY (`account_id`) REFERENCES `auth_accounts` (`id`),
  CONSTRAINT `fk_user_poster_job_categories_category`
    FOREIGN KEY (`category_id`) REFERENCES `user_service_categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Chi mo ta boi canh Poster, khong dung cho Matching';
