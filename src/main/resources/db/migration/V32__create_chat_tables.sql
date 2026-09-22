-- Module Chat (UC16) - hoan toan moi, KHONG phai ALTER bang da co du dac ta
-- TaskConnect_Chat_ImplementationSpec.md ghi "bang da co" cho chat_messages (sai so voi thuc
-- te repo nay, xem docs/PROGRESS-CHAT-MODULE.md muc 0.1). Tien to bang chat_ theo
-- .claude/rules/00-architecture.md.

CREATE TABLE `chat_channels` (
  `id` BINARY(16) NOT NULL,
  `application_id` BINARY(16) NOT NULL,
  `booking_id` BINARY(16) NULL COMMENT 'gan khi application duoc chon o UC11 (Round B4)',
  `status` VARCHAR(10) NOT NULL DEFAULT 'OPEN',
  `created_at` DATETIME(3) NOT NULL
    COMMENT 'thoi diem tin nhan/de xuat DAU TIEN gui thanh cong (lazy-create) - KHONG phai luc mo man soan',
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_chat_channels_status`
    CHECK (`status` IN ('OPEN', 'CLOSED')),
  CONSTRAINT `uq_chat_channels_application` UNIQUE (`application_id`),
  CONSTRAINT `fk_chat_channels_application`
    FOREIGN KEY (`application_id`) REFERENCES `task_applications` (`id`),
  CONSTRAINT `fk_chat_channels_booking`
    FOREIGN KEY (`booking_id`) REFERENCES `booking_bookings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='1 application = toi da 1 channel, xem dac ta muc 1 va 2';

CREATE TABLE `chat_messages` (
  `id` BINARY(16) NOT NULL,
  `channel_id` BINARY(16) NOT NULL,
  `sender_account_id` BINARY(16) NULL COMMENT 'NULL cho message_type = SYSTEM',
  `message_type` VARCHAR(20) NOT NULL,
  `body` TEXT NULL COMMENT 'noi dung TEXT/SYSTEM, hoac ghi chu kem theo PRICE_PROPOSAL/RESCHEDULE_PROPOSAL',
  `ref_price_history_id` BINARY(16) NULL COMMENT 'chi co khi message_type = PRICE_PROPOSAL',
  `proposed_time` DATETIME(3) NULL COMMENT 'chi co khi message_type = RESCHEDULE_PROPOSAL',
  `proposal_status` VARCHAR(10) NULL COMMENT 'ap dung cho PRICE_PROPOSAL va RESCHEDULE_PROPOSAL',
  `created_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_chat_messages_message_type`
    CHECK (`message_type` IN ('TEXT', 'SYSTEM', 'PRICE_PROPOSAL', 'RESCHEDULE_PROPOSAL')),
  CONSTRAINT `ck_chat_messages_proposal_status`
    CHECK (`proposal_status` IN ('PROPOSED', 'ACCEPTED', 'REJECTED')),
  CONSTRAINT `fk_chat_messages_channel`
    FOREIGN KEY (`channel_id`) REFERENCES `chat_channels` (`id`),
  CONSTRAINT `fk_chat_messages_sender`
    FOREIGN KEY (`sender_account_id`) REFERENCES `auth_accounts` (`id`),
  CONSTRAINT `fk_chat_messages_price_history`
    FOREIGN KEY (`ref_price_history_id`) REFERENCES `task_price_history` (`id`),
  KEY `idx_chat_messages_channel_created` (`channel_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tin nhan/de xuat trong 1 channel, xem dac ta muc 1';
