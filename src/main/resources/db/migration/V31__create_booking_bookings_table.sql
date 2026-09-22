-- Module Booking, khoi tao toi gian ("booking-lite") - chi du de UC11 trong
-- TaskConnect_Chat_ImplementationSpec.md co mot booking row that de gan chat_channel va tinh
-- fee_base, KHONG phai module Booking day du (dieu phoi lich, huy, khieu nai...). Xem
-- .claude/rules/00-architecture.md (tien to booking_) va docs/PROGRESS-CHAT-MODULE.md.
--
-- CHECK status CO Y CHU CHI CHO PHEP 'PENDING_ESCROW' - KHONG PHAI THIEU SOT. GUARDRAIL 2 cua
-- CLAUDE.md: khong booking nao duoc CONFIRMED neu chua giu tien thanh cong, khong ngoai le. Vi
-- module Payment/escrow that chua ton tai, khong co code path nao trong dot nay duoc phep tao
-- booking CONFIRMED - CHECK constraint nay chu dong chan dieu do o tang DB. Mo rong CHECK them
-- CONFIRMED/COMPLETED/CANCELLED se lam bang mot migration moi (fix-forward) khi module Payment
-- that duoc hien thuc, KHONG duoc sua lai migration nay.

CREATE TABLE `booking_bookings` (
  `id` BINARY(16) NOT NULL,
  `application_id` BINARY(16) NOT NULL,
  `task_id` BINARY(16) NOT NULL,
  `poster_id` BINARY(16) NOT NULL,
  `tasker_id` BINARY(16) NOT NULL,
  `fee_base_amount` BIGINT UNSIGNED NOT NULL COMMENT 'don vi dong, xem dac ta muc 4 ve cach tinh',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING_ESCROW',
  `scheduled_at` DATETIME(3) NULL,
  `created_at` DATETIME(3) NOT NULL,
  `updated_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_booking_bookings_status`
    CHECK (`status` IN ('PENDING_ESCROW')),
  CONSTRAINT `uq_booking_bookings_application` UNIQUE (`application_id`),
  CONSTRAINT `fk_booking_bookings_application`
    FOREIGN KEY (`application_id`) REFERENCES `task_applications` (`id`),
  CONSTRAINT `fk_booking_bookings_task`
    FOREIGN KEY (`task_id`) REFERENCES `task_tasks` (`id`),
  CONSTRAINT `fk_booking_bookings_poster`
    FOREIGN KEY (`poster_id`) REFERENCES `auth_accounts` (`id`),
  CONSTRAINT `fk_booking_bookings_tasker`
    FOREIGN KEY (`tasker_id`) REFERENCES `auth_accounts` (`id`),
  KEY `idx_booking_bookings_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Booking-lite (chua co Payment/escrow that) - xem GUARDRAIL 2 CLAUDE.md';
