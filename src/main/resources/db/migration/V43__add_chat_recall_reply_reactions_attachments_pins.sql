-- Bo sung tinh nang chat (theo yeu cau nguoi dung 2026-09-26, rule da chot qua AskUserQuestion):
-- thu hoi tin nhan (trong 15 phut, an voi CA HAI ben), gui anh/nhieu anh/video/file, tra loi
-- (quote) 1 tin nhan, tha bieu tuong cam xuc, ghim toi da 3 tin/kenh (ca 2 ben deu ghim/bo ghim
-- duoc). Mo rong chat_messages hien co (V32) thay vi tao bang moi cho TEXT/IMAGE/FILE/VIDEO -
-- van la 1 luong tin nhan duy nhat trong kenh, chi khac nhau o attachment kem theo.

-- IMAGE/FILE/VIDEO: tin nhan dinh kem file, dung chung message_type nhu TEXT (co the co caption
-- rieng trong cot body da co san). recalled_at: thu hoi mem - gia tri khac NULL nghia la tin da
-- bi thu hoi, tang service se AN body/attachments/reactions khi tra ve, KHONG xoa du lieu that
-- (giu nguyen ban ghi de kiem toan, dung tinh than "cam xoa/ghi de" cua CLAUDE.md o muc file repo,
-- ap dung tuong tu cho du lieu nguoi dung). reply_to_message_id: tra loi (quote) 1 tin nhan khac
-- trong CUNG kenh, tu tham chieu chinh bang nay.
ALTER TABLE `chat_messages`
  DROP CONSTRAINT `ck_chat_messages_message_type`,
  ADD CONSTRAINT `ck_chat_messages_message_type`
    CHECK (`message_type` IN ('TEXT', 'SYSTEM', 'PRICE_PROPOSAL', 'RESCHEDULE_PROPOSAL', 'IMAGE', 'FILE', 'VIDEO')),
  ADD COLUMN `recalled_at` DATETIME(3) NULL
    COMMENT 'Khac NULL = tin da bi chinh nguoi gui thu hoi (trong han 15 phut) - an voi ca hai ben'
    AFTER `proposal_status`,
  ADD COLUMN `reply_to_message_id` BINARY(16) NULL
    COMMENT 'Tin nhan dang tra loi (quote) - cung kenh, co the la bat ky message_type nao tru SYSTEM'
    AFTER `recalled_at`,
  ADD CONSTRAINT `fk_chat_messages_reply_to`
    FOREIGN KEY (`reply_to_message_id`) REFERENCES `chat_messages` (`id`);

-- 1 tin nhan IMAGE/VIDEO/FILE co the co nhieu file dinh kem (gui nhieu anh 1 luc) - sort_order
-- giu dung thu tu nguoi dung da chon luc gui. storage_key la S3 object key (KHONG public-read,
-- prefix rieng "chat-attachments/{applicationId}/", giong mau private cua ADR-004 KYC - noi
-- dung trao doi giua 2 ben khong nen public vinh vien nhu avatar). Khong luu URL cong khai:
-- moi lan tra ve ChatMessageResponse, service tu ky lai 1 presigned GET URL ngan han.
CREATE TABLE `chat_message_attachments` (
  `id` BINARY(16) NOT NULL,
  `message_id` BINARY(16) NOT NULL,
  `storage_key` VARCHAR(500) NOT NULL COMMENT 'S3 object key, prefix chat-attachments/{applicationId}/',
  `file_name` VARCHAR(255) NOT NULL COMMENT 'Ten file goc client gui len, chi de hien thi/tai xuong',
  `mime_type` VARCHAR(100) NOT NULL,
  `file_size_bytes` BIGINT NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_chat_message_attachments_message`
    FOREIGN KEY (`message_id`) REFERENCES `chat_messages` (`id`),
  KEY `idx_chat_message_attachments_message` (`message_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='File dinh kem cua 1 tin nhan IMAGE/FILE/VIDEO, nhieu dong cho 1 message_id';

-- 1 tai khoan chi tha DUNG 1 emoji tren 1 tin nhan (UNIQUE message_id+account_id) - tha lai
-- emoji khac se GHI DE dong cu (updated_at), tha lai CUNG emoji se XOA dong (bo tha), xu ly o
-- tang service (xem ChatService.reactToMessage), khong the dien ta "toggle" bang CHECK constraint.
CREATE TABLE `chat_message_reactions` (
  `id` BINARY(16) NOT NULL,
  `message_id` BINARY(16) NOT NULL,
  `account_id` BINARY(16) NOT NULL,
  `emoji` VARCHAR(32) NOT NULL COMMENT 'Ky tu emoji unicode (co the la 1 grapheme nhieu code point)',
  `created_at` DATETIME(3) NOT NULL,
  `updated_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `uq_chat_message_reactions_message_account` UNIQUE (`message_id`, `account_id`),
  CONSTRAINT `fk_chat_message_reactions_message`
    FOREIGN KEY (`message_id`) REFERENCES `chat_messages` (`id`),
  CONSTRAINT `fk_chat_message_reactions_account`
    FOREIGN KEY (`account_id`) REFERENCES `auth_accounts` (`id`),
  KEY `idx_chat_message_reactions_message` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Toi da 1 reaction/tai khoan/tin nhan';

-- Toi da 3 tin ghim MOI KENH (chung cho ca 2 ben, khong phai rieng tung nguoi) - gioi han nay
-- KHONG the dien ta bang CHECK/UNIQUE, thuc thi o tang service bang khoa PESSIMISTIC_WRITE tren
-- chat_channels (xem ChatChannelRepository.findByIdForUpdate, cung mau voi
-- KycVerificationRepository.findByIdForUpdate) de tranh 2 ben ghim dong thoi vuot qua 3.
CREATE TABLE `chat_pinned_messages` (
  `id` BINARY(16) NOT NULL,
  `channel_id` BINARY(16) NOT NULL,
  `message_id` BINARY(16) NOT NULL,
  `pinned_by_account_id` BINARY(16) NOT NULL,
  `created_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `uq_chat_pinned_messages_channel_message` UNIQUE (`channel_id`, `message_id`),
  CONSTRAINT `fk_chat_pinned_messages_channel`
    FOREIGN KEY (`channel_id`) REFERENCES `chat_channels` (`id`),
  CONSTRAINT `fk_chat_pinned_messages_message`
    FOREIGN KEY (`message_id`) REFERENCES `chat_messages` (`id`),
  CONSTRAINT `fk_chat_pinned_messages_pinned_by`
    FOREIGN KEY (`pinned_by_account_id`) REFERENCES `auth_accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Toi da 3 dong/channel_id, thuc thi o tang service';

-- Nguong nghiep vu cho tinh nang chat moi (khong hardcode trong Java, xem 02-source-of-truth.md).
-- Cac gia tri ban dau la bo de xuat da duoc nguoi dung xac nhan (2026-09-26).
INSERT INTO `admin_system_parameters`
  (`id`, `param_key`, `param_value`, `description`, `created_at`, `updated_at`)
VALUES
  (UNHEX(REPLACE('00000000-0000-4000-b000-000000000004', '-', '')), 'chat_message_recall_window_minutes', '15',
   'So phut toi da sau khi gui de chinh nguoi gui con thu hoi duoc 1 tin nhan (TEXT/IMAGE/FILE/VIDEO)', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  (UNHEX(REPLACE('00000000-0000-4000-b000-000000000005', '-', '')), 'chat_max_pinned_messages_per_channel', '3',
   'So tin nhan toi da duoc ghim dong thoi trong 1 kenh chat', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  (UNHEX(REPLACE('00000000-0000-4000-b000-000000000006', '-', '')), 'chat_image_max_count_per_message', '10',
   'So anh toi da gui trong 1 lan (1 tin nhan IMAGE)', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  (UNHEX(REPLACE('00000000-0000-4000-b000-000000000007', '-', '')), 'chat_image_max_size_mb', '10',
   'Dung luong toi da 1 anh (MB)', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  (UNHEX(REPLACE('00000000-0000-4000-b000-000000000008', '-', '')), 'chat_video_max_count_per_message', '1',
   'So video toi da gui trong 1 lan (1 tin nhan VIDEO)', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  (UNHEX(REPLACE('00000000-0000-4000-b000-000000000009', '-', '')), 'chat_video_max_size_mb', '100',
   'Dung luong toi da 1 video (MB)', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  (UNHEX(REPLACE('00000000-0000-4000-b000-00000000000a', '-', '')), 'chat_video_max_duration_seconds', '300',
   'Thoi luong toi da 1 video (giay) - 5 phut', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  (UNHEX(REPLACE('00000000-0000-4000-b000-00000000000b', '-', '')), 'chat_file_max_count_per_message', '5',
   'So file tai lieu toi da gui trong 1 lan (1 tin nhan FILE)', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
  (UNHEX(REPLACE('00000000-0000-4000-b000-00000000000c', '-', '')), 'chat_file_max_size_mb', '20',
   'Dung luong toi da 1 file tai lieu (MB)', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3));
