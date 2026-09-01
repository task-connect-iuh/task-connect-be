-- Module User. Them truong gioi thieu ban than (bio) vao ho so ca nhan - hien thi cong khai
-- cung ho ten/khu vuc hoat dong khi nguoi khac xem ho so (PublicProfileResponse). Gioi han
-- 1000 ky tu: khong co business rule san trong docs/ dinh nghia do dai, day la suy luan ky
-- thuat hop ly cho mot doan gioi thieu ngan, khong phai gia tri da duoc nguoi dung chot.
ALTER TABLE `user_profiles`
  ADD COLUMN `bio` VARCHAR(1000) NULL AFTER `address_text`;
