-- Module User. Nhom bang tien to `user_` trong database `taskconnect`.
-- Ho so ca nhan, master data danh muc, KYC, chung chi.
-- Xem .claude/rules/00-architecture.md, .claude/rules/01-domain-glossary.md,
-- .claude/rules/12-database.md.

CREATE TABLE `user_profiles` (
  `id` BINARY(16) NOT NULL,
  `account_id` BINARY(16) NOT NULL,
  `full_name` VARCHAR(150) NOT NULL,
  `avatar_url` VARCHAR(500) NULL,
  `address_text` VARCHAR(500) NULL,
  `operating_area` VARCHAR(255) NOT NULL,
  `location_lat` DECIMAL(10,7) NULL,
  `location_lng` DECIMAL(10,7) NULL,
  `kyc_status` VARCHAR(20) NOT NULL DEFAULT 'NOT_SUBMITTED',
  `created_at` DATETIME(3) NOT NULL,
  `updated_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_profiles_account` (`account_id`),
  CONSTRAINT `ck_user_profiles_kyc_status`
    CHECK (`kyc_status` IN ('NOT_SUBMITTED', 'VERIFYING', 'VERIFIED', 'REJECTED')),
  CONSTRAINT `fk_user_profiles_account`
    FOREIGN KEY (`account_id`) REFERENCES `auth_accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Diem uy tin KHONG luu o day, doc tu review_reputation_scores';

CREATE TABLE `user_service_categories` (
  `id` BINARY(16) NOT NULL,
  `code` VARCHAR(50) NOT NULL,
  `name` VARCHAR(150) NOT NULL,
  `description` TEXT NULL COMMENT 'dung cho RAG',
  `keywords` TEXT NULL,
  `min_experience_years` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_service_categories_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='MASTER DATA - lai dong thoi RAG phan loai, bo loc Matching, dieu kien ung tuyen';

CREATE TABLE `user_certificate_types` (
  `id` BINARY(16) NOT NULL,
  `code` VARCHAR(50) NOT NULL,
  `name` VARCHAR(200) NOT NULL,
  `issuing_authority` VARCHAR(255) NULL,
  `description` TEXT NULL,
  `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_certificate_types_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_category_certificate_requirements` (
  `id` BINARY(16) NOT NULL,
  `category_id` BINARY(16) NOT NULL,
  `certificate_type_id` BINARY(16) NOT NULL,
  `is_mandatory` BOOLEAN NOT NULL DEFAULT TRUE,
  `min_experience_years` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_cat_cert_req_category_type` (`category_id`, `certificate_type_id`),
  CONSTRAINT `fk_user_cat_cert_req_category`
    FOREIGN KEY (`category_id`) REFERENCES `user_service_categories` (`id`),
  CONSTRAINT `fk_user_cat_cert_req_cert_type`
    FOREIGN KEY (`certificate_type_id`) REFERENCES `user_certificate_types` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_tasker_skill_profiles` (
  `id` BINARY(16) NOT NULL,
  `account_id` BINARY(16) NOT NULL,
  `category_id` BINARY(16) NOT NULL,
  `years_experience` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `price_min` BIGINT UNSIGNED NULL COMMENT 'don vi dong',
  `price_max` BIGINT UNSIGNED NULL COMMENT 'don vi dong',
  `verification_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `verified_at` DATETIME(3) NULL,
  `created_at` DATETIME(3) NOT NULL,
  `updated_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_tasker_skill_profiles_account_category` (`account_id`, `category_id`),
  KEY `idx_user_tasker_skill_profiles_category_status` (`category_id`, `verification_status`),
  CONSTRAINT `ck_user_tasker_skill_profiles_verification_status`
    CHECK (`verification_status` IN ('PENDING', 'VERIFIED', 'REJECTED')),
  CONSTRAINT `fk_user_tasker_skill_profiles_account`
    FOREIGN KEY (`account_id`) REFERENCES `auth_accounts` (`id`),
  CONSTRAINT `fk_user_tasker_skill_profiles_category`
    FOREIGN KEY (`category_id`) REFERENCES `user_service_categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Matching chi lay ban ghi VERIFIED';

CREATE TABLE `user_tasker_availability` (
  `id` BINARY(16) NOT NULL,
  `account_id` BINARY(16) NOT NULL,
  `day_of_week` TINYINT UNSIGNED NOT NULL COMMENT '1=Thu 2 ... 7=Chu nhat',
  `start_time` TIME NOT NULL,
  `end_time` TIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_tasker_availability_account` (`account_id`),
  CONSTRAINT `ck_user_tasker_availability_day_of_week`
    CHECK (`day_of_week` BETWEEN 1 AND 7),
  CONSTRAINT `fk_user_tasker_availability_account`
    FOREIGN KEY (`account_id`) REFERENCES `auth_accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_kyc_verifications` (
  `id` BINARY(16) NOT NULL,
  `account_id` BINARY(16) NOT NULL,
  `full_name_on_id` VARCHAR(150) NOT NULL,
  `id_number_enc` VARBINARY(512) NOT NULL COMMENT 'AES-256',
  `id_card_front_url_enc` VARBINARY(1024) NOT NULL,
  `id_card_back_url_enc` VARBINARY(1024) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'VERIFYING',
  `rejection_reason` VARCHAR(500) NULL,
  `reviewed_by_admin_id` BINARY(16) NULL,
  `reviewed_at` DATETIME(3) NULL,
  `submitted_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_kyc_verifications_account` (`account_id`),
  CONSTRAINT `ck_user_kyc_verifications_status`
    CHECK (`status` IN ('VERIFYING', 'VERIFIED', 'REJECTED')),
  CONSTRAINT `fk_user_kyc_verifications_account`
    FOREIGN KEY (`account_id`) REFERENCES `auth_accounts` (`id`),
  CONSTRAINT `fk_user_kyc_verifications_reviewed_by_admin`
    FOREIGN KEY (`reviewed_by_admin_id`) REFERENCES `auth_accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_tasker_certifications` (
  `id` BINARY(16) NOT NULL,
  `account_id` BINARY(16) NOT NULL,
  `category_id` BINARY(16) NOT NULL COMMENT 'moi nhom dich vu nop rieng 1 ban',
  `certificate_type_id` BINARY(16) NOT NULL,
  `certificate_number_enc` VARBINARY(512) NULL,
  `issuing_authority` VARCHAR(255) NULL,
  `issued_date` DATE NULL,
  `expiry_date` DATE NULL COMMENT 'NULL = vo thoi han',
  `file_url_enc` VARBINARY(1024) NOT NULL,
  `experience_proof_url` VARCHAR(500) NULL,
  `claimed_experience_years` TINYINT UNSIGNED NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW',
  `rejection_reason` VARCHAR(500) NULL,
  `reviewed_by_admin_id` BINARY(16) NULL,
  `reviewed_at` DATETIME(3) NULL,
  `submitted_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_tasker_certifications_account_category` (`account_id`, `category_id`),
  KEY `idx_user_tasker_certifications_expiry` (`expiry_date`),
  CONSTRAINT `ck_user_tasker_certifications_status`
    CHECK (`status` IN ('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'EXPIRED')),
  CONSTRAINT `fk_user_tasker_certifications_account`
    FOREIGN KEY (`account_id`) REFERENCES `auth_accounts` (`id`),
  CONSTRAINT `fk_user_tasker_certifications_reviewed_by_admin`
    FOREIGN KEY (`reviewed_by_admin_id`) REFERENCES `auth_accounts` (`id`),
  CONSTRAINT `fk_user_tasker_certifications_category`
    FOREIGN KEY (`category_id`) REFERENCES `user_service_categories` (`id`),
  CONSTRAINT `fk_user_tasker_certifications_cert_type`
    FOREIGN KEY (`certificate_type_id`) REFERENCES `user_certificate_types` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tu choi 1 nhom khong anh huong nhom khac';
