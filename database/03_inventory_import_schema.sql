-- ========================================================================
-- PHÂN HỆ QUẢN LÝ NHẬP HÀNG & QUẢN LÝ KHO HÀNG (SGU - NHÓM 05)
-- CSDL: quanly_cuahang_dienthoai
-- ========================================================================

USE `quanly_cuahang_dienthoai`;

-- 1. BẢNG NHÀ CUNG CẤP
CREATE TABLE IF NOT EXISTS `nha_cung_cap` (
    `ma_nha_cung_cap` VARCHAR(20) PRIMARY KEY COMMENT 'VD: NCC01, NCC02',
    `ten_nha_cung_cap` VARCHAR(150) NOT NULL,
    `so_dien_thoai` VARCHAR(15) NOT NULL,
    `email` VARCHAR(100) NULL,
    `dia_chi` VARCHAR(255) NOT NULL,
    `ma_so_thue` VARCHAR(30) NULL,
    `cong_no` DECIMAL(14,2) DEFAULT 0.00,
    `trang_thai` VARCHAR(20) NOT NULL DEFAULT 'Dang_hop_tac'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. BẢNG SẢN PHẨM (NẾU CHƯA CÓ TRONG SCHEMA CŨ)
CREATE TABLE IF NOT EXISTS `san_pham` (
    `ma_san_pham` VARCHAR(20) PRIMARY KEY COMMENT 'VD: SP_IP15PM_256',
    `ten_san_pham` VARCHAR(150) NOT NULL,
    `dong_may` VARCHAR(100) NOT NULL,
    `mau_sac` VARCHAR(50) NOT NULL,
    `ram` VARCHAR(20) NOT NULL,
    `rom` VARCHAR(20) NOT NULL,
    `gia_nhap_tham_khao` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    `gia_ban` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    `so_luong_ton` INT NOT NULL DEFAULT 0,
    `dinh_muc_ton_toi_thieu` INT NOT NULL DEFAULT 2,
    `trang_thai` VARCHAR(30) NOT NULL DEFAULT 'Dang_kinh_doanh',
    `hinh_anh` VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. BẢNG PHIẾU NHẬP
CREATE TABLE IF NOT EXISTS `phieu_nhap` (
    `ma_phieu_nhap` VARCHAR(30) PRIMARY KEY COMMENT 'VD: PN20261113_01',
    `ma_nha_cung_cap` VARCHAR(20) NOT NULL,
    `ma_nhan_vien` VARCHAR(20) NOT NULL,
    `ngay_nhap` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `tong_tien` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    `da_thanh_toan` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    `cong_no_con_lai` DECIMAL(14,2) DEFAULT 0.00,
    `trang_thai` VARCHAR(20) NOT NULL DEFAULT 'Hoan_thanh',
    `ghi_chu` TEXT NULL,
    CONSTRAINT `fk_phieunhap_ncc` FOREIGN KEY (`ma_nha_cung_cap`) REFERENCES `nha_cung_cap`(`ma_nha_cung_cap`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. BẢNG CHI TIẾT PHIẾU NHẬP
CREATE TABLE IF NOT EXISTS `chi_tiet_phieu_nhap` (
    `ma_chi_tiet_nhap` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `ma_phieu_nhap` VARCHAR(30) NOT NULL,
    `ma_san_pham` VARCHAR(20) NOT NULL,
    `so_luong` INT NOT NULL,
    `don_gia_nhap` DECIMAL(14,2) NOT NULL,
    `thanh_tien` DECIMAL(14,2) NOT NULL,
    CONSTRAINT `fk_ctpn_phieunhap` FOREIGN KEY (`ma_phieu_nhap`) REFERENCES `phieu_nhap`(`ma_phieu_nhap`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_ctpn_sanpham` FOREIGN KEY (`ma_san_pham`) REFERENCES `san_pham`(`ma_san_pham`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. BẢNG IMEI SẢN PHẨM
CREATE TABLE IF NOT EXISTS `imei_san_pham` (
    `ma_imei` VARCHAR(30) PRIMARY KEY COMMENT '15 chữ số IMEI',
    `ma_san_pham` VARCHAR(20) NOT NULL,
    `ma_phieu_nhap` VARCHAR(30) NOT NULL,
    `ma_hoa_don` VARCHAR(30) NULL,
    `trang_thai` VARCHAR(30) NOT NULL DEFAULT 'Trong_kho',
    `ngay_nhap` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `ngay_ban` DATETIME NULL,
    `han_bao_hanh` DATE NULL,
    CONSTRAINT `fk_imei_sanpham` FOREIGN KEY (`ma_san_pham`) REFERENCES `san_pham`(`ma_san_pham`) ON UPDATE CASCADE,
    CONSTRAINT `fk_imei_phieunhap` FOREIGN KEY (`ma_phieu_nhap`) REFERENCES `phieu_nhap`(`ma_phieu_nhap`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. BẢNG PHIẾU KIỂM KÊ KHO
CREATE TABLE IF NOT EXISTS `phieu_kiem_ke` (
    `ma_phieu_kiem_ke` VARCHAR(30) PRIMARY KEY COMMENT 'VD: PKK20261113_01',
    `ma_nhan_vien` VARCHAR(20) NOT NULL,
    `ngay_kiem_ke` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `trang_thai` VARCHAR(20) NOT NULL DEFAULT 'Da_can_bang',
    `ghi_chu` TEXT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. BẢNG CHI TIẾT KIỂM KÊ
CREATE TABLE IF NOT EXISTS `chi_tiet_kiem_ke` (
    `ma_chi_tiet_kiem_ke` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `ma_phieu_kiem_ke` VARCHAR(30) NOT NULL,
    `ma_san_pham` VARCHAR(20) NOT NULL,
    `so_luong_he_thong` INT NOT NULL,
    `so_luong_thuc_te` INT NOT NULL,
    `chenh_lech` INT NOT NULL,
    `ly_do_chenh_lech` VARCHAR(255) NULL,
    CONSTRAINT `fk_ctkk_phieu` FOREIGN KEY (`ma_phieu_kiem_ke`) REFERENCES `phieu_kiem_ke`(`ma_phieu_kiem_ke`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_ctkk_sanpham` FOREIGN KEY (`ma_san_pham`) REFERENCES `san_pham`(`ma_san_pham`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
