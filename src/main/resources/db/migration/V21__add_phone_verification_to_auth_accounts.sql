-- Them coc phone_verified_at: NULL nghia la so dien thoai (neu co) chua duoc xac minh qua
-- Firebase Phone Auth. Di kem voi cot phone hien co - AuthService.updatePhone() luon ghi ca
-- hai cung luc tu sau migration nay, khong con duong ghi phone ma khong xac minh.
ALTER TABLE `auth_accounts`
  ADD COLUMN `phone_verified_at` DATETIME(3) NULL AFTER `phone`;
