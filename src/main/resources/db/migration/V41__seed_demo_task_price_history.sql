-- Seed du lieu demo cho tinh nang "Goi y muc gia" luc dang viec (TaskPriceSuggestionService,
-- xem docs/PROGRESS-TASK-POSTER-MODULE.md). Tap du lieu that hien gan nhu khong co (do an,
-- chua co giao dich that qua Booking/Payment) - yeu cau nguoi dung: seed them de co gi test
-- truoc khi co du lieu that.
--
-- 40 task (8/category x 5 category), moi task gan 1 task_application da ACCEPTED + 1 dong
-- task_price_history da Dong y (accepted_at khac NULL) - day la nguon du lieu duy nhat
-- TaskPriceSuggestionService dung de goi y (xem Javadoc class, KHONG can Booking hoan tat).
-- Poster/Tasker tai su dung tu V36__seed_demo_matching_data.sql (khong tao tai khoan moi):
-- poster01..15 (UUID prefix b002), tasker01..60 (UUID prefix b001). Toa do dung chung 1 diem
-- trung tam TP. Ho Chi Minh (khong quan trong cho tinh nang nay, chi can hop le NOT NULL).
--
-- Gia va mo ta duoc viet tay (khong dung generator AWK nhu V36 - quy mo 40 dong khong can),
-- da dang tu ngu trong tung category de embedding Gemini phan biet duoc, muc gia hop ly theo
-- do phuc tap tung viec (dot 1 chua kiem chung bang du lieu that, se dieu chinh khi co du lieu
-- that thay the dan seed nay - xem TaskPriceSuggestionProperties.similarityThreshold).

-- ===================== NHOM 1: DIEN DAN DUNG (category a000-...0001) =====================
INSERT INTO `task_tasks`
  (`id`, `poster_id`, `category_id`, `title`, `description`, `address_text`, `lat`, `lng`,
   `budget_amount`, `estimated_workers_needed`, `status`, `supplies_status`, `created_at`, `updated_at`)
VALUES
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000001', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000001', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000001', '-', '')), 'Sửa ổ cắm điện bị chập', 'Ổ cắm điện trong phòng khách bị chập cháy, cần thay mới và kiểm tra đường dây.', 'Quận 1, TP. Hồ Chí Minh', 10.7769, 106.7009, 200000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000002', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000002', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000001', '-', '')), 'Lắp đặt thêm ổ cắm điện', 'Cần lắp thêm 3 ổ cắm điện trong phòng ngủ, đã có sẵn dây nguồn từ tủ điện.', 'Quận 1, TP. Hồ Chí Minh', 10.7769, 106.7009, 350000, 1, 'ASSIGNED', 'FULL', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000003', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000003', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000001', '-', '')), 'Sửa công tắc đèn không lên', 'Công tắc đèn phòng bếp bấm không lên, nghi do hỏng công tắc hoặc đứt dây.', 'Quận 1, TP. Hồ Chí Minh', 10.7769, 106.7009, 150000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000004', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000004', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000001', '-', '')), 'Thay aptomat tổng bị nhảy liên tục', 'Aptomat tổng nhà hay nhảy khi bật nhiều thiết bị cùng lúc, cần kiểm tra và thay mới.', 'Quận 1, TP. Hồ Chí Minh', 10.7769, 106.7009, 400000, 1, 'ASSIGNED', 'PARTIAL', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000005', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000005', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000001', '-', '')), 'Lắp đèn chiếu sáng sân vườn', 'Cần lắp 4 bóng đèn LED chiếu sáng khu vực sân vườn, đã có sẵn đường dây điện.', 'Quận 1, TP. Hồ Chí Minh', 10.7769, 106.7009, 450000, 1, 'ASSIGNED', 'FULL', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000006', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000006', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000001', '-', '')), 'Sửa quạt trần kêu to khi chạy', 'Quạt trần phòng khách kêu to bất thường khi bật, nghi do bạc đạn mòn.', 'Quận 1, TP. Hồ Chí Minh', 10.7769, 106.7009, 180000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000007', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000007', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000001', '-', '')), 'Kiểm tra và sửa đường dây điện cũ', 'Nhà cũ có đường dây điện âm tường nghi bị chuột cắn, cần kiểm tra toàn bộ và sửa chữa.', 'Quận 1, TP. Hồ Chí Minh', 10.7769, 106.7009, 500000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000008', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000008', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000001', '-', '')), 'Lắp đặt đèn cảm ứng hành lang', 'Muốn lắp đèn cảm ứng chuyển động cho hành lang tầng 2, chưa có sẵn ổ cắm gần đó.', 'Quận 1, TP. Hồ Chí Minh', 10.7769, 106.7009, 300000, 1, 'ASSIGNED', 'PARTIAL', NOW(3), NOW(3)),

-- ===================== NHOM 2: DIEN LANH (category a000-...0002) =====================
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000009', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000009', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000002', '-', '')), 'Vệ sinh máy lạnh treo tường', 'Máy lạnh treo tường phòng ngủ đã 1 năm chưa vệ sinh, chạy yếu và có mùi hôi.', 'Quận 3, TP. Hồ Chí Minh', 10.7829, 106.6934, 250000, 1, 'ASSIGNED', 'FULL', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000010', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000010', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000002', '-', '')), 'Sửa máy lạnh không lạnh', 'Máy lạnh bật lên có gió nhưng không lạnh, nghi hết gas hoặc block yếu.', 'Quận 3, TP. Hồ Chí Minh', 10.7829, 106.6934, 500000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000011', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000011', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000002', '-', '')), 'Lắp đặt máy lạnh mới', 'Cần lắp đặt 1 máy lạnh inverter 1.5HP mới mua cho phòng khách, đã có sẵn ống đồng.', 'Quận 3, TP. Hồ Chí Minh', 10.7829, 106.6934, 600000, 1, 'ASSIGNED', 'FULL', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000012', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000012', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000002', '-', '')), 'Sửa tủ lạnh không đông đá', 'Tủ lạnh ngăn đá không đông, ngăn mát vẫn mát bình thường, nghi hỏng quạt dàn lạnh.', 'Quận 3, TP. Hồ Chí Minh', 10.7829, 106.6934, 400000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000013', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000013', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000002', '-', '')), 'Bảo trì định kỳ điều hòa văn phòng', 'Văn phòng nhỏ có 3 máy điều hòa cần bảo trì vệ sinh định kỳ hàng quý.', 'Quận 3, TP. Hồ Chí Minh', 10.7829, 106.6934, 700000, 1, 'ASSIGNED', 'FULL', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000014', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000014', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000002', '-', '')), 'Sửa tủ đông không lạnh sâu', 'Tủ đông cửa hàng không đạt độ lạnh sâu như trước, nghi hỏng gas hoặc block.', 'Quận 3, TP. Hồ Chí Minh', 10.7829, 106.6934, 800000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000015', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000015', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000002', '-', '')), 'Di dời máy lạnh sang phòng khác', 'Cần tháo và lắp lại máy lạnh cũ sang phòng ngủ khác trong cùng nhà.', 'Quận 3, TP. Hồ Chí Minh', 10.7829, 106.6934, 450000, 1, 'ASSIGNED', 'FULL', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000016', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000001', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000002', '-', '')), 'Sửa máy lạnh chảy nước', 'Máy lạnh treo tường bị chảy nước xuống sàn khi chạy, nghi tắc ống thoát nước.', 'Quận 3, TP. Hồ Chí Minh', 10.7829, 106.6934, 220000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),

-- ===================== NHOM 3: DIEN CONG NGHIEP NHO (category a000-...0003) =====================
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000017', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000002', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000003', '-', '')), 'Sửa tủ điện xưởng nhỏ', 'Tủ điện xưởng may nhỏ bị nhảy aptomat liên tục khi chạy máy may đồng loạt.', 'Quận Bình Tân, TP. Hồ Chí Minh', 10.7649, 106.6027, 600000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000018', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000003', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000003', '-', '')), 'Lắp đặt motor bơm nước công nghiệp', 'Cần lắp đặt 1 motor bơm nước 3 pha cho hệ thống tưới của xưởng.', 'Quận Bình Tân, TP. Hồ Chí Minh', 10.7649, 106.6027, 900000, 1, 'ASSIGNED', 'FULL', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000019', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000004', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000003', '-', '')), 'Kiểm tra hệ thống điện 3 pha cửa hàng', 'Cửa hàng tạp hóa mở rộng cần kiểm tra lại toàn bộ hệ thống điện 3 pha mới lắp.', 'Quận Bình Tân, TP. Hồ Chí Minh', 10.7649, 106.6027, 700000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000020', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000005', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000003', '-', '')), 'Sửa động cơ điện xưởng cơ khí', 'Động cơ điện máy tiện trong xưởng cơ khí chạy yếu, nghi cháy cuộn dây.', 'Quận Bình Tân, TP. Hồ Chí Minh', 10.7649, 106.6027, 1200000, 1, 'ASSIGNED', 'PARTIAL', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000021', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000006', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000003', '-', '')), 'Lắp đặt hệ thống chiếu sáng nhà xưởng', 'Cần lắp hệ thống đèn LED công nghiệp cho nhà xưởng diện tích khoảng 200m2.', 'Quận Bình Tân, TP. Hồ Chí Minh', 10.7649, 106.6027, 1500000, 1, 'ASSIGNED', 'FULL', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000022', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000007', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000003', '-', '')), 'Bảo trì tủ điện tổng công ty nhỏ', 'Công ty nhỏ cần bảo trì định kỳ tủ điện tổng, kiểm tra các mối nối và aptomat.', 'Quận Bình Tân, TP. Hồ Chí Minh', 10.7649, 106.6027, 500000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000023', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000008', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000003', '-', '')), 'Sửa máy bơm công nghiệp không chạy', 'Máy bơm nước công nghiệp trong xưởng không khởi động được, nghi hỏng rơ le.', 'Quận Bình Tân, TP. Hồ Chí Minh', 10.7649, 106.6027, 550000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000024', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000009', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000003', '-', '')), 'Lắp đặt điện 3 pha cho xưởng mới', 'Xưởng gỗ mới mở cần lắp đặt hệ thống điện 3 pha cho các máy cưa và máy bào.', 'Quận Bình Tân, TP. Hồ Chí Minh', 10.7649, 106.6027, 1400000, 1, 'ASSIGNED', 'FULL', NOW(3), NOW(3)),

-- ===================== NHOM 4: CAP THOAT NUOC (category a000-...0004) =====================
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000025', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000010', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000004', '-', '')), 'Sửa vòi nước bị rò rỉ trong bếp', 'Vòi nước bồn rửa chén trong bếp bị rò rỉ ở chân vòi, cần thay gioăng hoặc vòi mới.', 'Quận 7, TP. Hồ Chí Minh', 10.7343, 106.7218, 200000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000026', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000011', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000004', '-', '')), 'Thông tắc bồn cầu nhà vệ sinh', 'Bồn cầu bị nghẹt, xả nước không trôi, đã thử dùng thông bồn cầu thường nhưng không hết.', 'Quận 7, TP. Hồ Chí Minh', 10.7343, 106.7218, 250000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000027', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000012', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000004', '-', '')), 'Sửa ống nước bị vỡ dưới sàn', 'Phát hiện nước rò rỉ từ sàn nhà tắm, nghi ống nước âm sàn bị vỡ.', 'Quận 7, TP. Hồ Chí Minh', 10.7343, 106.7218, 700000, 1, 'ASSIGNED', 'PARTIAL', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000028', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000013', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000004', '-', '')), 'Lắp đặt bồn rửa mặt mới', 'Cần lắp đặt 1 bồn rửa mặt mới trong nhà tắm, đã tháo bồn cũ sẵn.', 'Quận 7, TP. Hồ Chí Minh', 10.7343, 106.7218, 350000, 1, 'ASSIGNED', 'FULL', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000029', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000014', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000004', '-', '')), 'Thông tắc cống thoát nước sân', 'Cống thoát nước sân sau bị tắc, nước đọng lại mỗi khi mưa.', 'Quận 7, TP. Hồ Chí Minh', 10.7343, 106.7218, 300000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000030', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000015', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000004', '-', '')), 'Sửa máy bơm nước gia đình yếu', 'Máy bơm nước gia đình chạy yếu, áp lực nước lên tầng 3 rất yếu.', 'Quận 7, TP. Hồ Chí Minh', 10.7343, 106.7218, 400000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000031', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000001', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000004', '-', '')), 'Chống thấm tường nhà vệ sinh', 'Tường nhà vệ sinh bị thấm nước loang lổ, cần xử lý chống thấm lại.', 'Quận 7, TP. Hồ Chí Minh', 10.7343, 106.7218, 600000, 1, 'ASSIGNED', 'PARTIAL', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000032', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000002', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000004', '-', '')), 'Lắp đặt đường ống cấp nước mới', 'Cần lắp thêm đường ống cấp nước cho khu vực sân thượng mới xây.', 'Quận 7, TP. Hồ Chí Minh', 10.7343, 106.7218, 450000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),

-- ===================== NHOM 5: THIET BI NUOC (category a000-...0005) =====================
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000033', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000003', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000005', '-', '')), 'Sửa bình nóng lạnh không nóng', 'Bình nóng lạnh bật lên nhưng nước vẫn lạnh, nghi hỏng thanh nhiệt.', 'Quận Gò Vấp, TP. Hồ Chí Minh', 10.8386, 106.6652, 350000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000034', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000004', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000005', '-', '')), 'Lắp đặt máy lọc nước RO mới', 'Cần lắp đặt 1 máy lọc nước RO mới mua cho gian bếp, đã có sẵn nguồn nước.', 'Quận Gò Vấp, TP. Hồ Chí Minh', 10.8386, 106.6652, 400000, 1, 'ASSIGNED', 'FULL', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000035', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000005', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000005', '-', '')), 'Sửa vòi sen bị yếu nước', 'Vòi sen phòng tắm phun nước rất yếu so với trước, nghi tắc lưới lọc.', 'Quận Gò Vấp, TP. Hồ Chí Minh', 10.8386, 106.6652, 150000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000036', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000006', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000005', '-', '')), 'Bảo trì máy lọc nước định kỳ', 'Máy lọc nước gia đình dùng đã 6 tháng, cần thay lõi lọc và bảo trì định kỳ.', 'Quận Gò Vấp, TP. Hồ Chí Minh', 10.8386, 106.6652, 300000, 1, 'ASSIGNED', 'FULL', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000037', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000007', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000005', '-', '')), 'Lắp đặt bình nóng lạnh mới', 'Cần lắp đặt 1 bình nóng lạnh 20 lít mới cho phòng tắm chính.', 'Quận Gò Vấp, TP. Hồ Chí Minh', 10.8386, 106.6652, 500000, 1, 'ASSIGNED', 'FULL', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000038', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000008', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000005', '-', '')), 'Sửa thiết bị vệ sinh bị lỏng', 'Bồn cầu bị lỏng chân đế, lung lay khi ngồi, cần siết chặt lại hoặc thay ron.', 'Quận Gò Vấp, TP. Hồ Chí Minh', 10.8386, 106.6652, 200000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000039', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000009', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000005', '-', '')), 'Lắp đặt vòi sen tăng áp', 'Muốn lắp thêm vòi sen tăng áp cho phòng tắm vì áp lực nước hiện tại yếu.', 'Quận Gò Vấp, TP. Hồ Chí Minh', 10.8386, 106.6652, 450000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3)),
  (UNHEX(REPLACE('00000000-0000-4000-c001-000000000040', '-', '')), UNHEX(REPLACE('00000000-0000-4000-b002-000000000010', '-', '')), UNHEX(REPLACE('00000000-0000-4000-a000-000000000005', '-', '')), 'Bảo trì bình nóng lạnh cũ', 'Bình nóng lạnh dùng 3 năm chưa bảo trì, muốn kiểm tra và vệ sinh thanh nhiệt.', 'Quận Gò Vấp, TP. Hồ Chí Minh', 10.8386, 106.6652, 250000, 1, 'ASSIGNED', 'UNKNOWN', NOW(3), NOW(3));

-- 1 task_application ACCEPTED cho moi task tren, tasker xoay vong tasker01..40 (trung UUID
-- prefix b001-000000000001..40, tat ca da co san trong V36, khong can gia tri > 40 vi 40 task
-- vua du khop mot-mot voi 40 tasker dau tien cho don gian - danh sach 60 tasker seed van con
-- du cho cac tinh nang khac).
INSERT INTO `task_applications`
  (`id`, `task_id`, `tasker_id`, `proposed_arrival_text`, `status`, `proposed_price`, `initiated_by`, `created_at`, `responded_at`)
SELECT
  UNHEX(REPLACE(CONCAT('00000000-0000-4000-c002-', LPAD(n, 12, '0')), '-', '')),
  UNHEX(REPLACE(CONCAT('00000000-0000-4000-c001-', LPAD(n, 12, '0')), '-', '')),
  UNHEX(REPLACE(CONCAT('00000000-0000-4000-b001-', LPAD(n, 12, '0')), '-', '')),
  'Trong tuần này',
  'ACCEPTED',
  t.budget_amount,
  'TASKER',
  NOW(3),
  NOW(3)
FROM (
  SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
  UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
  UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
  UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
  UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25
  UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
  UNION ALL SELECT 31 UNION ALL SELECT 32 UNION ALL SELECT 33 UNION ALL SELECT 34 UNION ALL SELECT 35
  UNION ALL SELECT 36 UNION ALL SELECT 37 UNION ALL SELECT 38 UNION ALL SELECT 39 UNION ALL SELECT 40
) AS seq
JOIN `task_tasks` t
  ON t.id = UNHEX(REPLACE(CONCAT('00000000-0000-4000-c001-', LPAD(seq.n, 12, '0')), '-', ''));

-- 1 dong task_price_history DA DUOC DONG Y (accepted_at khac NULL) cho moi application tren -
-- day la dong du lieu duy nhat TaskPriceSuggestionService doc de goi y gia. Nguoi tao de xuat
-- la Tasker, nguoi Dong y la Poster (dung tasker_id/poster_id da gan o task_tasks/task_applications).
INSERT INTO `task_price_history`
  (`id`, `application_id`, `change_type`, `amount`, `created_by_account_id`, `created_at`, `accepted_by_account_id`, `accepted_at`)
SELECT
  UNHEX(REPLACE(CONCAT('00000000-0000-4000-c003-', LPAD(seq.n, 12, '0')), '-', '')),
  a.id,
  'INITIAL_AGREEMENT',
  a.proposed_price,
  a.tasker_id,
  NOW(3),
  t.poster_id,
  NOW(3)
FROM (
  SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
  UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
  UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
  UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
  UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25
  UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
  UNION ALL SELECT 31 UNION ALL SELECT 32 UNION ALL SELECT 33 UNION ALL SELECT 34 UNION ALL SELECT 35
  UNION ALL SELECT 36 UNION ALL SELECT 37 UNION ALL SELECT 38 UNION ALL SELECT 39 UNION ALL SELECT 40
) AS seq
JOIN `task_applications` a
  ON a.id = UNHEX(REPLACE(CONCAT('00000000-0000-4000-c002-', LPAD(seq.n, 12, '0')), '-', ''))
JOIN `task_tasks` t
  ON t.id = a.task_id;
