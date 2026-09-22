-- Them cong tac cho Tasker tu chan loi moi truc tiep tu Poster (UC09 chong spam loi moi,
-- xem TaskConnect_Chat_ImplementationSpec.md muc 1 va 8). Mac dinh TRUE (cho phep) de khong
-- doi hanh vi cua Tasker hien co nao chua tung dong den cong tac nay.
ALTER TABLE `user_tasker_skill_profiles`
  ADD COLUMN `accepts_direct_invites` TINYINT(1) NOT NULL DEFAULT 1
    COMMENT 'FALSE = chan moi loi moi truc tiep (INVITED) toi Tasker nay cho category tuong ung'
    AFTER `verification_status`;
