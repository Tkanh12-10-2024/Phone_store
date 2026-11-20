-- ========================================================================
-- DỮ LIỆU MẪU: QUẢN LÝ NHẬP HÀNG & KHO HÀNG (SGU - NHÓM 05)
-- ========================================================================

USE `quanly_cuahang_dienthoai`;

-- 1. NHÀ CUNG CẤP
INSERT INTO `nha_cung_cap` (`ma_nha_cung_cap`, `ten_nha_cung_cap`, `so_dien_thoai`, `email`, `dia_chi`, `ma_so_thue`, `cong_no`, `trang_thai`) VALUES
('NCC01', 'Công ty Cổ phần Bán lẻ Kỹ thuật số FPT (Synnex FPT)', '02473006666', 'contact@synnexfpt.com.vn', 'Tòa nhà FPT, Phố Duy Tân, Cầu Giấy, Hà Nội', '0101778161', 25000000.00, 'Dang_hop_tac'),
('NCC02', 'Công ty Cổ phần Thế Giới Số (Digiworld Corporation)', '02839290059', 'info@dgw.com.vn', '195 Nguyễn Thái Bình, P. Nguyễn Thái Bình, Quận 1, TP.HCM', '0302861742', 0.00, 'Dang_hop_tac'),
('NCC03', 'Công ty TNHH Phân phối Synnex FPT miền Nam', '02873008888', 'contact.hcm@synnexfpt.com', 'Khu Công Nghệ Cao, TP. Thủ Đức, TP.HCM', '0301479821', 12000000.00, 'Dang_hop_tac'),
('NCC04', 'Tổng công ty Cổ phần Bưu chính Viettel (Viettel Commerce)', '02462777888', 'kinhdoanh@viettel.vn', 'Tòa nhà Viettel, Số 1 Giang Văn Minh, Ba Đình, Hà Nội', '0104093672', 0.00, 'Dang_hop_tac')
ON DUPLICATE KEY UPDATE `ten_nha_cung_cap`=VALUES(`ten_nha_cung_cap`);

-- 2. SẢN PHẨM MẪU VÀ TỒN KHO BAN ĐẦU
INSERT INTO `san_pham` (`ma_san_pham`, `ten_san_pham`, `dong_may`, `mau_sac`, `ram`, `rom`, `gia_nhap_tham_khao`, `gia_ban`, `so_luong_ton`, `dinh_muc_ton_toi_thieu`, `trang_thai`) VALUES
('SP_IP15PM_256', 'iPhone 15 Pro Max 256GB Titan Tự Nhiên', 'iPhone 15 Series', 'Titan Tự Nhiên', '8GB', '256GB', 26500000.00, 29990000.00, 5, 2, 'Dang_kinh_doanh'),
('SP_IP15PM_512', 'iPhone 15 Pro Max 512GB Titan Xanh', 'iPhone 15 Series', 'Titan Xanh', '8GB', '512GB', 31500000.00, 35490000.00, 2, 3, 'Dang_kinh_doanh'),
('SP_IP15_128', 'iPhone 15 128GB Hồng Pastel', 'iPhone 15 Series', 'Hồng Pastel', '6GB', '128GB', 17200000.00, 19490000.00, 1, 2, 'Dang_kinh_doanh'),
('SP_SS_S24U_256', 'Samsung Galaxy S24 Ultra 256GB Xám Titan', 'Galaxy S Series', 'Xám Titan', '12GB', '256GB', 24000000.00, 27990000.00, 4, 2, 'Dang_kinh_doanh'),
('SP_SS_S24_256', 'Samsung Galaxy S24 256GB Vàng Amber', 'Galaxy S Series', 'Vàng Amber', '8GB', '256GB', 15500000.00, 17990000.00, 0, 2, 'Dang_kinh_doanh'),
('SP_OPPO_R11_256', 'OPPO Reno11 5G 256GB Xanh Sóng Biển', 'Reno Series', 'Xanh Sóng Biển', '8GB', '256GB', 8200000.00, 9990000.00, 6, 2, 'Dang_kinh_doanh'),
('SP_XM_14U_512', 'Xiaomi 14 Ultra 512GB Đen Nhiếp Ảnh', 'Xiaomi Number Series', 'Đen', '16GB', '512GB', 26000000.00, 29990000.00, 1, 2, 'Dang_kinh_doanh')
ON DUPLICATE KEY UPDATE `ten_san_pham`=VALUES(`ten_san_pham`);

-- 3. PHIẾU NHẬP MẪU
INSERT INTO `phieu_nhap` (`ma_phieu_nhap`, `ma_nha_cung_cap`, `ma_nhan_vien`, `ngay_nhap`, `tong_tien`, `da_thanh_toan`, `cong_no_con_lai`, `trang_thai`, `ghi_chu`) VALUES
('PN20261110_01', 'NCC01', 'NV001', '2026-11-10 09:30:00', 132500000.00, 132500000.00, 0.00, 'Hoan_thanh', 'Nhập lô iPhone 15 Pro Max đợt 1 tháng 11'),
('PN20261112_02', 'NCC02', 'NV001', '2026-11-12 14:15:00', 96000000.00, 71000000.00, 25000000.00, 'Hoan_thanh', 'Nhập bổ sung Samsung S24 Ultra từ Digiworld'),
('PN20261113_03', 'NCC04', 'NV001', '2026-11-13 08:45:00', 49200000.00, 49200000.00, 0.00, 'Hoan_thanh', 'Nhập OPPO Reno11 và Xiaomi 14 Ultra')
ON DUPLICATE KEY UPDATE `tong_tien`=VALUES(`tong_tien`);

-- 4. CHI TIẾT PHIẾU NHẬP
INSERT INTO `chi_tiet_phieu_nhap` (`ma_chi_tiet_nhap`, `ma_phieu_nhap`, `ma_san_pham`, `so_luong`, `don_gia_nhap`, `thanh_tien`) VALUES
(1, 'PN20261110_01', 'SP_IP15PM_256', 5, 26500000.00, 132500000.00),
(2, 'PN20261112_02', 'SP_SS_S24U_256', 4, 24000000.00, 96000000.00),
(3, 'PN20261113_03', 'SP_OPPO_R11_256', 6, 8200000.00, 49200000.00)
ON DUPLICATE KEY UPDATE `so_luong`=VALUES(`so_luong`);

-- 5. MÃ IMEI SẢN PHẨM TRONG KHO (15 SỐ CHUẨN)
INSERT INTO `imei_san_pham` (`ma_imei`, `ma_san_pham`, `ma_phieu_nhap`, `trang_thai`, `ngay_nhap`) VALUES
('354928110293841', 'SP_IP15PM_256', 'PN20261110_01', 'Trong_kho', '2026-11-10 09:30:00'),
('354928110293842', 'SP_IP15PM_256', 'PN20261110_01', 'Trong_kho', '2026-11-10 09:30:00'),
('354928110293843', 'SP_IP15PM_256', 'PN20261110_01', 'Trong_kho', '2026-11-10 09:30:00'),
('354928110293844', 'SP_IP15PM_256', 'PN20261110_01', 'Trong_kho', '2026-11-10 09:30:00'),
('354928110293845', 'SP_IP15PM_256', 'PN20261110_01', 'Trong_kho', '2026-11-10 09:30:00'),

('358712093847561', 'SP_SS_S24U_256', 'PN20261112_02', 'Trong_kho', '2026-11-12 14:15:00'),
('358712093847562', 'SP_SS_S24U_256', 'PN20261112_02', 'Trong_kho', '2026-11-12 14:15:00'),
('358712093847563', 'SP_SS_S24U_256', 'PN20261112_02', 'Trong_kho', '2026-11-12 14:15:00'),
('358712093847564', 'SP_SS_S24U_256', 'PN20261112_02', 'Trong_kho', '2026-11-12 14:15:00'),

('864209128374611', 'SP_OPPO_R11_256', 'PN20261113_03', 'Trong_kho', '2026-11-13 08:45:00'),
('864209128374612', 'SP_OPPO_R11_256', 'PN20261113_03', 'Trong_kho', '2026-11-13 08:45:00'),
('864209128374613', 'SP_OPPO_R11_256', 'PN20261113_03', 'Trong_kho', '2026-11-13 08:45:00')
ON DUPLICATE KEY UPDATE `trang_thai`=VALUES(`trang_thai`);

-- 6. PHIẾU KIỂM KÊ KHO MẪU
INSERT INTO `phieu_kiem_ke` (`ma_phieu_kiem_ke`, `ma_nhan_vien`, `ngay_kiem_ke`, `trang_thai`, `ghi_chu`) VALUES
('PKK20261111_01', 'NV001', '2026-11-11 17:00:00', 'Da_can_bang', 'Kiểm kê định kỳ đầu tuần khu vực iPhone')
ON DUPLICATE KEY UPDATE `trang_thai`=VALUES(`trang_thai`);

-- 7. CHI TIẾT KIỂM KÊ
INSERT INTO `chi_tiet_kiem_ke` (`ma_chi_tiet_kiem_ke`, `ma_phieu_kiem_ke`, `ma_san_pham`, `so_luong_he_thong`, `so_luong_thuc_te`, `chenh_lech`, `ly_do_chenh_lech`) VALUES
(1, 'PKK20261111_01', 'SP_IP15PM_256', 5, 5, 0, 'Khớp số lượng'),
(2, 'PKK20261111_01', 'SP_IP15_128', 2, 1, -1, '1 máy mang đi trưng bày tại bàn trải nghiệm')
ON DUPLICATE KEY UPDATE `so_luong_thuc_te`=VALUES(`so_luong_thuc_te`);
