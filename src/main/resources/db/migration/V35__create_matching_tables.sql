-- Module Matching. Nhom bang tien to `matching_` trong database `taskconnect` - module nay
-- truoc gio khong so huu bang rieng (xem .claude/rules/00-architecture.md), lan dau co bang
-- that de phuc vu tinh nang "Tasker goi y" (goi y AI + luong Poster moi truc tiep Tasker,
-- song song voi task_applications/UC10) - xem task-connect-claude plan da duyet
-- "AI Tasker Suggestion".
--
-- matching_tasker_invites: mirror task_applications (V19) nhung theo chieu nguoc lai (Poster
-- chu dong moi, khong phai Tasker tu ung tuyen).
CREATE TABLE `matching_tasker_invites` (
  `id` BINARY(16) NOT NULL,
  `task_id` BINARY(16) NOT NULL,
  `tasker_id` BINARY(16) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `created_at` DATETIME(3) NOT NULL,
  `responded_at` DATETIME(3) NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_matching_tasker_invites_status`
    CHECK (`status` IN ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED')),
  CONSTRAINT `uq_matching_tasker_invites_task_tasker` UNIQUE (`task_id`, `tasker_id`),
  CONSTRAINT `fk_matching_tasker_invites_task`
    FOREIGN KEY (`task_id`) REFERENCES `task_tasks` (`id`),
  CONSTRAINT `fk_matching_tasker_invites_tasker`
    FOREIGN KEY (`tasker_id`) REFERENCES `auth_accounts` (`id`),
  KEY `idx_matching_tasker_invites_task` (`task_id`),
  KEY `idx_matching_tasker_invites_tasker` (`tasker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Loi moi Poster gui truc tiep cho Tasker - song song voi task_applications (UC10)';

-- Cache embedding vector cua ho so mot Tasker cho MOT category (bio + facts ky nang), dung
-- cho buoc rerank ngu nghia. vector luu duoi dang chuoi JSON cua mang float trong cot TEXT -
-- MariaDB hien tai khong co kieu vector rieng, va quy mo brute-force cosine trong Java la du
-- cho quy mo do an (xem plan da duyet, phan "Vector DB that: dung cho UC06, KHONG dung cho
-- matching").
CREATE TABLE `matching_tasker_embeddings` (
  `id` BINARY(16) NOT NULL,
  `account_id` BINARY(16) NOT NULL,
  `category_id` BINARY(16) NOT NULL,
  `vector` TEXT NOT NULL COMMENT 'JSON array cua mang float, vd [0.1,0.2,...]',
  `model` VARCHAR(100) NOT NULL COMMENT 'ten/phien ban model da sinh vector nay',
  `updated_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `uq_matching_tasker_embeddings_account_category` UNIQUE (`account_id`, `category_id`),
  CONSTRAINT `fk_matching_tasker_embeddings_account`
    FOREIGN KEY (`account_id`) REFERENCES `auth_accounts` (`id`),
  CONSTRAINT `fk_matching_tasker_embeddings_category`
    FOREIGN KEY (`category_id`) REFERENCES `user_service_categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Cache embedding ho so Tasker theo category, tinh lazy lan dau can dung';

-- Cache embedding vector cua mo ta mot Task, tinh mot lan va tai su dung cho moi lan goi y
-- Tasker cua task do. task_id la khoa chinh - moi Task chi co dung mot cache.
CREATE TABLE `matching_task_embedding_cache` (
  `task_id` BINARY(16) NOT NULL,
  `vector` TEXT NOT NULL COMMENT 'JSON array cua mang float, vd [0.1,0.2,...]',
  `model` VARCHAR(100) NOT NULL COMMENT 'ten/phien ban model da sinh vector nay',
  `created_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`task_id`),
  CONSTRAINT `fk_matching_task_embedding_cache_task`
    FOREIGN KEY (`task_id`) REFERENCES `task_tasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Cache embedding mo ta Task, tinh lazy lan dau can dung';
