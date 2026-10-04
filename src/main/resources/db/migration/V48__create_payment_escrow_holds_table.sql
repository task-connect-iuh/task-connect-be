-- 1 ban ghi tam giu that cho 1 booking (UC11 chon phuong thuc thanh toan, quyet dinh nguoi dung
-- 2026-10-02). poster_id/tasker_id duoc SAO CHEP (denormalize) tu Booking luc tao - Payment
-- khong duoc goi dong bo nguoc ve Booking (chi Booking -> Payment la dong bo,
-- .claude/rules/10-module-boundary.md), nen can co san o day de khong phai hoi lai.
--
-- CHECK status CHI CHO PHEP 'HELD' - KHONG PHAI THIEU SOT, cung tinh than voi BookingStatus
-- (xem V31). Dot nay nap tien la gia lap va luon thanh cong ngay nen PENDING khong bao gio quan
-- sat duoc; giai ngan/hoan tien (RELEASED/REFUNDED/FAILED) NGOAI PHAM VI dot nay (chua co luong
-- Poster xac nhan hoan thanh cong viec) - se mo rong CHECK nay bang 1 migration fix-forward rieng
-- khi lam giai ngan that.

CREATE TABLE `payment_escrow_holds` (
  `id` BINARY(16) NOT NULL,
  `booking_id` BINARY(16) NOT NULL,
  `poster_id` BINARY(16) NOT NULL COMMENT 'denormalized tu Booking luc tao',
  `tasker_id` BINARY(16) NOT NULL COMMENT 'denormalized tu Booking luc tao',
  `held_amount` BIGINT UNSIGNED NOT NULL COMMENT 'tong dang tam giu hien tai, don vi dong',
  `status` VARCHAR(20) NOT NULL DEFAULT 'HELD',
  `created_at` DATETIME(3) NOT NULL,
  `updated_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_payment_escrow_holds_status` CHECK (`status` IN ('HELD')),
  CONSTRAINT `uq_payment_escrow_holds_booking` UNIQUE (`booking_id`),
  CONSTRAINT `fk_payment_escrow_holds_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking_bookings` (`id`),
  CONSTRAINT `fk_payment_escrow_holds_poster` FOREIGN KEY (`poster_id`) REFERENCES `auth_accounts` (`id`),
  CONSTRAINT `fk_payment_escrow_holds_tasker` FOREIGN KEY (`tasker_id`) REFERENCES `auth_accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tien tam giu that cho 1 booking - chi dung HELD dot nay, xem GUARDRAIL 2 CLAUDE.md va EscrowHoldStatus';
