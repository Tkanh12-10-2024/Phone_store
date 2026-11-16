-- ============================================================================
-- MIGRATION & DỮ LIỆU MẪU BỔ SUNG CHO UC-23 ĐẾN UC-28
-- (Quản lý khuyến mãi, Áp dụng khuyến mãi POS, Thống kê & Lập báo cáo)
-- Tương thích MySQL 8.0+ trên CSDL quanly_cuahang_dienthoai
-- ============================================================================

CREATE DATABASE IF NOT EXISTS quanly_cuahang_dienthoai
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE quanly_cuahang_dienthoai;

-- ----------------------------------------------------------------------------
-- 1. Mở rộng bảng khuyen_mai cho UC-23 & UC-24
-- ----------------------------------------------------------------------------
ALTER TABLE khuyen_mai
  MODIFY COLUMN loai_giam_gia ENUM('Phan_tram', 'Tien_mat', 'Qua_tang') NOT NULL DEFAULT 'Phan_tram',
  ADD COLUMN IF NOT EXISTS qua_tang VARCHAR(255) NULL COMMENT 'Tên quà tặng / combo kèm theo' AFTER giam_toi_da,
  ADD COLUMN IF NOT EXISTS dong_may_ap_dung VARCHAR(100) NULL COMMENT 'VD: iPhone 15 Series, Galaxy S24 Series' AFTER ma_thuong_hieu_ap_dung,
  ADD COLUMN IF NOT EXISTS ma_danh_muc_ap_dung INT NULL COMMENT 'NULL nếu áp dụng mọi danh mục' AFTER dong_may_ap_dung,
  ADD COLUMN IF NOT EXISTS doi_tuong_khach_hang ENUM('Tat_ca', 'Khach_moi', 'Thanh_vien', 'Khach_VIP') NOT NULL DEFAULT 'Tat_ca' AFTER ma_danh_muc_ap_dung,
  ADD COLUMN IF NOT EXISTS mo_ta VARCHAR(500) NULL AFTER doi_tuong_khach_hang,
  ADD COLUMN IF NOT EXISTS ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER trang_thai;

-- ----------------------------------------------------------------------------
-- 2. Mở rộng bảng nhat_ky_bao_cao cho UC-28
-- ----------------------------------------------------------------------------
ALTER TABLE nhat_ky_bao_cao
  MODIFY COLUMN loai_bao_cao ENUM('Doanh_thu', 'Kiem_ke_kho', 'SanPham_BanChay', 'Ton_kho', 'Tong_hop', 'Bao_hanh') NOT NULL,
  ADD COLUMN IF NOT EXISTS tieu_chi_loc VARCHAR(500) NULL COMMENT 'Bộ lọc báo cáo đã áp dụng' AFTER duong_dan_file;

-- ----------------------------------------------------------------------------
-- 3. Cập nhật / Tạo Views hỗ trợ UC-25, UC-26, UC-27
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_canh_bao_ton_kho AS
SELECT
    sp.ma_san_pham,
    sp.ten_san_pham,
    th.ten_thuong_hieu,
    sp.dong_may,
    sp.mau_sac,
    sp.ram,
    sp.rom,
    sp.so_luong_ton,
    sp.dinh_muc_ton_toi_thieu,
    sp.gia_nhap_tham_khao,
    sp.gia_ban,
    CASE
        WHEN sp.so_luong_ton = 0 THEN 'Hết hàng'
        WHEN sp.so_luong_ton <= sp.dinh_muc_ton_toi_thieu THEN 'Sắp hết hàng'
        ELSE 'Bình thường'
    END AS muc_do_canh_bao
FROM san_pham sp
JOIN thuong_hieu th ON sp.ma_thuong_hieu = th.ma_thuong_hieu
WHERE sp.trang_thai = 'Dang_kinh_doanh';

CREATE OR REPLACE VIEW v_top_san_pham_ban_chay AS
SELECT
    sp.ma_san_pham,
    sp.ten_san_pham,
    th.ten_thuong_hieu,
    sp.dong_may,
    sp.mau_sac,
    sp.ram,
    sp.rom,
    COUNT(cthd.ma_chi_tiet_hd) AS so_luong_da_ban,
    COALESCE(SUM(cthd.don_gia), 0) AS tong_doanh_thu_san_pham,
    sp.so_luong_ton AS ton_kho_hien_tai
FROM san_pham sp
JOIN thuong_hieu th ON sp.ma_thuong_hieu = th.ma_thuong_hieu
LEFT JOIN chi_tiet_hoa_don cthd ON sp.ma_san_pham = cthd.ma_san_pham
LEFT JOIN hoa_don hd ON cthd.ma_hoa_don = hd.ma_hoa_don AND hd.trang_thai_thanh_toan = 'Thanh_cong'
GROUP BY sp.ma_san_pham, sp.ten_san_pham, th.ten_thuong_hieu, sp.dong_may, sp.mau_sac, sp.ram, sp.rom, sp.so_luong_ton;

CREATE OR REPLACE VIEW v_thong_ke_doanh_thu AS
SELECT
    DATE(hd.ngay_lap) AS ngay,
    COUNT(DISTINCT hd.ma_hoa_don) AS so_hoa_don,
    COALESCE(SUM(hd.tong_tien_hang), 0) AS tong_tien_hang_truoc_giam,
    COALESCE(SUM(hd.tien_giam_gia), 0) AS tong_chiet_khau_khuyen_mai,
    COALESCE(SUM(hd.tien_thue_vat), 0) AS tong_vat,
    COALESCE(SUM(hd.tong_thanh_toan), 0) AS doanh_thu_thuc_thu
FROM hoa_don hd
WHERE hd.trang_thai_thanh_toan = 'Thanh_cong'
GROUP BY DATE(hd.ngay_lap);

-- ----------------------------------------------------------------------------
-- 4. Dữ liệu mẫu phong phú cho UC-23..UC-28 (Idempotent)
-- ----------------------------------------------------------------------------

-- 4.1 Sản phẩm bổ sung để test cảnh báo hết hàng & tồn đọng (>60 ngày)
INSERT INTO san_pham (ma_san_pham, ten_san_pham, ma_thuong_hieu, ma_danh_muc, dong_may, mau_sac, ram, rom, man_hinh, chip_xu_ly, pin, camera, he_dieu_hanh, gia_nhap_tham_khao, gia_ban, thoi_gian_bao_hanh_thang, so_luong_ton, dinh_muc_ton_toi_thieu, trang_thai)
VALUES
('SP_OPPO_FINDN3', 'OPPO Find N3 5G 512GB Vàng Champange', 4, 4, 'Find N Series', 'Vàng', '16GB', '512GB', 'AMOLED 7.8 inch 120Hz', 'Snapdragon 8 Gen 2', '4805 mAh', '48MP + 64MP + 48MP', 'Android 14', 32000000, 38990000, 12, 4, 2, 'Dang_kinh_doanh'),
('SP_XIAOMI_14U_OUT', 'Xiaomi 14 Ultra 512GB Đen Da', 3, 1, 'Xiaomi 14 Series', 'Đen', '16GB', '512GB', 'AMOLED 6.73 inch 120Hz', 'Snapdragon 8 Gen 3', '5000 mAh', '50MP Leica Quad', 'Android 14', 25000000, 29990000, 12, 0, 2, 'Dang_kinh_doanh')
ON DUPLICATE KEY UPDATE ten_san_pham = VALUES(ten_san_pham);

-- Điều chỉnh định mức & tồn kho một số máy hiện hữu để hiển thị đầy đủ trạng thái cảnh báo
UPDATE san_pham SET so_luong_ton = 1, dinh_muc_ton_toi_thieu = 3 WHERE ma_san_pham = 'SP_IP15PM_512DEN';
UPDATE san_pham SET so_luong_ton = 2, dinh_muc_ton_toi_thieu = 4 WHERE ma_san_pham = 'SP_SS_ZFLIP5_256';
UPDATE san_pham SET so_luong_ton = 12, dinh_muc_ton_toi_thieu = 3 WHERE ma_san_pham = 'SP_IP15PM_256TN';
UPDATE san_pham SET so_luong_ton = 8, dinh_muc_ton_toi_thieu = 3 WHERE ma_san_pham = 'SP_SS_S24U_256AM';
UPDATE san_pham SET so_luong_ton = 10, dinh_muc_ton_toi_thieu = 3 WHERE ma_san_pham = 'SP_MI_14_256TRANG';
UPDATE san_pham SET so_luong_ton = 5, dinh_muc_ton_toi_thieu = 2 WHERE ma_san_pham = 'SP_OPPO_RENO11_256';

-- Phiếu nhập cũ (> 75 ngày) cho sản phẩm tồn đọng & phiếu nhập mới cho sản phẩm hiện hành
INSERT IGNORE INTO phieu_nhap (ma_phieu_nhap, ma_nha_cung_cap, ma_nhan_vien, ngay_nhap, tong_tien, da_thanh_toan, trang_thai, ghi_chu) VALUES
('PN_OLD_2026_01', 'NCC03', 'NV004', DATE_SUB(NOW(), INTERVAL 78 DAY), 128000000, 128000000, 'Hoan_thanh', 'Nhập lô OPPO Find N3 tồn kho lâu (>60 ngày)'),
('PN_NOV_2026_04', 'NCC01', 'NV004', DATE_SUB(NOW(), INTERVAL 3 DAY), 106000000, 106000000, 'Hoan_thanh', 'Bổ sung iPhone 15 Pro Max & Galaxy S24 Ultra');

INSERT IGNORE INTO chi_tiet_phieu_nhap (ma_phieu_nhap, ma_san_pham, so_luong, don_gia_nhap) VALUES
('PN_OLD_2026_01', 'SP_OPPO_FINDN3', 4, 32000000),
('PN_NOV_2026_04', 'SP_IP15PM_256TN', 4, 26500000);

-- Thêm IMEI tồn kho & IMEI nhập >75 ngày phục vụ POS và Thống kê tồn kho lâu
INSERT IGNORE INTO imei_san_pham (ma_imei, ma_san_pham, ma_phieu_nhap, trang_thai, ngay_nhap) VALUES
('358941091234566', 'SP_IP15PM_256TN', 'PN_NOV_2026_04', 'Trong_kho', DATE_SUB(NOW(), INTERVAL 3 DAY)),
('358941091234567', 'SP_IP15PM_256TN', 'PN_NOV_2026_04', 'Trong_kho', DATE_SUB(NOW(), INTERVAL 3 DAY)),
('358941091234568', 'SP_IP15PM_256TN', 'PN_NOV_2026_04', 'Trong_kho', DATE_SUB(NOW(), INTERVAL 3 DAY)),
('358941099900111', 'SP_IP15PM_512DEN', 'PN20261001_01', 'Trong_kho', DATE_SUB(NOW(), INTERVAL 25 DAY)),
('864219082345675', 'SP_SS_S24U_256AM', 'PN20261005_02', 'Trong_kho', DATE_SUB(NOW(), INTERVAL 20 DAY)),
('864219082345676', 'SP_SS_S24U_256AM', 'PN20261005_02', 'Trong_kho', DATE_SUB(NOW(), INTERVAL 20 DAY)),
('864219088800221', 'SP_SS_ZFLIP5_256', 'PN20261005_02', 'Trong_kho', DATE_SUB(NOW(), INTERVAL 68 DAY)),
('864219088800222', 'SP_SS_ZFLIP5_256', 'PN20261005_02', 'Trong_kho', DATE_SUB(NOW(), INTERVAL 68 DAY)),
('865123073456784', 'SP_MI_14_256TRANG', 'PN20261010_03', 'Trong_kho', DATE_SUB(NOW(), INTERVAL 15 DAY)),
('869000112233401', 'SP_OPPO_FINDN3', 'PN_OLD_2026_01', 'Trong_kho', DATE_SUB(NOW(), INTERVAL 78 DAY)),
('869000112233402', 'SP_OPPO_FINDN3', 'PN_OLD_2026_01', 'Trong_kho', DATE_SUB(NOW(), INTERVAL 78 DAY)),
('869000112233403', 'SP_OPPO_FINDN3', 'PN_OLD_2026_01', 'Trong_kho', DATE_SUB(NOW(), INTERVAL 78 DAY)),
('869000112233404', 'SP_OPPO_FINDN3', 'PN_OLD_2026_01', 'Trong_kho', DATE_SUB(NOW(), INTERVAL 78 DAY)),
('869000556677801', 'SP_OPPO_RENO11_256', 'PN20261010_03', 'Trong_kho', DATE_SUB(NOW(), INTERVAL 10 DAY));

-- 4.2 Khuyến mãi đầy đủ kịch bản UC-23 & UC-24
UPDATE khuyen_mai
SET mo_ta = 'Giảm trực tiếp 500.000đ cho mọi đơn hàng từ 20 triệu',
    doi_tuong_khach_hang = 'Tat_ca',
    ngay_ket_thuc = DATE_ADD(NOW(), INTERVAL 45 DAY),
    trang_thai = 'Dang_dien_ra'
WHERE ma_khuyen_mai = 'KM_CHAOTHU2026';

UPDATE khuyen_mai
SET dong_may_ap_dung = 'iPhone 15 Series',
    doi_tuong_khach_hang = 'Tat_ca',
    mo_ta = 'Giảm 5% tối đa 1.5 triệu áp dụng cho các dòng Apple iPhone',
    ngay_ket_thuc = DATE_ADD(NOW(), INTERVAL 30 DAY),
    trang_thai = 'Dang_dien_ra'
WHERE ma_khuyen_mai = 'KM_APPLE_VIP';

INSERT INTO khuyen_mai (ma_khuyen_mai, code_voucher, ten_khuyen_mai, loai_giam_gia, gia_tri_giam, gia_tri_don_hang_toi_thieu, giam_toi_da, qua_tang, so_luong_phat_hanh, so_luong_da_dung, ngay_bat_dau, ngay_ket_thuc, ma_thuong_hieu_ap_dung, dong_may_ap_dung, ma_danh_muc_ap_dung, doi_tuong_khach_hang, mo_ta, trang_thai)
VALUES
('KM_FLASH_12H', 'FLASH12H', 'Flash Sale cuối tuần - Sắp hết hạn < 24h', 'Phan_tram', 8, 10000000, 1200000, NULL, 100, 25, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 14 HOUR), NULL, NULL, NULL, 'Tat_ca', 'Cảnh báo sắp hết hạn dưới 24 giờ', 'Dang_dien_ra'),
('KM_VIP_SAPHET', 'VIPSAP97', 'Ưu đãi Khách VIP - Số lượng sắp hết < 5%', 'Tien_mat', 1200000, 15000000, NULL, NULL, 100, 97, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_ADD(NOW(), INTERVAL 15 DAY), NULL, NULL, NULL, 'Khach_VIP', 'Chỉ còn 3% số lượng voucher khả dụng', 'Dang_dien_ra'),
('KM_COMBO_PK', 'GIFTSAC20W', 'Combo Tặng Sạc Nhanh 20W + Cường lực', 'Qua_tang', 450000, 10000000, 450000, 'Củ sạc nhanh PD 20W + Kính cường lực KingKong', 200, 18, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 25 DAY), NULL, NULL, NULL, 'Tat_ca', 'Tặng combo phụ kiện trị giá 450.000đ cho đơn từ 10 triệu', 'Dang_dien_ra'),
('KM_EXPIRED_25', 'HET_HAN_2025', 'Khuyến mãi Tết cũ đã hết hạn', 'Tien_mat', 300000, 5000000, NULL, NULL, 50, 50, '2025-12-01 00:00:00', '2026-01-31 23:59:59', NULL, NULL, NULL, 'Tat_ca', 'Voucher test trường hợp hết hạn ở POS', 'Het_han')
ON DUPLICATE KEY UPDATE ngay_ket_thuc = VALUES(ngay_ket_thuc), so_luong_da_dung = VALUES(so_luong_da_dung), trang_thai = VALUES(trang_thai);

-- 4.3 Đơn hàng giữ chỗ ('Cho_thanh_toan') để hiển thị số lượng giữ chỗ trong UC-27
INSERT IGNORE INTO don_hang (ma_don_hang, ma_khach_hang, ma_nhan_vien, ma_khuyen_mai, ngay_tao, tong_tien_hang, tien_giam_gia, thue_vat, tong_tien_can_tra, trang_thai, ghi_chu) VALUES
('DH_GIUCHO_001', 'KH003', 'NV003', NULL, NOW(), 57980000, 0, 0, 57980000, 'Cho_thanh_toan', 'Khách đặt cọc giữ máy chờ nhận tại quầy');

INSERT IGNORE INTO chi_tiet_don_hang (ma_don_hang, ma_san_pham, so_luong, don_gia) VALUES
('DH_GIUCHO_001', 'SP_IP15PM_256TN', 1, 29990000),
('DH_GIUCHO_001', 'SP_SS_S24U_256AM', 1, 27990000);

-- 4.4 Đơn hàng & Hóa đơn bổ sung across Tháng 9, 10, 11/2026 để biểu đồ doanh thu (UC-25) và Top bán chạy (UC-26) sinh động
INSERT IGNORE INTO don_hang (ma_don_hang, ma_khach_hang, ma_nhan_vien, ma_khuyen_mai, ngay_tao, tong_tien_hang, tien_giam_gia, thue_vat, tong_tien_can_tra, trang_thai, ghi_chu) VALUES
('DH20260918_001', 'KH001', 'NV003', 'KM_CHAOTHU2026', '2026-09-18 11:00:00', 22990000, 500000, 0, 22490000, 'Da_thanh_toan', 'Bán Xiaomi 14 tháng 9'),
('DH20261025_003', 'KH004', 'NV006', 'KM_APPLE_VIP', '2026-10-25 16:20:00', 34990000, 1500000, 0, 33490000, 'Da_thanh_toan', 'Bán iPhone 15 PM 512GB'),
('DH20261105_004', 'KH002', 'NV003', 'KM_CHAOTHU2026', DATE_SUB(NOW(), INTERVAL 7 DAY), 29990000, 500000, 0, 29490000, 'Da_thanh_toan', 'Đơn bán đầu tháng 11'),
('DH20261110_005', 'KH003', 'NV006', NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), 33980000, 0, 0, 33980000, 'Da_thanh_toan', 'Bán Xiaomi 14 + OPPO Reno11'),
('DH20261112_006', 'KH004', 'NV003', 'KM_APPLE_VIP', DATE_SUB(NOW(), INTERVAL 1 DAY), 29990000, 1499500, 0, 28490500, 'Da_thanh_toan', 'Khách VIP mua iPhone 15 Pro Max'),
('DH20261113_007', 'KH001', 'NV003', 'KM_COMBO_PK', NOW(), 27990000, 0, 0, 27990000, 'Da_thanh_toan', 'Giao dịch trong ngày hiện tại');

INSERT IGNORE INTO chi_tiet_don_hang (ma_don_hang, ma_san_pham, so_luong, don_gia) VALUES
('DH20260918_001', 'SP_MI_14_256TRANG', 1, 22990000),
('DH20261025_003', 'SP_IP15PM_512DEN', 1, 34990000),
('DH20261105_004', 'SP_IP15PM_256TN', 1, 29990000),
('DH20261110_005', 'SP_MI_14_256TRANG', 1, 22990000),
('DH20261110_005', 'SP_OPPO_RENO11_256', 1, 10990000),
('DH20261112_006', 'SP_IP15PM_256TN', 1, 29990000),
('DH20261113_007', 'SP_SS_S24U_256AM', 1, 27990000);

INSERT IGNORE INTO hoa_don (ma_hoa_don, ma_don_hang, ma_khach_hang, ma_nhan_vien, ngay_lap, tong_tien_hang, thue_vat_phan_tram, tien_thue_vat, tien_giam_gia, tong_thanh_toan, phuong_thuc_thanh_toan, tien_khach_dua, tien_thua, trang_thai_thanh_toan, ghi_chu) VALUES
('HD20260918_001', 'DH20260918_001', 'KH001', 'NV003', '2026-09-18 11:05:00', 22990000, 0, 0, 500000, 22490000, 'Chuyen_khoan_QR', 22490000, 0, 'Thanh_cong', 'Thanh toán QR tháng 9'),
('HD20261025_003', 'DH20261025_003', 'KH004', 'NV006', '2026-10-25 16:25:00', 34990000, 0, 0, 1500000, 33490000, 'The', 33490000, 0, 'Thanh_cong', 'Thanh toán thẻ cuối tháng 10'),
('HD20261105_004', 'DH20261105_004', 'KH002', 'NV003', DATE_SUB(NOW(), INTERVAL 7 DAY), 29990000, 0, 0, 500000, 29490000, 'Tien_mat', 30000000, 510000, 'Thanh_cong', 'Khách trả tiền mặt'),
('HD20261110_005', 'DH20261110_005', 'KH003', 'NV006', DATE_SUB(NOW(), INTERVAL 3 DAY), 33980000, 0, 0, 0, 33980000, 'Chuyen_khoan_QR', 33980000, 0, 'Thanh_cong', 'Đơn combo 2 máy'),
('HD20261112_006', 'DH20261112_006', 'KH004', 'NV003', DATE_SUB(NOW(), INTERVAL 1 DAY), 29990000, 0, 0, 1499500, 28490500, 'Chuyen_khoan_QR', 28490500, 0, 'Thanh_cong', 'Khách VIP Apple'),
('HD20261113_007', 'DH20261113_007', 'KH001', 'NV003', NOW(), 27990000, 0, 0, 0, 27990000, 'Tien_mat', 28000000, 10000, 'Thanh_cong', 'Đơn hôm nay tặng kèm combo sạc 20W');

-- Thêm IMEI đã bán gắn với các hóa đơn mẫu trên
INSERT IGNORE INTO imei_san_pham (ma_imei, ma_san_pham, ma_phieu_nhap, ma_hoa_don, trang_thai, ngay_nhap, ngay_ban, han_bao_hanh) VALUES
('865123073456791', 'SP_MI_14_256TRANG', 'PN20261010_03', 'HD20260918_001', 'Da_ban', '2026-09-10 09:00:00', '2026-09-18 11:05:00', '2027-09-18'),
('358941099900199', 'SP_IP15PM_512DEN', 'PN20261001_01', 'HD20261025_003', 'Da_ban', '2026-10-01 09:30:00', '2026-10-25 16:25:00', '2027-10-25'),
('358941091234592', 'SP_IP15PM_256TN', 'PN20261001_01', 'HD20261105_004', 'Da_ban', '2026-10-01 09:30:00', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 12 MONTH)),
('865123073456792', 'SP_MI_14_256TRANG', 'PN20261010_03', 'HD20261110_005', 'Da_ban', '2026-10-10 10:00:00', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 12 MONTH)),
('869000556677892', 'SP_OPPO_RENO11_256', 'PN20261010_03', 'HD20261110_005', 'Da_ban', '2026-10-10 10:00:00', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 12 MONTH)),
('358941091234593', 'SP_IP15PM_256TN', 'PN_NOV_2026_04', 'HD20261112_006', 'Da_ban', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 12 MONTH)),
('864219082345692', 'SP_SS_S24U_256AM', 'PN20261005_02', 'HD20261113_007', 'Da_ban', '2026-10-05 14:15:00', NOW(), DATE_ADD(CURDATE(), INTERVAL 12 MONTH));

INSERT IGNORE INTO chi_tiet_hoa_don (ma_hoa_don, ma_san_pham, ma_imei, don_gia, thoi_gian_bao_hanh_thang) VALUES
('HD20260918_001', 'SP_MI_14_256TRANG', '865123073456791', 22990000, 12),
('HD20261025_003', 'SP_IP15PM_512DEN', '358941099900199', 34990000, 12),
('HD20261105_004', 'SP_IP15PM_256TN', '358941091234592', 29990000, 12),
('HD20261110_005', 'SP_MI_14_256TRANG', '865123073456792', 22990000, 12),
('HD20261110_005', 'SP_OPPO_RENO11_256', '869000556677892', 10990000, 12),
('HD20261112_006', 'SP_IP15PM_256TN', '358941091234593', 29990000, 12),
('HD20261113_007', 'SP_SS_S24U_256AM', '864219082345692', 27990000, 12);

-- Khôi phục tồn kho chính xác sau trigger mẫu nếu cần
UPDATE san_pham SET so_luong_ton = 12 WHERE ma_san_pham = 'SP_IP15PM_256TN';
UPDATE san_pham SET so_luong_ton = 1 WHERE ma_san_pham = 'SP_IP15PM_512DEN';
UPDATE san_pham SET so_luong_ton = 8 WHERE ma_san_pham = 'SP_SS_S24U_256AM';
UPDATE san_pham SET so_luong_ton = 2 WHERE ma_san_pham = 'SP_SS_ZFLIP5_256';
UPDATE san_pham SET so_luong_ton = 10 WHERE ma_san_pham = 'SP_MI_14_256TRANG';
UPDATE san_pham SET so_luong_ton = 5 WHERE ma_san_pham = 'SP_OPPO_RENO11_256';
UPDATE san_pham SET so_luong_ton = 4 WHERE ma_san_pham = 'SP_OPPO_FINDN3';
UPDATE san_pham SET so_luong_ton = 0 WHERE ma_san_pham = 'SP_XIAOMI_14U_OUT';

-- 4.5 Nhật ký xuất báo cáo mẫu cho UC-28
INSERT IGNORE INTO nhat_ky_bao_cao (ma_bao_cao, loai_bao_cao, ma_nhan_vien_lap, ngay_lap, tu_ngay, den_ngay, dinh_dang_xuat, duong_dan_file, tieu_chi_loc, ghi_chu) VALUES
(1, 'Doanh_thu', 'NV001', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(CURDATE(), INTERVAL 30 DAY), CURDATE(), 'EXCEL', 'bao_cao_doanh_thu_thang.xlsx', 'Thương hiệu: Tất cả | Kỳ: 30 ngày gần nhất', 'Tổng hợp doanh thu định kỳ'),
(2, 'Kiem_ke_kho', 'NV004', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(CURDATE(), INTERVAL 14 DAY), CURDATE(), 'PDF', 'bao_cao_ton_kho_tuan.pdf', 'Cảnh báo định mức tồn kho', 'Báo cáo tồn kho & cảnh báo nhập hàng');

