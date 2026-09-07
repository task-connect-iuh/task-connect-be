-- V11 dinh them CANCELLED vao 4 CHECK constraint nhung dung chung 1 cau ALTER TABLE
-- DROP CONSTRAINT x, ADD CONSTRAINT x (cung ten) - MariaDB coi day la "khong doi gi" va
-- am tham bo qua phan ADD, du statement chay khong loi va Flyway ghi nhan success. Hau qua:
-- cot status/kyc_status/verification_status van bi khoa o danh sach cu, thieu CANCELLED,
-- nen KycVerificationService.cancel()/TaskerSkillService.cancel() nem
-- DataIntegrityViolationException khi UPDATE. Fix: tach DROP va ADD thanh 2 cau ALTER
-- TABLE rieng cho tung bang, da kiem chung truc tiep tren DB la co hieu luc that.

ALTER TABLE `user_profiles`
  DROP CONSTRAINT `ck_user_profiles_kyc_status`;
ALTER TABLE `user_profiles`
  ADD CONSTRAINT `ck_user_profiles_kyc_status`
    CHECK (`kyc_status` IN ('NOT_SUBMITTED', 'VERIFYING', 'VERIFIED', 'REJECTED', 'CANCELLED'));

ALTER TABLE `user_tasker_skill_profiles`
  DROP CONSTRAINT `ck_user_tasker_skill_profiles_verification_status`;
ALTER TABLE `user_tasker_skill_profiles`
  ADD CONSTRAINT `ck_user_tasker_skill_profiles_verification_status`
    CHECK (`verification_status` IN ('PENDING', 'VERIFIED', 'REJECTED', 'CANCELLED'));

ALTER TABLE `user_kyc_verifications`
  DROP CONSTRAINT `ck_user_kyc_verifications_status`;
ALTER TABLE `user_kyc_verifications`
  ADD CONSTRAINT `ck_user_kyc_verifications_status`
    CHECK (`status` IN ('VERIFYING', 'VERIFIED', 'REJECTED', 'CANCELLED'));

ALTER TABLE `user_tasker_certifications`
  DROP CONSTRAINT `ck_user_tasker_certifications_status`;
ALTER TABLE `user_tasker_certifications`
  ADD CONSTRAINT `ck_user_tasker_certifications_status`
    CHECK (`status` IN ('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'EXPIRED', 'CANCELLED'));
