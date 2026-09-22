-- Vá dữ liệu cho bug đã sửa 2026-09-22 (xem docs/PROGRESS-CHAT-MODULE.md): truoc day
-- ChatService.attachBooking() chi gan booking_id cho kenh chat DA TON TAI tai thoi diem
-- confirm() (UC11) - neu ung vien thang chua tung chat/de xuat gia truoc do (vd task co san
-- ngan sach, khong can thuong luong) thi kenh chua ton tai luc goi attachBooking(), lenh nay
-- la no-op, roi kenh moi duoc lazy-create SAU do boi notifyApplicationConfirmed() nhung
-- khong bao gio duoc gan booking_id. He qua: tab "Dang thuc hien" (dua tren
-- chat_channels.booking_id IS NOT NULL) bi rong du co booking that. TaskApplicationService.confirm()
-- da doi thu tu goi (notifyApplicationConfirmed() truoc, attachBooking() sau) de khong con xay
-- ra voi cac luot confirm MOI - migration nay chi vao lai du lieu CU da bi thieu booking_id.
UPDATE chat_channels cc
JOIN booking_bookings b ON b.application_id = cc.application_id
SET cc.booking_id = b.id
WHERE cc.booking_id IS NULL;
