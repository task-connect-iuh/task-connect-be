-- He thong doi quy uoc luu thoi gian tu UTC sang UTC+7 (Asia/Ho_Chi_Minh), xem
-- spring.jpa.properties.hibernate.jdbc.time_zone trong application.yml va
-- .claude/rules/12-database.md.
--
-- Cac ban ghi da ton tai truoc migration nay dang luu wall-clock theo UTC. Sau khi
-- doi hibernate.jdbc.time_zone sang Asia/Ho_Chi_Minh, Hibernate se doc lai chuoi wall-clock
-- cu nhu the no la gio UTC+7, lam moc thoi gian bi lui 7 tieng so voi gia tri that (anh
-- huong truc tiep expires_at, locked_until dang dung de tinh han). Cong them 7 tieng vao
-- moi cot DATETIME hien co de gia tri doc lai sau khi doi time_zone khop voi moc thoi gian
-- goc. Cac cot NULL khong bi anh huong (NULL + INTERVAL van la NULL).

UPDATE `auth_accounts`
   SET `locked_until` = `locked_until` + INTERVAL 7 HOUR,
       `last_login_at` = `last_login_at` + INTERVAL 7 HOUR,
       `created_at` = `created_at` + INTERVAL 7 HOUR,
       `updated_at` = `updated_at` + INTERVAL 7 HOUR;

UPDATE `auth_account_roles`
   SET `granted_at` = `granted_at` + INTERVAL 7 HOUR;

UPDATE `auth_refresh_tokens`
   SET `expires_at` = `expires_at` + INTERVAL 7 HOUR,
       `revoked_at` = `revoked_at` + INTERVAL 7 HOUR,
       `created_at` = `created_at` + INTERVAL 7 HOUR;

UPDATE `auth_email_verification_tokens`
   SET `expires_at` = `expires_at` + INTERVAL 7 HOUR,
       `used_at` = `used_at` + INTERVAL 7 HOUR,
       `created_at` = `created_at` + INTERVAL 7 HOUR;

UPDATE `auth_password_reset_tokens`
   SET `expires_at` = `expires_at` + INTERVAL 7 HOUR,
       `used_at` = `used_at` + INTERVAL 7 HOUR,
       `created_at` = `created_at` + INTERVAL 7 HOUR;

UPDATE `user_profiles`
   SET `created_at` = `created_at` + INTERVAL 7 HOUR,
       `updated_at` = `updated_at` + INTERVAL 7 HOUR;

UPDATE `user_tasker_skill_profiles`
   SET `verified_at` = `verified_at` + INTERVAL 7 HOUR,
       `created_at` = `created_at` + INTERVAL 7 HOUR,
       `updated_at` = `updated_at` + INTERVAL 7 HOUR;

UPDATE `user_kyc_verifications`
   SET `reviewed_at` = `reviewed_at` + INTERVAL 7 HOUR,
       `submitted_at` = `submitted_at` + INTERVAL 7 HOUR;

UPDATE `user_tasker_certifications`
   SET `reviewed_at` = `reviewed_at` + INTERVAL 7 HOUR,
       `submitted_at` = `submitted_at` + INTERVAL 7 HOUR;
