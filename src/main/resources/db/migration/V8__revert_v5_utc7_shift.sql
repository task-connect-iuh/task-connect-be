-- Nguoi dung doi y, quay lai luu UTC nhu ban dau (KHONG doi sang UTC+7 nua). Neu sau nay
-- can hien thi UTC+7 se convert o frontend, khong dong vao tang luu tru. Xem
-- .claude/rules/12-database.md va TimeConfig.java (da revert ve UTC).
--
-- V5 (installed_on = 2026-08-30 04:50:54, xem flyway_schema_history) da cong +7 gio vao
-- MOI dong ton tai tai thoi diem no chay. Nhung ngay sau do cau hinh Asia/Ho_Chi_Minh
-- chua bao gio thuc su co hieu luc (van ghi UTC, khong xac dinh duoc nguyen nhan trong
-- qua trinh debug), nen moi dong duoc tao SAU luc V5 chay van la UTC nguyen ban, KHONG
-- bi dich. Vi vay khong the hoan tac V5 bang mot phep tru 7 gio dong loat tren toan bang
-- -- lam vay se lam sai lech ca nhung dong da dung UTC tao sau V5.
--
-- Da doi chieu thu cong tung dong qua truy van truc tiep DB that (khong suy doan): trong
-- toan bo 9 bang V5 co dung, chi con dung 2 bang co dong sinh ra TRUOC luc V5 chay:
--   - auth_accounts: 2 tai khoan that (legiakhanhxman@gmail.com, khasnhlee142@gmail.com),
--     nhung chi rieng created_at cua 2 dong nay la bi V5 dich - updated_at/last_login_at
--     cua ca hai da duoc ghi lai SAU luc V5 chay (do dang nhap trong luc debug), dung UTC
--     nguyen ban, KHONG duoc dong vao.
--   - auth_account_roles: 4 dong granted_at gan cho 2 tai khoan tren.
-- Cac bang con lai ma V5 co dung (auth_refresh_tokens, auth_email_verification_tokens,
-- auth_password_reset_tokens, user_profiles, user_tasker_skill_profiles,
-- user_kyc_verifications, user_tasker_certifications) hoac dang rong, hoac chi con dong
-- sinh SAU luc V5 chay - da xac nhan tung dong, khong dong nao con sot lai tu truoc V5.
--
-- Nguong '2026-08-30 04:50:54' lay dung tu flyway_schema_history.installed_on cua V5.
-- Ap dung rieng tung cot (khong gop chung dieu kien theo dong) vi mot dong co the vua co
-- cot da bi V5 dich (created_at) vua co cot khac duoc ghi lai sau do (last_login_at).

UPDATE `auth_accounts`
   SET `created_at` = `created_at` - INTERVAL 7 HOUR
 WHERE `created_at` < '2026-08-30 04:50:54';

UPDATE `auth_accounts`
   SET `updated_at` = `updated_at` - INTERVAL 7 HOUR
 WHERE `updated_at` < '2026-08-30 04:50:54';

UPDATE `auth_accounts`
   SET `last_login_at` = `last_login_at` - INTERVAL 7 HOUR
 WHERE `last_login_at` < '2026-08-30 04:50:54';

UPDATE `auth_account_roles`
   SET `granted_at` = `granted_at` - INTERVAL 7 HOUR
 WHERE `granted_at` < '2026-08-30 04:50:54';
