-- =================================================================================================
-- ĐỒ ÁN MÔN: PHÂN TÍCH THIẾT KẾ HƯỚNG ĐỐI TƯỢNG (PTTKHĐT) - ĐẠI HỌC SÀI GÒN (SGU)
-- ĐỀ TÀI: HỆ THỐNG QUẢN LÝ CỬA HÀNG ĐIỆN THOẠI
-- NHÓM: 05 | GVHD: THẦY HOÀNG MẠNH HÀ
-- HỆ QUẢN TRỊ CSDL: MySQL / MariaDB (Tương thích hoàn toàn với XAMPP phpMyAdmin)
-- Bảng mã: UTF8MB4 (Hỗ trợ tiếng Việt Unicode đầy đủ)
-- =================================================================================================

DROP DATABASE IF EXISTS quanly_cuahang_dienthoai;
CREATE DATABASE quanly_cuahang_dienthoai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE quanly_cuahang_dienthoai;

-- =================================================================================================
-- PHÂN HỆ 1: TÀI KHOẢN, PHÂN QUYỀN VÀ NHÂN VIÊN (Use Case 2.1, 2.3)
-- =================================================================================================

-- 1. Bảng Vai trò (Roles / Chức vụ phân quyền)
CREATE TABLE vai_tro (
    ma_vai_tro INT AUTO_INCREMENT PRIMARY KEY,
    ten_vai_tro VARCHAR(50) NOT NULL UNIQUE COMMENT 'Admin, Quản lý, Nhân viên bán hàng, Nhân viên kho, Kỹ thuật viên',
    mo_ta VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bảng Nhân viên
CREATE TABLE nhan_vien (
    ma_nhan_vien VARCHAR(20) PRIMARY KEY COMMENT 'VD: NV001, NV002',
    ho_ten VARCHAR(100) NOT NULL,
    gioi_tinh ENUM('Nam', 'Nu', 'Khac') DEFAULT 'Nam',
    ngay_sinh DATE NULL,
    so_dien_thoai VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(100) UNIQUE,
    dia_chi VARCHAR(255),
    chuc_vu VARCHAR(50) NOT NULL COMMENT 'Quản lý, Bán hàng, Kho, Kỹ thuật',
    ngay_vao_lam DATE NOT NULL DEFAULT (CURRENT_DATE),
    luong_co_ban DECIMAL(12,2) DEFAULT 0,
    trang_thai ENUM('Dang_lam_viec', 'Da_nghi_viec') NOT NULL DEFAULT 'Dang_lam_viec'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bảng Tài khoản (Liên kết 1-1 với nhân viên)
CREATE TABLE tai_khoan (
    ma_tai_khoan INT AUTO_INCREMENT PRIMARY KEY,
    ma_nhan_vien VARCHAR(20) NOT NULL UNIQUE,
    ten_dang_nhap VARCHAR(50) NOT NULL UNIQUE,
    mat_khau VARCHAR(255) NOT NULL COMMENT 'Mật khẩu đã mã hoá hoặc văn bản mẫu',
    ma_vai_tro INT NOT NULL,
    trang_thai ENUM('Hoat_dong', 'Bi_khoa') NOT NULL DEFAULT 'Hoat_dong',
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    lan_dang_nhap_cuoi DATETIME NULL,
    CONSTRAINT fk_taikhoan_nhanvien FOREIGN KEY (ma_nhan_vien) REFERENCES nhan_vien(ma_nhan_vien) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_taikhoan_vaitro FOREIGN KEY (ma_vai_tro) REFERENCES vai_tro(ma_vai_tro) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =================================================================================================
-- PHÂN HỆ 2: SẢN PHẨM & DANH MỤC ĐIỆN THOẠI (Use Case 2.2.2, 2.4)
-- =================================================================================================

-- 4. Bảng Thương hiệu (Hãng sản xuất: Apple, Samsung, Xiaomi, OPPO, v.v.)
CREATE TABLE thuong_hieu (
    ma_thuong_hieu INT AUTO_INCREMENT PRIMARY KEY,
    ten_thuong_hieu VARCHAR(50) NOT NULL UNIQUE,
    quoc_gia VARCHAR(50) DEFAULT 'Quốc tế',
    logo_url VARCHAR(255) NULL,
    mo_ta TEXT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Bảng Danh mục / Phân loại (Điện thoại cao cấp, tầm trung, giá rẻ, phụ kiện...)
CREATE TABLE danh_muc (
    ma_danh_muc INT AUTO_INCREMENT PRIMARY KEY,
    ten_danh_muc VARCHAR(100) NOT NULL UNIQUE,
    mo_ta VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Bảng Sản phẩm (Điện thoại)
CREATE TABLE san_pham (
    ma_san_pham VARCHAR(20) PRIMARY KEY COMMENT 'VD: SP_IP15PM_256, SP_SS_S24U',
    ten_san_pham VARCHAR(150) NOT NULL,
    ma_thuong_hieu INT NOT NULL,
    ma_danh_muc INT NOT NULL,
    dong_may VARCHAR(100) NOT NULL COMMENT 'iPhone 15 Series, Galaxy S Series...',
    mau_sac VARCHAR(50) NOT NULL COMMENT 'Titan Tự Nhiên, Đen, Xanh Titan...',
    ram VARCHAR(20) NOT NULL COMMENT '8GB, 12GB...',
    rom VARCHAR(20) NOT NULL COMMENT '128GB, 256GB, 512GB, 1TB',
    man_hinh VARCHAR(100) NULL COMMENT 'OLED 6.7 inch 120Hz',
    chip_xu_ly VARCHAR(100) NULL COMMENT 'Apple A17 Pro, Snapdragon 8 Gen 3',
    pin VARCHAR(50) NULL COMMENT '4422 mAh',
    camera VARCHAR(100) NULL COMMENT 'Chính 48MP, Phụ 12MP, 12MP',
    he_dieu_hanh VARCHAR(50) NULL COMMENT 'iOS 17, Android 14',
    gia_nhap_tham_khao DECIMAL(14,2) NOT NULL DEFAULT 0,
    gia_ban DECIMAL(14,2) NOT NULL DEFAULT 0,
    thoi_gian_bao_hanh_thang INT NOT NULL DEFAULT 12 COMMENT 'Mặc định 12 tháng',
    so_luong_ton INT NOT NULL DEFAULT 0 COMMENT 'Số lượng thực tế trong kho',
    dinh_muc_ton_toi_thieu INT NOT NULL DEFAULT 2 COMMENT 'Ngưỡng cảnh báo hết hàng',
    hinh_anh VARCHAR(255) NULL,
    trang_thai ENUM('Dang_kinh_doanh', 'Ngung_kinh_doanh') NOT NULL DEFAULT 'Dang_kinh_doanh',
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sanpham_thuonghieu FOREIGN KEY (ma_thuong_hieu) REFERENCES thuong_hieu(ma_thuong_hieu) ON UPDATE CASCADE,
    CONSTRAINT fk_sanpham_danhmuc FOREIGN KEY (ma_danh_muc) REFERENCES danh_muc(ma_danh_muc) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =================================================================================================
-- PHÂN HỆ 3: NHÀ CUNG CẤP & NHẬP HÀNG (Use Case 2.2.3, 2.5)
-- =================================================================================================

-- 7. Bảng Nhà cung cấp
CREATE TABLE nha_cung_cap (
    ma_nha_cung_cap VARCHAR(20) PRIMARY KEY COMMENT 'VD: NCC01, NCC02',
    ten_nha_cung_cap VARCHAR(150) NOT NULL,
    so_dien_thoai VARCHAR(15) NOT NULL,
    email VARCHAR(100) NULL,
    dia_chi VARCHAR(255) NOT NULL,
    ma_so_thue VARCHAR(30) NULL,
    cong_no DECIMAL(14,2) DEFAULT 0.00 COMMENT 'Dư nợ với nhà cung cấp',
    trang_thai ENUM('Dang_hop_tac', 'Ngung_hop_tac') NOT NULL DEFAULT 'Dang_hop_tac'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Bảng Phiếu nhập kho
CREATE TABLE phieu_nhap (
    ma_phieu_nhap VARCHAR(30) PRIMARY KEY COMMENT 'VD: PN20261018_01',
    ma_nha_cung_cap VARCHAR(20) NOT NULL,
    ma_nhan_vien VARCHAR(20) NOT NULL COMMENT 'Nhân viên kho lập phiếu',
    ngay_nhap DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tong_tien DECIMAL(14,2) NOT NULL DEFAULT 0,
    da_thanh_toan DECIMAL(14,2) NOT NULL DEFAULT 0,
    cong_no_con_lai DECIMAL(14,2) GENERATED ALWAYS AS (tong_tien - da_thanh_toan) STORED,
    trang_thai ENUM('Cho_duyet', 'Hoan_thanh', 'Da_huy') NOT NULL DEFAULT 'Hoan_thanh',
    ghi_chu TEXT NULL,
    CONSTRAINT fk_phieunhap_ncc FOREIGN KEY (ma_nha_cung_cap) REFERENCES nha_cung_cap(ma_nha_cung_cap) ON UPDATE CASCADE,
    CONSTRAINT fk_phieunhap_nhanvien FOREIGN KEY (ma_nhan_vien) REFERENCES nhan_vien(ma_nhan_vien) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Bảng Chi tiết phiếu nhập
CREATE TABLE chi_tiet_phieu_nhap (
    ma_chi_tiet_nhap INT AUTO_INCREMENT PRIMARY KEY,
    ma_phieu_nhap VARCHAR(30) NOT NULL,
    ma_san_pham VARCHAR(20) NOT NULL,
    so_luong INT NOT NULL CHECK (so_luong > 0),
    don_gia_nhap DECIMAL(14,2) NOT NULL CHECK (don_gia_nhap >= 0),
    thanh_tien DECIMAL(14,2) GENERATED ALWAYS AS (so_luong * don_gia_nhap) STORED,
    CONSTRAINT fk_ctpn_phieunhap FOREIGN KEY (ma_phieu_nhap) REFERENCES phieu_nhap(ma_phieu_nhap) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ctpn_sanpham FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =================================================================================================
-- PHÂN HỆ 4: QUẢN LÝ MÃ IMEI & TỒN KHO THIẾT BỊ (ĐẶC THÙ NGÀNH ĐIỆN THOẠI - Use Case 2.5.2.1.1)
-- =================================================================================================

-- 10. Bảng IMEI Sản phẩm (Mỗi chiếc điện thoại có 1 mã IMEI duy nhất)
CREATE TABLE imei_san_pham (
    ma_imei VARCHAR(30) PRIMARY KEY COMMENT 'Mã IMEI gồm 15 số duy nhất',
    ma_san_pham VARCHAR(20) NOT NULL,
    ma_phieu_nhap VARCHAR(30) NOT NULL,
    ma_hoa_don VARCHAR(30) NULL COMMENT 'Gắn với hoá đơn bán khi đã bán ra',
    trang_thai ENUM('Trong_kho', 'Da_ban', 'Dang_bao_hanh', 'Loi_doi_tra', 'Da_tra_NCC') NOT NULL DEFAULT 'Trong_kho',
    ngay_nhap DATETIME DEFAULT CURRENT_TIMESTAMP,
    ngay_ban DATETIME NULL,
    han_bao_hanh DATE NULL COMMENT 'Tính theo ngày bán + số tháng bảo hành',
    CONSTRAINT fk_imei_sanpham FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham) ON UPDATE CASCADE,
    CONSTRAINT fk_imei_phieunhap FOREIGN KEY (ma_phieu_nhap) REFERENCES phieu_nhap(ma_phieu_nhap) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =================================================================================================
-- PHÂN HỆ 5: KIỂM KÊ KHO & ĐỊNH MỨC TỒN KHO (Use Case 2.6)
-- =================================================================================================

-- 11. Bảng Phiếu kiểm kê
CREATE TABLE phieu_kiem_ke (
    ma_phieu_kiem_ke VARCHAR(30) PRIMARY KEY COMMENT 'VD: PKK20261018_01',
    ma_nhan_vien VARCHAR(20) NOT NULL,
    ngay_kiem_ke DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trang_thai ENUM('Da_can_bang', 'Cho_xu_ly') DEFAULT 'Da_can_bang',
    ghi_chu TEXT NULL,
    CONSTRAINT fk_kiemke_nhanvien FOREIGN KEY (ma_nhan_vien) REFERENCES nhan_vien(ma_nhan_vien) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Bảng Chi tiết kiểm kê kho
CREATE TABLE chi_tiet_kiem_ke (
    ma_chi_tiet_kiem_ke INT AUTO_INCREMENT PRIMARY KEY,
    ma_phieu_kiem_ke VARCHAR(30) NOT NULL,
    ma_san_pham VARCHAR(20) NOT NULL,
    so_luong_he_thong INT NOT NULL COMMENT 'Số lượng trên CSDL trước kiểm kê',
    so_luong_thuc_te INT NOT NULL COMMENT 'Số lượng đếm thực tế',
    chenh_lech INT GENERATED ALWAYS AS (so_luong_thuc_te - so_luong_he_thong) STORED,
    ly_do_chenh_lech VARCHAR(255) NULL COMMENT 'Mất, hỏng hóc, sai lệch tem mác...',
    CONSTRAINT fk_ctkk_phieu FOREIGN KEY (ma_phieu_kiem_ke) REFERENCES phieu_kiem_ke(ma_phieu_kiem_ke) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ctkk_sanpham FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =================================================================================================
-- PHÂN HỆ 6: KHÁCH HÀNG & KHUYẾN MÃI (Use Case 2.12, 2.14)
-- =================================================================================================

-- 13. Bảng Khách hàng
CREATE TABLE khach_hang (
    ma_khach_hang VARCHAR(20) PRIMARY KEY COMMENT 'VD: KH001, KH002',
    ho_ten VARCHAR(100) NOT NULL,
    so_dien_thoai VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(100) NULL,
    dia_chi VARCHAR(255) NULL,
    diem_tich_luy INT DEFAULT 0,
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    trang_thai ENUM('Hoat_dong', 'Ngung_hoat_dong') NOT NULL DEFAULT 'Hoat_dong'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. Bảng Chương trình khuyến mãi & Voucher
CREATE TABLE khuyen_mai (
    ma_khuyen_mai VARCHAR(30) PRIMARY KEY COMMENT 'VD: KM_IPHONE_OCT, TET2026',
    code_voucher VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã voucher quét hoặc nhập',
    ten_khuyen_mai VARCHAR(150) NOT NULL,
    loai_giam_gia ENUM('Phan_tram', 'Tien_mat') NOT NULL DEFAULT 'Phan_tram',
    gia_tri_giam DECIMAL(14,2) NOT NULL COMMENT '% giảm (VD: 10) hoặc số tiền (VD: 500000)',
    gia_tri_don_hang_toi_thieu DECIMAL(14,2) NOT NULL DEFAULT 0,
    giam_toi_da DECIMAL(14,2) NULL COMMENT 'Giới hạn mức giảm tối đa nếu là %',
    so_luong_phat_hanh INT NOT NULL DEFAULT 100,
    so_luong_da_dung INT NOT NULL DEFAULT 0,
    ngay_bat_dau DATETIME NOT NULL,
    ngay_ket_thuc DATETIME NOT NULL,
    ma_thuong_hieu_ap_dung INT NULL COMMENT 'NULL nếu áp dụng cho tất cả, hoặc chỉ hãng cụ thể',
    trang_thai ENUM('Dang_dien_ra', 'Tam_dung', 'Het_han') NOT NULL DEFAULT 'Dang_dien_ra',
    CONSTRAINT fk_km_thuonghieu FOREIGN KEY (ma_thuong_hieu_ap_dung) REFERENCES thuong_hieu(ma_thuong_hieu) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =================================================================================================
-- PHÂN HỆ 7: BÁN HÀNG, ĐƠN HÀNG & HÓA ĐƠN (Use Case 2.7, 2.8, 2.9, 2.10, 2.11)
-- =================================================================================================

-- 15. Bảng Giỏ hàng tạm thời (Session bán hàng tại quầy POS)
CREATE TABLE gio_hang (
    ma_gio_hang INT AUTO_INCREMENT PRIMARY KEY,
    ma_nhan_vien VARCHAR(20) NOT NULL,
    ma_khach_hang VARCHAR(20) NULL,
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_giohang_nhanvien FOREIGN KEY (ma_nhan_vien) REFERENCES nhan_vien(ma_nhan_vien) ON UPDATE CASCADE,
    CONSTRAINT fk_giohang_khachhang FOREIGN KEY (ma_khach_hang) REFERENCES khach_hang(ma_khach_hang) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. Bảng Chi tiết giỏ hàng
CREATE TABLE chi_tiet_gio_hang (
    ma_chi_tiet_gio_hang INT AUTO_INCREMENT PRIMARY KEY,
    ma_gio_hang INT NOT NULL,
    ma_san_pham VARCHAR(20) NOT NULL,
    so_luong INT NOT NULL DEFAULT 1 CHECK (so_luong > 0),
    don_gia DECIMAL(14,2) NOT NULL,
    CONSTRAINT fk_ctgh_giohang FOREIGN KEY (ma_gio_hang) REFERENCES gio_hang(ma_gio_hang) ON DELETE CASCADE,
    CONSTRAINT fk_ctgh_sanpham FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. Bảng Đơn hàng (Khởi tạo từ giỏ hàng, chờ thanh toán hoặc đã thanh toán)
CREATE TABLE don_hang (
    ma_don_hang VARCHAR(30) PRIMARY KEY COMMENT 'VD: DH20261018_001',
    ma_khach_hang VARCHAR(20) NULL,
    ma_nhan_vien VARCHAR(20) NOT NULL,
    ma_khuyen_mai VARCHAR(30) NULL,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tong_tien_hang DECIMAL(14,2) NOT NULL DEFAULT 0,
    tien_giam_gia DECIMAL(14,2) NOT NULL DEFAULT 0,
    thue_vat DECIMAL(14,2) NOT NULL DEFAULT 0,
    tong_tien_can_tra DECIMAL(14,2) NOT NULL DEFAULT 0,
    trang_thai ENUM('Cho_thanh_toan', 'Da_thanh_toan', 'Da_huy') NOT NULL DEFAULT 'Cho_thanh_toan',
    ghi_chu TEXT NULL,
    CONSTRAINT fk_donhang_khachhang FOREIGN KEY (ma_khach_hang) REFERENCES khach_hang(ma_khach_hang) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_donhang_nhanvien FOREIGN KEY (ma_nhan_vien) REFERENCES nhan_vien(ma_nhan_vien) ON UPDATE CASCADE,
    CONSTRAINT fk_donhang_khuyenmai FOREIGN KEY (ma_khuyen_mai) REFERENCES khuyen_mai(ma_khuyen_mai) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. Bảng Chi tiết đơn hàng
CREATE TABLE chi_tiet_don_hang (
    ma_chi_tiet_don INT AUTO_INCREMENT PRIMARY KEY,
    ma_don_hang VARCHAR(30) NOT NULL,
    ma_san_pham VARCHAR(20) NOT NULL,
    so_luong INT NOT NULL CHECK (so_luong > 0),
    don_gia DECIMAL(14,2) NOT NULL,
    thanh_tien DECIMAL(14,2) GENERATED ALWAYS AS (so_luong * don_gia) STORED,
    CONSTRAINT fk_ctdh_donhang FOREIGN KEY (ma_don_hang) REFERENCES don_hang(ma_don_hang) ON DELETE CASCADE,
    CONSTRAINT fk_ctdh_sanpham FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 19. Bảng Hóa đơn bán lẻ (Sau khi thanh toán thành công)
CREATE TABLE hoa_don (
    ma_hoa_don VARCHAR(30) PRIMARY KEY COMMENT 'VD: HD20261018_001',
    ma_don_hang VARCHAR(30) NOT NULL UNIQUE,
    ma_khach_hang VARCHAR(20) NULL,
    ma_nhan_vien VARCHAR(20) NOT NULL,
    ngay_lap DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tong_tien_hang DECIMAL(14,2) NOT NULL,
    thue_vat_phan_tram DECIMAL(5,2) DEFAULT 0.00 COMMENT 'Thuế VAT (VD: 8% hoặc 10%)',
    tien_thue_vat DECIMAL(14,2) DEFAULT 0.00,
    tien_giam_gia DECIMAL(14,2) DEFAULT 0.00,
    tong_thanh_toan DECIMAL(14,2) NOT NULL,
    phuong_thuc_thanh_toan ENUM('Tien_mat', 'Chuyen_khoan_QR', 'The') NOT NULL DEFAULT 'Tien_mat',
    tien_khach_dua DECIMAL(14,2) NOT NULL,
    tien_thua DECIMAL(14,2) DEFAULT 0.00,
    trang_thai_thanh_toan ENUM('Thanh_cong', 'That_bai', 'Da_hoan_tien') NOT NULL DEFAULT 'Thanh_cong',
    ghi_chu TEXT NULL,
    CONSTRAINT fk_hoadon_donhang FOREIGN KEY (ma_don_hang) REFERENCES don_hang(ma_don_hang) ON UPDATE CASCADE,
    CONSTRAINT fk_hoadon_khachhang FOREIGN KEY (ma_khach_hang) REFERENCES khach_hang(ma_khach_hang) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_hoadon_nhanvien FOREIGN KEY (ma_nhan_vien) REFERENCES nhan_vien(ma_nhan_vien) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 20. Bảng Chi tiết hóa đơn (Liên kết rõ từng chiếc máy IMEI được bán)
CREATE TABLE chi_tiet_hoa_don (
    ma_chi_tiet_hd INT AUTO_INCREMENT PRIMARY KEY,
    ma_hoa_don VARCHAR(30) NOT NULL,
    ma_san_pham VARCHAR(20) NOT NULL,
    ma_imei VARCHAR(30) NOT NULL UNIQUE COMMENT 'Chính xác chiếc điện thoại có IMEI này được xuất bán',
    don_gia DECIMAL(14,2) NOT NULL,
    thoi_gian_bao_hanh_thang INT NOT NULL DEFAULT 12,
    CONSTRAINT fk_cthd_hoadon FOREIGN KEY (ma_hoa_don) REFERENCES hoa_don(ma_hoa_don) ON DELETE CASCADE,
    CONSTRAINT fk_cthd_sanpham FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham) ON UPDATE CASCADE,
    CONSTRAINT fk_cthd_imei FOREIGN KEY (ma_imei) REFERENCES imei_san_pham(ma_imei) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Cập nhật ràng buộc ngoại cho bảng imei_san_pham tới hoa_don
ALTER TABLE imei_san_pham
ADD CONSTRAINT fk_imei_hoadon FOREIGN KEY (ma_hoa_don) REFERENCES hoa_don(ma_hoa_don) ON DELETE SET NULL ON UPDATE CASCADE;

-- =================================================================================================
-- PHÂN HỆ 8: QUẢN LÝ BẢO HÀNH THIẾT BỊ (Use Case 2.2.5, 2.13)
-- =================================================================================================

-- 21. Bảng Phiếu tiếp nhận bảo hành
CREATE TABLE phieu_bao_hanh (
    ma_phieu_bao_hanh VARCHAR(30) PRIMARY KEY COMMENT 'VD: PBH20261018_01',
    ma_imei VARCHAR(30) NOT NULL,
    ma_san_pham VARCHAR(20) NOT NULL,
    ma_khach_hang VARCHAR(20) NOT NULL,
    ma_hoa_don VARCHAR(30) NULL COMMENT 'Hóa đơn mua máy gốc',
    ma_nhan_vien_tiep_nhan VARCHAR(20) NOT NULL,
    ma_ky_thuat_vien VARCHAR(20) NULL COMMENT 'Kỹ thuật viên phụ trách sửa chữa',
    ngay_tiep_nhan DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tinh_trang_tiep_nhan TEXT NOT NULL COMMENT 'Mô tả ngoại quan: máy trầy xước, vỡ kính, vô nước...',
    loi_khach_phan_anh TEXT NOT NULL COMMENT 'Mô tả lỗi: loa rè, không nhận sạc, sập nguồn...',
    chan_doan_ky_thuat TEXT NULL,
    noi_dung_sua_chua TEXT NULL,
    chi_phi_sua_chua DECIMAL(14,2) DEFAULT 0.00 COMMENT '0đ nếu bảo hành chính hãng, >0đ nếu sửa dịch vụ ngoài',
    ngay_hen_tra DATE NULL,
    ngay_tra_thuc_te DATETIME NULL,
    trang_thai ENUM('Tiep_nhan', 'Dang_sua_chua', 'Cho_tra_khach', 'Hoan_tat', 'Tu_choi_bao_hanh') NOT NULL DEFAULT 'Tiep_nhan',
    ghi_chu TEXT NULL,
    CONSTRAINT fk_pbh_imei FOREIGN KEY (ma_imei) REFERENCES imei_san_pham(ma_imei) ON UPDATE CASCADE,
    CONSTRAINT fk_pbh_sanpham FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham) ON UPDATE CASCADE,
    CONSTRAINT fk_pbh_khachhang FOREIGN KEY (ma_khach_hang) REFERENCES khach_hang(ma_khach_hang) ON UPDATE CASCADE,
    CONSTRAINT fk_pbh_nv_tiepnhan FOREIGN KEY (ma_nhan_vien_tiep_nhan) REFERENCES nhan_vien(ma_nhan_vien) ON UPDATE CASCADE,
    CONSTRAINT fk_pbh_ktv FOREIGN KEY (ma_ky_thuat_vien) REFERENCES nhan_vien(ma_nhan_vien) ON UPDATE CASCADE,
    CONSTRAINT fk_pbh_hoadon FOREIGN KEY (ma_hoa_don) REFERENCES hoa_don(ma_hoa_don) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 22. Bảng Lịch sử tiến độ bảo hành (Theo dõi vòng đời bảo hành)
CREATE TABLE lich_su_bao_hanh (
    ma_lich_su INT AUTO_INCREMENT PRIMARY KEY,
    ma_phieu_bao_hanh VARCHAR(30) NOT NULL,
    ma_nhan_vien_cap_nhat VARCHAR(20) NOT NULL,
    thoi_gian_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trang_thai_moi ENUM('Tiep_nhan', 'Dang_sua_chua', 'Cho_tra_khach', 'Hoan_tat', 'Tu_choi_bao_hanh') NOT NULL,
    ghi_chu_tien_do TEXT NOT NULL,
    CONSTRAINT fk_lsbh_phieu FOREIGN KEY (ma_phieu_bao_hanh) REFERENCES phieu_bao_hanh(ma_phieu_bao_hanh) ON DELETE CASCADE,
    CONSTRAINT fk_lsbh_nhanvien FOREIGN KEY (ma_nhan_vien_cap_nhat) REFERENCES nhan_vien(ma_nhan_vien) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =================================================================================================
-- PHÂN HỆ 9: THỐNG KÊ, BÁO CÁO & NHẬT KÝ (Use Case 2.2.6, 2.16)
-- =================================================================================================

-- 23. Bảng Nhật ký xuất báo cáo
CREATE TABLE nhat_ky_bao_cao (
    ma_bao_cao INT AUTO_INCREMENT PRIMARY KEY,
    loai_bao_cao ENUM('Doanh_thu', 'Kiem_ke_kho', 'SanPham_BanChay', 'Bao_hanh') NOT NULL,
    ma_nhan_vien_lap VARCHAR(20) NOT NULL,
    ngay_lap DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tu_ngay DATE NULL,
    den_ngay DATE NULL,
    dinh_dang_xuat ENUM('EXCEL', 'PDF') NOT NULL DEFAULT 'EXCEL',
    duong_dan_file VARCHAR(255) NULL,
    ghi_chu VARCHAR(255) NULL,
    CONSTRAINT fk_baocao_nhanvien FOREIGN KEY (ma_nhan_vien_lap) REFERENCES nhan_vien(ma_nhan_vien) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =================================================================================================
-- CHỈ MỤC (INDEXES) TỐI ƯU HÓA TRA CỨU THEO YÊU CẦU NGHIỆP VỤ
-- =================================================================================================
CREATE INDEX idx_sanpham_ten ON san_pham(ten_san_pham);
CREATE INDEX idx_sanpham_dongmay ON san_pham(dong_may);
CREATE INDEX idx_imei_trangthai ON imei_san_pham(trang_thai);
CREATE INDEX idx_khachhang_sdt ON khach_hang(so_dien_thoai);
CREATE INDEX idx_hoadon_ngaylap ON hoa_don(ngay_lap);
CREATE INDEX idx_phieunhap_ngaynhap ON phieu_nhap(ngay_nhap);
CREATE INDEX idx_baohanh_imei ON phieu_bao_hanh(ma_imei);
CREATE INDEX idx_baohanh_trangthai ON phieu_bao_hanh(trang_thai);

-- =================================================================================================
-- VIEWS PHỤC VỤ TRỰC TIẾP USE CASE THỐNG KÊ & BÁO CÁO (Use Case 2.16)
-- =================================================================================================

-- View 1: Cảnh báo tồn kho dưới mức định mức an toàn (Use Case 2.6.1.2 & 2.16.3.2)
CREATE OR REPLACE VIEW v_canh_bao_ton_kho AS
SELECT 
    sp.ma_san_pham,
    sp.ten_san_pham,
    th.ten_thuong_hieu,
    sp.dong_may,
    sp.mau_sac,
    sp.rom,
    sp.so_luong_ton,
    sp.dinh_muc_ton_toi_thieu,
    CASE 
        WHEN sp.so_luong_ton = 0 THEN 'Hết hàng'
        WHEN sp.so_luong_ton <= sp.dinh_muc_ton_toi_thieu THEN 'Sắp hết hàng'
        ELSE 'Bình thường'
    END AS muc_do_canh_bao
FROM san_pham sp
JOIN thuong_hieu th ON sp.ma_thuong_hieu = th.ma_thuong_hieu
WHERE sp.so_luong_ton <= sp.dinh_muc_ton_toi_thieu AND sp.trang_thai = 'Dang_kinh_doanh';

-- View 2: Thống kê sản phẩm bán chạy (Best-Seller) (Use Case 2.16.1.1)
CREATE OR REPLACE VIEW v_top_san_pham_ban_chay AS
SELECT 
    sp.ma_san_pham,
    sp.ten_san_pham,
    th.ten_thuong_hieu,
    sp.dong_may,
    sp.mau_sac,
    sp.rom,
    COUNT(cthd.ma_imei) AS so_luong_da_ban,
    SUM(cthd.don_gia) AS tong_doanh_thu,
    sp.so_luong_ton AS ton_kho_hien_tai
FROM chi_tiet_hoa_don cthd
JOIN san_pham sp ON cthd.ma_san_pham = sp.ma_san_pham
JOIN thuong_hieu th ON sp.ma_thuong_hieu = th.ma_thuong_hieu
JOIN hoa_don hd ON cthd.ma_hoa_don = hd.ma_hoa_don
WHERE hd.trang_thai_thanh_toan = 'Thanh_cong'
GROUP BY sp.ma_san_pham, sp.ten_san_pham, th.ten_thuong_hieu, sp.dong_may, sp.mau_sac, sp.rom, sp.so_luong_ton
ORDER BY so_luong_da_ban DESC;

-- View 3: Thống kê doanh thu theo ngày & tháng (Use Case 2.16.2.1)
CREATE OR REPLACE VIEW v_thong_ke_doanh_thu AS
SELECT 
    DATE(ngay_lap) AS ngay,
    YEAR(ngay_lap) AS nam,
    MONTH(ngay_lap) AS thang,
    COUNT(ma_hoa_don) AS so_don_thanh_cong,
    SUM(tong_tien_hang) AS tong_tien_hang,
    SUM(tien_giam_gia) AS tong_giam_gia,
    SUM(tien_thue_vat) AS tong_vat,
    SUM(tong_thanh_toan) AS tong_doanh_thu_thuan
FROM hoa_don
WHERE trang_thai_thanh_toan = 'Thanh_cong'
GROUP BY DATE(ngay_lap), YEAR(ngay_lap), MONTH(ngay_lap)
ORDER BY ngay DESC;

-- View 4: Tra cứu điều kiện bảo hành theo mã IMEI / Khách hàng (Use Case 2.13.1.1)
CREATE OR REPLACE VIEW v_tra_cuu_bao_hanh AS
SELECT 
    i.ma_imei,
    sp.ten_san_pham,
    sp.dong_may,
    sp.mau_sac,
    sp.rom,
    kh.ma_khach_hang,
    kh.ho_ten AS ten_khach_hang,
    kh.so_dien_thoai AS sdt_khach_hang,
    hd.ma_hoa_don,
    hd.ngay_lap AS ngay_mua,
    i.han_bao_hanh,
    CURRENT_DATE() AS ngay_hien_tai,
    CASE 
        WHEN CURRENT_DATE() <= i.han_bao_hanh THEN 'Còn hạn bảo hành'
        ELSE 'Đã hết hạn bảo hành'
    END AS trang_thai_bao_hanh,
    i.trang_thai AS trang_thai_may
FROM imei_san_pham i
JOIN san_pham sp ON i.ma_san_pham = sp.ma_san_pham
LEFT JOIN hoa_don hd ON i.ma_hoa_don = hd.ma_hoa_don
LEFT JOIN khach_hang kh ON hd.ma_khach_hang = kh.ma_khach_hang
WHERE i.trang_thai IN ('Da_ban', 'Dang_bao_hanh');

-- =================================================================================================
-- TRIGGERS HỖ TRỢ TỰ ĐỘNG HÓA NGHIỆP VỤ (AUTOMATION & DATA INTEGRITY)
-- =================================================================================================

DELIMITER $$

-- Trigger 1: Tự động cập nhật số lượng tồn kho khi nhập IMEI mới
CREATE TRIGGER trg_sau_khi_them_imei
AFTER INSERT ON imei_san_pham
FOR EACH ROW
BEGIN
    IF NEW.trang_thai = 'Trong_kho' THEN
        UPDATE san_pham 
        SET so_luong_ton = so_luong_ton + 1 
        WHERE ma_san_pham = NEW.ma_san_pham;
    END IF;
END$$

-- Trigger 2: Tự động trừ tồn kho và cập nhật ngày bán, hạn bảo hành cho IMEI khi bán hàng
CREATE TRIGGER trg_sau_khi_ban_imei
AFTER INSERT ON chi_tiet_hoa_don
FOR EACH ROW
BEGIN
    -- Cập nhật trạng thái máy sang Đã bán
    UPDATE imei_san_pham 
    SET 
        trang_thai = 'Da_ban',
        ma_hoa_don = NEW.ma_hoa_don,
        ngay_ban = (SELECT ngay_lap FROM hoa_don WHERE ma_hoa_don = NEW.ma_hoa_don),
        han_bao_hanh = DATE_ADD((SELECT ngay_lap FROM hoa_don WHERE ma_hoa_don = NEW.ma_hoa_don), INTERVAL NEW.thoi_gian_bao_hanh_thang MONTH)
    WHERE ma_imei = NEW.ma_imei;

    -- Trừ số lượng tồn kho
    UPDATE san_pham 
    SET so_luong_ton = GREATEST(so_luong_ton - 1, 0)
    WHERE ma_san_pham = NEW.ma_san_pham;
END$$

-- Trigger 3: Cập nhật trạng thái IMEI khi tiếp nhận bảo hành
CREATE TRIGGER trg_khi_tiep_nhan_bao_hanh
AFTER INSERT ON phieu_bao_hanh
FOR EACH ROW
BEGIN
    UPDATE imei_san_pham 
    SET trang_thai = 'Dang_bao_hanh'
    WHERE ma_imei = NEW.ma_imei;
    
    -- Ghi nhật ký tiến độ ban đầu
    INSERT INTO lich_su_bao_hanh (ma_phieu_bao_hanh, ma_nhan_vien_cap_nhat, trang_thai_moi, ghi_chu_tien_do)
    VALUES (NEW.ma_phieu_bao_hanh, NEW.ma_nhan_vien_tiep_nhan, 'Tiep_nhan', 'Tiếp nhận thiết bị bảo hành từ khách hàng');
END$$

-- Trigger 4: Cập nhật trạng thái IMEI khi hoàn tất hoặc đóng phiếu bảo hành
CREATE TRIGGER trg_khi_hoan_tat_bao_hanh
AFTER UPDATE ON phieu_bao_hanh
FOR EACH ROW
BEGIN
    IF NEW.trang_thai = 'Hoan_tat' AND OLD.trang_thai != 'Hoan_tat' THEN
        UPDATE imei_san_pham 
        SET trang_thai = 'Da_ban'
        WHERE ma_imei = NEW.ma_imei;
        
        INSERT INTO lich_su_bao_hanh (ma_phieu_bao_hanh, ma_nhan_vien_cap_nhat, trang_thai_moi, ghi_chu_tien_do)
        VALUES (NEW.ma_phieu_bao_hanh, COALESCE(NEW.ma_ky_thuat_vien, NEW.ma_nhan_vien_tiep_nhan), 'Hoan_tat', 'Đã bàn giao lại thiết bị cho khách hàng, hoàn tất quy trình bảo hành');
    END IF;
END$$

DELIMITER ;

-- =================================================================================================
-- DỮ LIỆU MẪU CHUẨN THỰC TẾ (DEMO DATA CHO ĐỒ ÁN)
-- =================================================================================================

-- 1. Dữ liệu vai trò
INSERT INTO vai_tro (ten_vai_tro, mo_ta) VALUES
('Admin', 'Toàn quyền quản trị hệ thống, quản lý nhân sự và phân quyền'),
('QuanLy', 'Quản lý cửa hàng, duyệt báo cáo, kiểm soát doanh thu và khuyến mãi'),
('NhanVienBanHang', 'Lập đơn hàng, quản lý giỏ hàng, tính tiền, in hoá đơn, tiếp nhận bảo hành'),
('NhanVienKho', 'Quản lý kho, lập phiếu nhập hàng, quét mã IMEI, kiểm kê tồn kho'),
('KyThuatVien', 'Tiếp nhận xử lý kỹ thuật, sửa chữa và cập nhật tiến độ bảo hành');

-- 2. Dữ liệu nhân viên
INSERT INTO nhan_vien (ma_nhan_vien, ho_ten, gioi_tinh, ngay_sinh, so_dien_thoai, email, dia_chi, chuc_vu, ngay_vao_lam, luong_co_ban, trang_thai) VALUES
('NV001', 'Hoàng Mạnh Hà', 'Nam', '1985-05-12', '0901234567', 'admin@cuahangdienthoai.vn', '273 An Dương Vương, Q.5, TP.HCM', 'Quản trị viên', '2023-01-01', 25000000, 'Dang_lam_viec'),
('NV002', 'Lê Tạ Khánh', 'Nam', '2004-03-15', '0912345678', 'khanh.lt@cuahangdienthoai.vn', '105 Bà Huyện Thanh Quan, Q.3, TP.HCM', 'Quản lý cửa hàng', '2023-06-01', 18000000, 'Dang_lam_viec'),
('NV003', 'Đặng Hoàng Nam', 'Nam', '2004-07-20', '0923456789', 'nam.dh@cuahangdienthoai.vn', '45 Lê Hồng Phong, Q.10, TP.HCM', 'Nhân viên bán hàng', '2023-09-10', 9000000, 'Dang_lam_viec'),
('NV004', 'Nguyễn Thành Sáng', 'Nam', '2004-11-02', '0934567890', 'sang.nt@cuahangdienthoai.vn', '12 Nguyễn Trãi, Q.5, TP.HCM', 'Nhân viên kho', '2023-09-15', 9500000, 'Dang_lam_viec'),
('NV005', 'Lê Trường Vũ', 'Nam', '2004-01-18', '0945678901', 'vu.lt@cuahangdienthoai.vn', '88 Trần Hưng Đạo, Q.1, TP.HCM', 'Kỹ thuật viên', '2023-10-01', 11000000, 'Dang_lam_viec'),
('NV006', 'Huỳnh Trọng Duy', 'Nam', '2004-09-25', '0956789012', 'duy.ht@cuahangdienthoai.vn', '22 Hùng Vương, Q.5, TP.HCM', 'Nhân viên bán hàng', '2024-01-05', 8500000, 'Dang_lam_viec'),
('NV007', 'Nguyễn Đức Quốc Tuấn', 'Nam', '2004-12-10', '0967890123', 'tuan.ndq@cuahangdienthoai.vn', '67 Lý Thường Kiệt, Q.10, TP.HCM', 'Kỹ thuật viên', '2024-02-15', 10500000, 'Dang_lam_viec');

-- 3. Dữ liệu tài khoản
INSERT INTO tai_khoan (ma_nhan_vien, ten_dang_nhap, mat_khau, ma_vai_tro, trang_thai) VALUES
('NV001', 'admin', 'admin123', 1, 'Hoat_dong'),
('NV002', 'quanly', 'quanly123', 2, 'Hoat_dong'),
('NV003', 'banhang_nam', '123456', 3, 'Hoat_dong'),
('NV004', 'kho_sang', '123456', 4, 'Hoat_dong'),
('NV005', 'kythuat_vu', '123456', 5, 'Hoat_dong'),
('NV006', 'banhang_duy', '123456', 3, 'Hoat_dong'),
('NV007', 'kythuat_tuan', '123456', 5, 'Hoat_dong');

-- 4. Dữ liệu thương hiệu
INSERT INTO thuong_hieu (ten_thuong_hieu, quoc_gia, mo_ta) VALUES
('Apple', 'Mỹ', 'Thương hiệu iPhone, iPad, Apple Watch danh tiếng'),
('Samsung', 'Hàn Quốc', 'Tập đoàn công nghệ hàng đầu thế giới với dòng Galaxy S, Z Flip, Z Fold'),
('Xiaomi', 'Trung Quốc', 'Hãng điện thoại thông minh cấu hình cao, giá tốt'),
('OPPO', 'Trung Quốc', 'Dòng máy chuyên chụp ảnh, thiết kế trẻ trung Reno Series, Find Series');

-- 5. Dữ liệu danh mục
INSERT INTO danh_muc (ten_danh_muc, mo_ta) VALUES
('Flagship (Cao cấp)', 'Điện thoại cao cấp đầu bảng của các hãng'),
('Cận cao cấp', 'Phân khúc từ 13 triệu đến 20 triệu'),
('Tầm trung', 'Phân khúc từ 6 triệu đến 12 triệu'),
('Điện thoại gập', 'Công nghệ màn hình gập cao cấp Flip và Fold');

-- 6. Dữ liệu sản phẩm (Điện thoại)
INSERT INTO san_pham (ma_san_pham, ten_san_pham, ma_thuong_hieu, ma_danh_muc, dong_may, mau_sac, ram, rom, man_hinh, chip_xu_ly, pin, camera, he_dieu_hanh, gia_nhap_tham_khao, gia_ban, thoi_gian_bao_hanh_thang, so_luong_ton, dinh_muc_ton_toi_thieu) VALUES
('SP_IP15PM_256TN', 'iPhone 15 Pro Max 256GB Titan Tự Nhiên', 1, 1, 'iPhone 15 Series', 'Titan Tự Nhiên', '8GB', '256GB', 'OLED 6.7 inch 120Hz', 'Apple A17 Pro', '4422 mAh', 'Chính 48MP, 12MP, 12MP', 'iOS 17', 26500000, 29990000, 12, 0, 3),
('SP_IP15PM_512DEN', 'iPhone 15 Pro Max 512GB Titan Đen', 1, 1, 'iPhone 15 Series', 'Titan Đen', '8GB', '512GB', 'OLED 6.7 inch 120Hz', 'Apple A17 Pro', '4422 mAh', 'Chính 48MP, 12MP, 12MP', 'iOS 17', 30500000, 34490000, 12, 0, 2),
('SP_SS_S24U_256AM', 'Samsung Galaxy S24 Ultra 256GB Xám Titan', 2, 1, 'Galaxy S24 Series', 'Xám Titan', '12GB', '256GB', 'Dynamic AMOLED 2X 6.8 inch', 'Snapdragon 8 Gen 3', '5000 mAh', '200MP + 50MP + 12MP + 10MP', 'Android 14', 24000000, 27990000, 12, 0, 3),
('SP_SS_ZFLIP5_256', 'Samsung Galaxy Z Flip5 256GB Xanh Mint', 2, 4, 'Galaxy Z Series', 'Xanh Mint', '8GB', '256GB', 'Dynamic AMOLED 2X 6.7 inch', 'Snapdragon 8 Gen 2', '3700 mAh', '12MP + 12MP', 'Android 13', 15000000, 18490000, 12, 0, 2),
('SP_MI_14_256TRANG', 'Xiaomi 14 256GB Trắng Tuyết', 3, 1, 'Xiaomi 14 Series', 'Trắng Tuyết', '12GB', '256GB', 'AMOLED 6.36 inch 120Hz', 'Snapdragon 8 Gen 3', '4610 mAh', 'Leica 50MP + 50MP + 50MP', 'Xiaomi HyperOS', 17000000, 20990000, 18, 0, 2),
('SP_OPPO_RENO11_256', 'OPPO Reno11 5G 256GB Xanh Sóng Biển', 4, 3, 'Reno Series', 'Xanh Sóng Biển', '8GB', '256GB', 'AMOLED 6.7 inch 120Hz', 'MediaTek Dimensity 7050', '5000 mAh', '50MP + 32MP + 8MP', 'ColorOS 14', 8000000, 10490000, 12, 0, 4);

-- 7. Dữ liệu nhà cung cấp
INSERT INTO nha_cung_cap (ma_nha_cung_cap, ten_nha_cung_cap, so_dien_thoai, email, dia_chi, ma_so_thue, cong_no) VALUES
('NCC01', 'Công ty Cổ phần Phân phối Synnex FPT', '02873001010', 'contact@fpt.com.vn', 'Tòa nhà FPT Tân Thuận, Q.7, TP.HCM', '0101778163', 50000000),
('NCC02', 'Công ty Cổ phần Thế Giới Số (Digiworld)', '02839290059', 'info@digiworld.com.vn', '195 Điện Biên Phủ, P.15, Q.Bình Thạnh, TP.HCM', '0302888708', 0),
('NCC03', 'Công ty TNHH Phân phối Dầu khí (PHTD)', '02839115588', 'support@phtd.com.vn', 'Tòa nhà PetroVietnam, Q.1, TP.HCM', '0304958211', 25000000);

-- 8. Dữ liệu phiếu nhập kho
INSERT INTO phieu_nhap (ma_phieu_nhap, ma_nha_cung_cap, ma_nhan_vien, ngay_nhap, tong_tien, da_thanh_toan, trang_thai, ghi_chu) VALUES
('PN20261001_01', 'NCC01', 'NV004', '2026-10-01 09:30:00', 132500000, 100000000, 'Hoan_thanh', 'Nhập lô iPhone 15 Pro Max đợt đầu tháng'),
('PN20261005_02', 'NCC02', 'NV004', '2026-10-05 14:15:00', 96000000, 96000000, 'Hoan_thanh', 'Nhập lô Samsung S24 Ultra chính hãng'),
('PN20261010_03', 'NCC03', 'NV004', '2026-10-10 10:00:00', 51000000, 51000000, 'Hoan_thanh', 'Nhập Xiaomi 14 và OPPO Reno11');

-- 9. Dữ liệu chi tiết phiếu nhập
INSERT INTO chi_tiet_phieu_nhap (ma_phieu_nhap, ma_san_pham, so_luong, don_gia_nhap) VALUES
('PN20261001_01', 'SP_IP15PM_256TN', 5, 26500000),
('PN20261005_02', 'SP_SS_S24U_256AM', 4, 24000000),
('PN20261010_03', 'SP_MI_14_256TRANG', 3, 17000000);

-- 10. Quét/Nhập mã IMEI cho các máy nhập kho (Kích hoạt Trigger 1 tự động tăng tồn kho)
INSERT INTO imei_san_pham (ma_imei, ma_san_pham, ma_phieu_nhap, trang_thai, ngay_nhap) VALUES
-- 5 máy iPhone 15 Pro Max
('358941091234561', 'SP_IP15PM_256TN', 'PN20261001_01', 'Trong_kho', '2026-10-01 09:30:00'),
('358941091234562', 'SP_IP15PM_256TN', 'PN20261001_01', 'Trong_kho', '2026-10-01 09:30:00'),
('358941091234563', 'SP_IP15PM_256TN', 'PN20261001_01', 'Trong_kho', '2026-10-01 09:30:00'),
('358941091234564', 'SP_IP15PM_256TN', 'PN20261001_01', 'Trong_kho', '2026-10-01 09:30:00'),
('358941091234565', 'SP_IP15PM_256TN', 'PN20261001_01', 'Trong_kho', '2026-10-01 09:30:00'),

-- 4 máy Samsung Galaxy S24 Ultra
('864219082345671', 'SP_SS_S24U_256AM', 'PN20261005_02', 'Trong_kho', '2026-10-05 14:15:00'),
('864219082345672', 'SP_SS_S24U_256AM', 'PN20261005_02', 'Trong_kho', '2026-10-05 14:15:00'),
('864219082345673', 'SP_SS_S24U_256AM', 'PN20261005_02', 'Trong_kho', '2026-10-05 14:15:00'),
('864219082345674', 'SP_SS_S24U_256AM', 'PN20261005_02', 'Trong_kho', '2026-10-05 14:15:00'),

-- 3 máy Xiaomi 14
('861054073456781', 'SP_MI_14_256TRANG', 'PN20261010_03', 'Trong_kho', '2026-10-10 10:00:00'),
('861054073456782', 'SP_MI_14_256TRANG', 'PN20261010_03', 'Trong_kho', '2026-10-10 10:00:00'),
('861054073456783', 'SP_MI_14_256TRANG', 'PN20261010_03', 'Trong_kho', '2026-10-10 10:00:00');

-- 11. Dữ liệu khách hàng
INSERT INTO khach_hang (ma_khach_hang, ho_ten, so_dien_thoai, email, dia_chi, diem_tich_luy) VALUES
('KH001', 'Trần Văn An', '0981112233', 'an.tran@gmail.com', '12 Nguyễn Trãi, Q.5, TP.HCM', 120),
('KH002', 'Nguyễn Thị Bích', '0982223344', 'bich.nguyen@yahoo.com', '45 Hùng Vương, Q.5, TP.HCM', 250),
('KH003', 'Phạm Quốc Cường', '0983334455', 'cuong.pham@outlook.com', '78 Lý Thường Kiệt, Q.10, TP.HCM', 50),
('KH004', 'Vũ Mai Linh', '0984445566', 'linh.vu@gmail.com', '101 Cách Mạng Tháng 8, Q.3, TP.HCM', 300);

-- 12. Dữ liệu chương trình khuyến mãi & Voucher
INSERT INTO khuyen_mai (ma_khuyen_mai, code_voucher, ten_khuyen_mai, loai_giam_gia, gia_tri_giam, gia_tri_don_hang_toi_thieu, giam_toi_da, so_luong_phat_hanh, so_luong_da_dung, ngay_bat_dau, ngay_ket_thuc, ma_thuong_hieu_ap_dung, trang_thai) VALUES
('KM_CHAOTHU2026', 'CHAOTHU26', 'Chào Thu 2026 - Giảm 500k cho đơn từ 20tr', 'Tien_mat', 500000, 20000000, 500000, 100, 2, '2026-09-01 00:00:00', '2026-11-30 23:59:59', NULL, 'Dang_dien_ra'),
('KM_APPLE_VIP', 'APPLEVip5', 'Tri ân tín đồ Apple - Giảm 5% dòng iPhone', 'Phan_tram', 5, 25000000, 1500000, 50, 1, '2026-10-01 00:00:00', '2026-10-31 23:59:59', 1, 'Dang_dien_ra'),
('KM_SS_MEMBER', 'SAMSUNGS24', 'Ưu đãi đặt trước Galaxy S24 Series', 'Tien_mat', 1000000, 25000000, 1000000, 30, 1, '2026-10-01 00:00:00', '2026-10-25 23:59:59', 2, 'Dang_dien_ra');

-- 13. Dữ liệu Đơn hàng mẫu
INSERT INTO don_hang (ma_don_hang, ma_khach_hang, ma_nhan_vien, ma_khuyen_mai, ngay_tao, tong_tien_hang, tien_giam_gia, thue_vat, tong_tien_can_tra, trang_thai, ghi_chu) VALUES
('DH20261012_001', 'KH001', 'NV003', 'KM_APPLE_VIP', '2026-10-12 10:15:00', 29990000, 1499500, 0, 28490500, 'Da_thanh_toan', 'Khách áp dụng mã ưu đãi Apple'),
('DH20261014_002', 'KH002', 'NV003', 'KM_SS_MEMBER', '2026-10-14 15:45:00', 27990000, 1000000, 0, 26990000, 'Da_thanh_toan', 'Khách mua Galaxy S24 Ultra thanh toán tiền mặt');

-- 14. Dữ liệu Chi tiết đơn hàng
INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_san_pham, so_luong, don_gia) VALUES
('DH20261012_001', 'SP_IP15PM_256TN', 1, 29990000),
('DH20261014_002', 'SP_SS_S24U_256AM', 1, 27990000);

-- 15. Dữ liệu Hóa đơn bán lẻ
INSERT INTO hoa_don (ma_hoa_don, ma_don_hang, ma_khach_hang, ma_nhan_vien, ngay_lap, tong_tien_hang, thue_vat_phan_tram, tien_thue_vat, tien_giam_gia, tong_thanh_toan, phuong_thuc_thanh_toan, tien_khach_dua, tien_thua, trang_thai_thanh_toan, ghi_chu) VALUES
('HD20261012_001', 'DH20261012_001', 'KH001', 'NV003', '2026-10-12 10:20:00', 29990000, 0, 0, 1499500, 28490500, 'Chuyen_khoan_QR', 28490500, 0, 'Thanh_cong', 'Thanh toán qua quét mã VietQR'),
('HD20261014_002', 'DH20261014_002', 'KH002', 'NV003', '2026-10-14 15:50:00', 27990000, 0, 0, 1000000, 26990000, 'Tien_mat', 27000000, 10000, 'Thanh_cong', 'Khách đưa 27 triệu, thối lại 10.000đ');

-- 16. Dữ liệu Chi tiết hóa đơn (Gắn mã IMEI bán ra -> Tự động kích hoạt Trigger 2 trừ tồn kho & cập nhật IMEI)
INSERT INTO chi_tiet_hoa_don (ma_hoa_don, ma_san_pham, ma_imei, don_gia, thoi_gian_bao_hanh_thang) VALUES
('HD20261012_001', 'SP_IP15PM_256TN', '358941091234561', 29990000, 12),
('HD20261014_002', 'SP_SS_S24U_256AM', '864219082345671', 27990000, 12);

-- 17. Dữ liệu Phiếu bảo hành (Tiếp nhận máy có IMEI '358941091234561' bị lỗi loa rè - Use Case 2.13)
INSERT INTO phieu_bao_hanh (ma_phieu_bao_hanh, ma_imei, ma_san_pham, ma_khach_hang, ma_hoa_don, ma_nhan_vien_tiep_nhan, ma_ky_thuat_vien, ngay_tiep_nhan, tinh_trang_tiep_nhan, loi_khach_phan_anh, chan_doan_ky_thuat, noi_dung_sua_chua, chi_phi_sua_chua, ngay_hen_tra, trang_thai, ghi_chu) VALUES
('PBH20261016_01', '358941091234561', 'SP_IP15PM_256TN', 'KH001', 'HD20261012_001', 'NV003', 'NV005', '2026-10-16 09:00:00', 'Máy còn đẹp 99%, không trầy cấn, tem niêm phong nguyên vẹn', 'Loa trong bị rè khi đàm thoại', 'Bụi bẩn bám màng loa, cần vệ sinh và cân chỉnh âm thanh', 'Đã vệ sinh màng loa âm tầng, kiểm tra âm thanh đạt chuẩn', 0.00, '2026-10-17', 'Cho_tra_khach', 'Bảo hành miễn phí theo chính sách');

-- 18. Dữ liệu Phiếu kiểm kê kho (Use Case 2.6)
INSERT INTO phieu_kiem_ke (ma_phieu_kiem_ke, ma_nhan_vien, ngay_kiem_ke, trang_thai, ghi_chu) VALUES
('PKK20261015_01', 'NV004', '2026-10-15 17:30:00', 'Da_can_bang', 'Kiểm kê định kỳ giữa tháng 10/2026');

INSERT INTO chi_tiet_kiem_ke (ma_phieu_kiem_ke, ma_san_pham, so_luong_he_thong, so_luong_thuc_te, ly_do_chenh_lech) VALUES
('PKK20261015_01', 'SP_IP15PM_256TN', 4, 4, 'Khớp dữ liệu'),
('PKK20261015_01', 'SP_SS_S24U_256AM', 3, 3, 'Khớp dữ liệu');

-- 19. Dữ liệu Nhật ký xuất báo cáo (Use Case 2.16)
INSERT INTO nhat_ky_bao_cao (loai_bao_cao, ma_nhan_vien_lap, tu_ngay, den_ngay, dinh_dang_xuat, duong_dan_file, ghi_chu) VALUES
('Doanh_thu', 'NV002', '2026-10-01', '2026-10-15', 'EXCEL', '/reports/doanh_thu_nua_dau_t10.xlsx', 'Báo cáo doanh thu nửa đầu tháng 10 cho ban giám đốc'),
('SanPham_BanChay', 'NV002', '2026-10-01', '2026-10-15', 'PDF', '/reports/top_banchay_t10.pdf', 'Danh sách sản phẩm tiêu thụ tốt nhất');

-- =================================================================================================
-- KIỂM TRA DỮ LIỆU SAU KHI KHỞI TẠO
-- =================================================================================================
SELECT '=== THÀNH CÔNG: ĐÃ KHỞI TẠO HOÀN CHỈNH DATABASE QUẢN LÝ CỬA HÀNG ĐIỆN THOẠI CHO XAMPP ===' AS ThongBao;
