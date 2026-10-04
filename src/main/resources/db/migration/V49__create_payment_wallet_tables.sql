-- Module Payment, lan dau co code that (truoc day chi la dac ta o .claude/rules/14-payment-escrow.md).
-- Dot nay ("escrow-lite", quyet dinh nguoi dung 2026-10-02): nap tien la GIA LAP, luon thanh cong
-- ngay trong 1 transaction, chua goi Sepay hay cong thanh toan that nao - xem Javadoc
-- PaymentFacadeImpl. Vi/giao dich o day chi phuc vu escrow cua tung booking (UC11 chon phuong thuc
-- thanh toan), KHONG phai vi tong quat toan he thong (chua co nap/rut tu do, chua co man "Vi cua
-- toi") - se mo rong that khi module Payment duoc hien thuc day du.

CREATE TABLE `payment_wallets` (
  `id` BINARY(16) NOT NULL,
  `account_id` BINARY(16) NOT NULL,
  `balance` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'don vi dong, so du KHA DUNG (chua tru tam giu)',
  `created_at` DATETIME(3) NOT NULL,
  `updated_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `uq_payment_wallets_account` UNIQUE (`account_id`),
  CONSTRAINT `fk_payment_wallets_account` FOREIGN KEY (`account_id`) REFERENCES `auth_accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Vi gia lap theo tai khoan - tao lazy khi lan dau can toi, xem PaymentFacadeImpl.findOrCreateWallet';

-- Append-only (cung quy uoc voi task_price_history/task_edit_events) - KHONG bao gio UPDATE/DELETE
-- dong da ghi, moi lan doi balance phai kem dung 1 dong o day trong CUNG 1 transaction
-- (.claude/rules/14-payment-escrow.md quy tac 2/3).
CREATE TABLE `payment_wallet_transactions` (
  `id` BINARY(16) NOT NULL,
  `wallet_id` BINARY(16) NOT NULL,
  `type` VARCHAR(30) NOT NULL COMMENT 'DEPOSIT_SIMULATED | ESCROW_HOLD | ESCROW_TOPUP, xem WalletTransactionType',
  `amount` BIGINT NOT NULL COMMENT 'co dau: duong la tang balance, am la giam balance',
  `reference_booking_id` BINARY(16) NULL COMMENT 'booking lien quan, null neu giao dich khong gan booking nao',
  `note` VARCHAR(255) NULL,
  `created_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_payment_wallet_tx_type`
    CHECK (`type` IN ('DEPOSIT_SIMULATED', 'ESCROW_HOLD', 'ESCROW_TOPUP')),
  CONSTRAINT `fk_payment_wallet_tx_wallet` FOREIGN KEY (`wallet_id`) REFERENCES `payment_wallets` (`id`),
  KEY `idx_payment_wallet_tx_wallet` (`wallet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='So giao dich vi, chi ghi them - khong bao gio UPDATE/DELETE';
