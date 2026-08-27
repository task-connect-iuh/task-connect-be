-- Seed Master Data cho user_service_categories - dung 5 nhom dich vu da chot trong
-- .claude/rules/01-domain-glossary.md (GUARDRAIL muc 4, khong duoc sinh danh muc ngoai
-- pham vi dien-nuoc). description/keywords la kho tri thuc RAG (xem 12-database.md muc
-- "Mot nguon su that"), duoc dung boi module AI khi phan loai cong viec (chua hien thuc).
--
-- min_experience_years de 0 (mac dinh cot) cho ca 5 nhom: muc kinh nghiem toi thieu that
-- cho Dien lanh va Dien cong nghiep quy mo nho (hai nhom "dac thu" theo glossary) CHUA
-- duoc chot - xem docs/OPEN-QUESTIONS.md OQ-04. Cap nhat lai bang UPDATE trong migration
-- moi (cam sua migration nay) khi OQ-04 duoc chot, khong tu doan so.

INSERT INTO `user_service_categories`
  (`id`, `code`, `name`, `description`, `keywords`, `min_experience_years`, `is_active`)
VALUES
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000001', '-', '')), 'DIEN_DAN_DUNG', 'Điện dân dụng',
   'Sửa chữa, lắp đặt hệ thống điện trong nhà ở dân dụng: ổ cắm, công tắc, đường dây, aptomat, đèn chiếu sáng, quạt trần.',
   'điện nhà, sửa điện, lắp đặt điện, ổ cắm, công tắc, aptomat, đường dây điện, đèn, quạt trần, chập điện, mất điện',
   0, TRUE),

  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000002', '-', '')), 'DIEN_LANH', 'Điện lạnh',
   'Lắp đặt, vệ sinh, bảo trì và sửa chữa máy lạnh, điều hòa, tủ lạnh, tủ đông.',
   'máy lạnh, điều hòa, tủ lạnh, tủ đông, vệ sinh máy lạnh, bơm gas, sửa điều hòa, lắp máy lạnh',
   0, TRUE),

  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000003', '-', '')), 'DIEN_CONG_NGHIEP_NHO', 'Điện công nghiệp quy mô nhỏ',
   'Lắp đặt, bảo trì hệ thống điện công nghiệp quy mô nhỏ: tủ điện, động cơ, máy bơm công nghiệp, hệ thống điện xưởng nhỏ, cửa hàng.',
   'điện công nghiệp, tủ điện, động cơ điện, máy bơm công nghiệp, điện 3 pha, điện xưởng',
   0, TRUE),

  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000004', '-', '')), 'CAP_THOAT_NUOC', 'Cấp thoát nước',
   'Sửa chữa, lắp đặt hệ thống cấp nước và thoát nước: đường ống, bồn cầu, bồn rửa, máy bơm nước, chống thấm, thông tắc cống.',
   'sửa ống nước, thông tắc, thoát nước, cấp nước, bồn cầu, bồn rửa, máy bơm nước, rò rỉ nước, chống thấm',
   0, TRUE),

  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000005', '-', '')), 'THIET_BI_NUOC', 'Lắp đặt và bảo trì thiết bị nước',
   'Lắp đặt, bảo trì thiết bị dùng nước: máy nước nóng, bình nóng lạnh, máy lọc nước, vòi sen, thiết bị vệ sinh.',
   'máy nước nóng, bình nóng lạnh, máy lọc nước, vòi sen, thiết bị vệ sinh, lắp đặt thiết bị nước',
   0, TRUE);
