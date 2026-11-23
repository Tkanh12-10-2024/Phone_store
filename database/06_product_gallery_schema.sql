-- ============================================================================
-- MIGRATION & DỮ LIỆU MẪU BỔ SUNG CHO PHÂN HỆ 2: QUẢN LÝ SẢN PHẨM & DANH MỤC
-- (Thành viên 2: Huỳnh Trọng Duy - MSSV: 3124560014)
-- Tương thích MySQL 8.0+ / MariaDB (XAMPP) trên CSDL quanly_cuahang_dienthoai
-- ============================================================================

CREATE DATABASE IF NOT EXISTS quanly_cuahang_dienthoai
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE quanly_cuahang_dienthoai;

-- 1. Tạo bảng Thư viện ảnh sản phẩm (hinh_anh_san_pham / galleries)
CREATE TABLE IF NOT EXISTS hinh_anh_san_pham (
    ma_hinh_anh INT AUTO_INCREMENT PRIMARY KEY,
    ma_san_pham VARCHAR(20) NOT NULL,
    duong_dan_anh VARCHAR(500) NOT NULL,
    la_anh_chinh TINYINT(1) NOT NULL DEFAULT 0,
    thu_tu INT NOT NULL DEFAULT 0,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_hasp_sanpham (ma_san_pham),
    CONSTRAINT fk_hasp_sanpham FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Cập nhật ảnh đại diện mẫu cục bộ nếu sản phẩm chưa có ảnh
UPDATE san_pham SET hinh_anh = '/uploads/products/iphone-15-pro-max.svg'
WHERE ma_san_pham IN ('SP_IP15PM_256TN', 'SP_IP15PM_512DEN') AND (hinh_anh IS NULL OR hinh_anh = '');

UPDATE san_pham SET hinh_anh = '/uploads/products/galaxy-s24-ultra.svg'
WHERE ma_san_pham IN ('SP_SS_S24U_256AM', 'SP_SS_ZFLIP5_256') AND (hinh_anh IS NULL OR hinh_anh = '');

UPDATE san_pham SET hinh_anh = '/uploads/products/xiaomi-14.svg'
WHERE ma_san_pham IN ('SP_MI_14_256TRANG', 'SP_XIAOMI_14U_OUT') AND (hinh_anh IS NULL OR hinh_anh = '');

UPDATE san_pham SET hinh_anh = '/uploads/products/oppo-reno11.svg'
WHERE ma_san_pham IN ('SP_OPPO_RENO11_256', 'SP_OPPO_FINDN3') AND (hinh_anh IS NULL OR hinh_anh = '');

-- 3. Bổ sung dữ liệu gallery mẫu an toàn (idempotent, chỉ chèn nếu sản phẩm tồn tại và chưa có gallery)
INSERT INTO hinh_anh_san_pham (ma_san_pham, duong_dan_anh, la_anh_chinh, thu_tu)
SELECT sp.ma_san_pham, sp.hinh_anh, 1, 1
FROM san_pham sp
WHERE sp.hinh_anh IS NOT NULL AND sp.hinh_anh <> ''
  AND NOT EXISTS (SELECT 1 FROM hinh_anh_san_pham ha WHERE ha.ma_san_pham = sp.ma_san_pham);
