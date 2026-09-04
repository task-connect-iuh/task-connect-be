-- Them gia tri CANCELLED vao 3 state machine: KYC (user_kyc_verifications.status,
-- guong o user_profiles.kyc_status), chung chi hanh nghe (user_tasker_certifications.status),
-- ho so ky nang (user_tasker_skill_profiles.verification_status) - cho phep chinh chu tu huy
-- lan nop dang cho duyet cua minh (khac REJECTED - do Admin quyet dinh). Xem
-- .claude/rules/01-domain-glossary.md va KycVerificationService.cancel()/TaskerSkillService.cancel().
--
-- Cac cot nay la VARCHAR(20) + CHECK constraint (khong phai ENUM SQL that, xem V2 migration),
-- nen chi can DROP/ADD lai CHECK constraint, khong dung toi ALTER kieu cot, khong rebuild bang.

ALTER TABLE `user_profiles`
  DROP CONSTRAINT `ck_user_profiles_kyc_status`,
  ADD CONSTRAINT `ck_user_profiles_kyc_status`
    CHECK (`kyc_status` IN ('NOT_SUBMITTED', 'VERIFYING', 'VERIFIED', 'REJECTED', 'CANCELLED'));

ALTER TABLE `user_tasker_skill_profiles`
  DROP CONSTRAINT `ck_user_tasker_skill_profiles_verification_status`,
  ADD CONSTRAINT `ck_user_tasker_skill_profiles_verification_status`
    CHECK (`verification_status` IN ('PENDING', 'VERIFIED', 'REJECTED', 'CANCELLED'));

ALTER TABLE `user_kyc_verifications`
  DROP CONSTRAINT `ck_user_kyc_verifications_status`,
  ADD CONSTRAINT `ck_user_kyc_verifications_status`
    CHECK (`status` IN ('VERIFYING', 'VERIFIED', 'REJECTED', 'CANCELLED'));

ALTER TABLE `user_tasker_certifications`
  DROP CONSTRAINT `ck_user_tasker_certifications_status`,
  ADD CONSTRAINT `ck_user_tasker_certifications_status`
    CHECK (`status` IN ('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'EXPIRED', 'CANCELLED'));
