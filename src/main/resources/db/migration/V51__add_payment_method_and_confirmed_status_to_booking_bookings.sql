-- Fix-forward dung nhu Javadoc BookingStatus/V31 da du bao tu truoc: mo rong CHECK status them
-- CONFIRMED (quyet dinh nguoi dung 2026-10-02) - KHONG them COMPLETED/CANCELLED, giai ngan/huy
-- booking that van ngoai pham vi dot nay. Tu dot nay, GUARDRAIL 2 CLAUDE.md (khong booking nao
-- CONFIRMED neu chua giu tien thanh cong) thoa man duoc that su: BookingFacadeImpl.
-- createFromApplication() chi set CONFIRMED SAU KHI PaymentFacade.holdInitial() tra ve thanh cong.
ALTER TABLE `booking_bookings`
  DROP CONSTRAINT `ck_booking_bookings_status`;
ALTER TABLE `booking_bookings`
  ADD CONSTRAINT `ck_booking_bookings_status` CHECK (`status` IN ('PENDING_ESCROW', 'CONFIRMED'));

-- Phuong thuc thanh toan Poster chon o UC11 "Chon nguoi nay" (quyet dinh nguoi dung 2026-10-02):
-- FULL_ESCROW giu 100% qua he thong, FEE_ONLY_ESCROW chi giu 8% phi nen tang, 92% con lai hai ben
-- tu thanh toan ngoai he thong. DEFAULT 'FULL_ESCROW' chi de ALTER khong vo tren cac row da ton
-- tai (moi truong dev/test hien co) - khong con y nghia nghiep vu cho row moi, code Java luon set
-- tuong minh, khong bao gio dua vao DEFAULT nay.
ALTER TABLE `booking_bookings`
  ADD COLUMN `payment_method` VARCHAR(20) NOT NULL DEFAULT 'FULL_ESCROW' AFTER `fee_base_amount`;
ALTER TABLE `booking_bookings`
  ADD CONSTRAINT `ck_booking_bookings_payment_method`
    CHECK (`payment_method` IN ('FULL_ESCROW', 'FEE_ONLY_ESCROW'));
