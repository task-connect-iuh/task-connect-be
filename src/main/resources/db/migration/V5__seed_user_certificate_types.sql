-- Seed Master Data cho user_certificate_types va user_category_certificate_requirements
-- (docs/PROGRESS-USER-MODULE.md Buoc 5). Danh sach chung chi cho tung nhom da duoc nguoi
-- dung chot truoc khi seed - moi nhom dich vu co 3 loai chung chi thay the nhau.
--
-- QUAN HE OR, khong phai AND: trong mot category, cac dong is_mandatory=TRUE la cac lua
-- chon thay the nhau (vd Dien dan dung chap nhan so cap HOAC trung cap HOAC bang ky su,
-- khong bat buoc co du ca ba). Buoc 6 (dang ky ky nang gop nop chung chi) chi can DUNG MOT
-- chung chi trong danh sach nay duoc Admin duyet la ho so ky nang cua category do chuyen
-- VERIFIED - khac cach hieu "chung chi bat buoc cuoi cung" da ghi nham truoc do trong
-- PROGRESS-USER-MODULE.md (da sua lai).
--
-- min_experience_years de 0 cho moi dong, cung ly do da ghi o V4: cho OQ-04 chot muc rieng
-- cho Dien lanh va Dien cong nghiep quy mo nho (docs/OPEN-QUESTIONS.md).

INSERT INTO `user_certificate_types` (`id`, `code`, `name`, `issuing_authority`, `description`, `is_active`)
VALUES
  -- Dien dan dung
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000101', '-', '')), 'SO_CAP_DIEN_DD',
   'Chứng chỉ sơ cấp nghề Điện dân dụng', 'Trường/trung tâm dạy nghề', NULL, TRUE),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000102', '-', '')), 'TRUNG_CAP_DIEN_DD',
   'Bằng trung cấp nghề Điện dân dụng', 'Trường trung cấp/cao đẳng nghề', NULL, TRUE),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000103', '-', '')), 'KY_SU_DIEN',
   'Bằng kỹ sư/cao đẳng Điện - Điện tử', 'Trường đại học/cao đẳng', NULL, TRUE),

  -- Dien lanh
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000104', '-', '')), 'SO_CAP_DIEN_LANH',
   'Chứng chỉ sơ cấp nghề Kỹ thuật máy lạnh và điều hoà không khí', 'Trường/trung tâm dạy nghề', NULL, TRUE),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000105', '-', '')), 'TRUNG_CAP_DIEN_LANH',
   'Bằng trung cấp/cao đẳng Kỹ thuật máy lạnh và điều hoà không khí', 'Trường trung cấp/cao đẳng nghề', NULL, TRUE),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000106', '-', '')), 'KY_SU_NHIET_LANH',
   'Bằng kỹ sư Nhiệt - Điện lạnh', 'Trường đại học', NULL, TRUE),

  -- Dien cong nghiep quy mo nho
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000107', '-', '')), 'TRUNG_CAP_DIEN_CN',
   'Bằng trung cấp/cao đẳng nghề Điện công nghiệp', 'Trường trung cấp/cao đẳng nghề', NULL, TRUE),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000108', '-', '')), 'KY_SU_DIEN_CN',
   'Bằng kỹ sư Điện công nghiệp / Tự động hoá', 'Trường đại học', NULL, TRUE),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000109', '-', '')), 'AN_TOAN_DIEN',
   'Chứng chỉ an toàn điện', 'Đơn vị huấn luyện được uỷ quyền', NULL, TRUE),

  -- Cap thoat nuoc
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000110', '-', '')), 'SO_CAP_CTN',
   'Chứng chỉ sơ cấp nghề Cấp thoát nước', 'Trường/trung tâm dạy nghề', NULL, TRUE),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000111', '-', '')), 'TRUNG_CAP_CTN',
   'Bằng trung cấp/cao đẳng Cấp thoát nước', 'Trường trung cấp/cao đẳng nghề', NULL, TRUE),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000112', '-', '')), 'KY_SU_CTN',
   'Bằng kỹ sư Cấp thoát nước - Môi trường nước', 'Trường đại học', NULL, TRUE),

  -- Lap dat va bao tri thiet bi nuoc
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000113', '-', '')), 'SO_CAP_TBN',
   'Chứng chỉ sơ cấp nghề lắp đặt, sửa chữa thiết bị nước', 'Trường/trung tâm dạy nghề', NULL, TRUE),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000114', '-', '')), 'TRUNG_CAP_TBN',
   'Bằng trung cấp Cấp thoát nước / Cơ khí liên quan', 'Trường trung cấp/cao đẳng nghề', NULL, TRUE),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000115', '-', '')), 'CHUNG_NHAN_HANG_SX',
   'Chứng chỉ đào tạo kỹ thuật từ hãng sản xuất thiết bị', 'Hãng sản xuất/nhà phân phối', NULL, TRUE);

-- category_id lay tu cac UUID co dinh da seed o V4__seed_user_service_categories.sql
-- (00000000-0000-4000-a000-000000000001..5 theo dung thu tu DIEN_DAN_DUNG, DIEN_LANH,
-- DIEN_CONG_NGHIEP_NHO, CAP_THOAT_NUOC, THIET_BI_NUOC).
INSERT INTO `user_category_certificate_requirements`
  (`id`, `category_id`, `certificate_type_id`, `is_mandatory`, `min_experience_years`)
VALUES
  -- Dien dan dung (category ...0001)
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000201', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000001', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000101', '-', '')), TRUE, 0),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000202', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000001', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000102', '-', '')), TRUE, 0),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000203', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000001', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000103', '-', '')), TRUE, 0),

  -- Dien lanh (category ...0002)
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000204', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000002', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000104', '-', '')), TRUE, 0),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000205', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000002', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000105', '-', '')), TRUE, 0),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000206', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000002', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000106', '-', '')), TRUE, 0),

  -- Dien cong nghiep quy mo nho (category ...0003)
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000207', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000003', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000107', '-', '')), TRUE, 0),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000208', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000003', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000108', '-', '')), TRUE, 0),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000209', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000003', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000109', '-', '')), TRUE, 0),

  -- Cap thoat nuoc (category ...0004)
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000210', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000004', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000110', '-', '')), TRUE, 0),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000211', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000004', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000111', '-', '')), TRUE, 0),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000212', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000004', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000112', '-', '')), TRUE, 0),

  -- Lap dat va bao tri thiet bi nuoc (category ...0005)
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000213', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000005', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000113', '-', '')), TRUE, 0),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000214', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000005', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000114', '-', '')), TRUE, 0),
  (UNHEX(REPLACE('00000000-0000-4000-a000-000000000215', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000005', '-', '')),
   UNHEX(REPLACE('00000000-0000-4000-a000-000000000115', '-', '')), TRUE, 0);
