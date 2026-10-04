-- Them tin nhan thoai (VOICE) - yeu cau nguoi dung 2026-09-26, rule da chot qua AskUserQuestion:
-- toi da 5 phut/15MB moi tin, cho thu hoi giong TEXT/IMAGE/FILE/VIDEO (trong 15 phut). Dung
-- CHUNG co che dinh kem voi IMAGE/FILE/VIDEO (van la 1 dong chat_message_attachments), chi khac
-- whitelist content-type (audio/webm, audio/mp4 - xem ChatAttachmentContentTypes).
ALTER TABLE `chat_messages`
  DROP CONSTRAINT `ck_chat_messages_message_type`,
  ADD CONSTRAINT `ck_chat_messages_message_type`
    CHECK (`message_type` IN ('TEXT', 'SYSTEM', 'PRICE_PROPOSAL', 'RESCHEDULE_PROPOSAL', 'IMAGE', 'FILE', 'VIDEO', 'VOICE'));

INSERT INTO `admin_system_parameters`
  (`id`, `param_key`, `param_value`, `description`, `created_at`, `updated_at`)
VALUES
  (UNHEX(REPLACE('00000000-0000-4000-b000-00000000000d', '-', '')), 'chat_voice_max_duration_seconds', '300',
   'Thoi luong toi da 1 tin nhan thoai (giay) - 5 phut', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  (UNHEX(REPLACE('00000000-0000-4000-b000-00000000000e', '-', '')), 'chat_voice_max_size_mb', '15',
   'Dung luong toi da 1 tin nhan thoai (MB)', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3));
