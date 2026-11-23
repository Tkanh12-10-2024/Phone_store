-- H2 (MODE=MySQL) initialization script matching quanly_cuahang_dienthoai.sql + UC-23..UC-28 extensions
-- Dùng cho kiểm thử tự động (JUnit) và chế độ demo H2 (--spring.profiles.active=h2)

CREATE TABLE IF NOT EXISTS vai_tro (
                                       ma_vai_tro INT AUTO_INCREMENT PRIMARY KEY,
                                       ten_vai_tro VARCHAR(50) NOT NULL UNIQUE,
    mo_ta VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS nhan_vien (
                                         ma_nhan_vien VARCHAR(20) PRIMARY KEY,
    ho_ten VARCHAR(100) NOT NULL,
    gioi_tinh VARCHAR(10) DEFAULT 'Nam',
    ngay_sinh DATE,
    so_dien_thoai VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(100) UNIQUE,
    dia_chi VARCHAR(255),
    chuc_vu VARCHAR(50) NOT NULL,
    ngay_vao_lam DATE NOT NULL DEFAULT CURRENT_DATE,
    luong_co_ban DECIMAL(12,2) DEFAULT 0,
    trang_thai VARCHAR(30) NOT NULL DEFAULT 'Dang_lam_viec',
    mat_khau VARCHAR(255) DEFAULT '123456',
    ten_dang_nhap VARCHAR(50)
    );

CREATE TABLE IF NOT EXISTS tai_khoan (
                                         ma_tai_khoan INT AUTO_INCREMENT PRIMARY KEY,
                                         ma_nhan_vien VARCHAR(20) NOT NULL UNIQUE,
    ten_dang_nhap VARCHAR(50) NOT NULL UNIQUE,
    mat_khau VARCHAR(255) NOT NULL,
    ma_vai_tro INT NOT NULL,
    trang_thai VARCHAR(30) NOT NULL DEFAULT 'Hoat_dong',
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    lan_dang_nhap_cuoi DATETIME,
    CONSTRAINT fk_tk_nv FOREIGN KEY (ma_nhan_vien) REFERENCES nhan_vien(ma_nhan_vien),
    CONSTRAINT fk_tk_vt FOREIGN KEY (ma_vai_tro) REFERENCES vai_tro(ma_vai_tro)
    );

CREATE TABLE IF NOT EXISTS thuong_hieu (
                                           ma_thuong_hieu INT AUTO_INCREMENT PRIMARY KEY,
                                           ten_thuong_hieu VARCHAR(50) NOT NULL UNIQUE,
    quoc_gia VARCHAR(50) DEFAULT 'Quốc tế',
    logo_url VARCHAR(255),
    mo_ta TEXT
    );

CREATE TABLE IF NOT EXISTS danh_muc (
                                        ma_danh_muc INT AUTO_INCREMENT PRIMARY KEY,
                                        ten_danh_muc VARCHAR(100) NOT NULL UNIQUE,
    mo_ta VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS san_pham (
                                        ma_san_pham VARCHAR(20) PRIMARY KEY,
    ten_san_pham VARCHAR(150) NOT NULL,
    ma_thuong_hieu INT NOT NULL,
    ma_danh_muc INT NOT NULL,
    dong_may VARCHAR(100) NOT NULL,
    mau_sac VARCHAR(50) NOT NULL,
    ram VARCHAR(20) NOT NULL,
    rom VARCHAR(20) NOT NULL,
    man_hinh VARCHAR(100),
    chip_xu_ly VARCHAR(100),
    pin VARCHAR(50),
    camera VARCHAR(100),
    he_dieu_hanh VARCHAR(50),
    gia_nhap_tham_khao DECIMAL(14,2) NOT NULL DEFAULT 0,
    gia_ban DECIMAL(14,2) NOT NULL DEFAULT 0,
    thoi_gian_bao_hanh_thang INT NOT NULL DEFAULT 12,
    so_luong_ton INT NOT NULL DEFAULT 0,
    dinh_muc_ton_toi_thieu INT NOT NULL DEFAULT 2,
    hinh_anh VARCHAR(255),
    trang_thai VARCHAR(30) NOT NULL DEFAULT 'Dang_kinh_doanh',
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sp_th FOREIGN KEY (ma_thuong_hieu) REFERENCES thuong_hieu(ma_thuong_hieu),
    CONSTRAINT fk_sp_dm FOREIGN KEY (ma_danh_muc) REFERENCES danh_muc(ma_danh_muc)
    );

CREATE TABLE IF NOT EXISTS hinh_anh_san_pham (
                                                 ma_hinh_anh INT AUTO_INCREMENT PRIMARY KEY,
                                                 ma_san_pham VARCHAR(20) NOT NULL,
    duong_dan_anh VARCHAR(500) NOT NULL,
    la_anh_chinh BOOLEAN NOT NULL DEFAULT FALSE,
    thu_tu INT NOT NULL DEFAULT 1,
    ngay_tao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gallery_san_pham FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham)
    );

CREATE TABLE IF NOT EXISTS nha_cung_cap (
                                            ma_nha_cung_cap VARCHAR(20) PRIMARY KEY,
    ten_nha_cung_cap VARCHAR(150) NOT NULL,
    so_dien_thoai VARCHAR(15) NOT NULL,
    email VARCHAR(100),
    dia_chi VARCHAR(255) NOT NULL,
    ma_so_thue VARCHAR(30),
    cong_no DECIMAL(14,2) DEFAULT 0.00,
    trang_thai VARCHAR(30) NOT NULL DEFAULT 'Dang_hop_tac'
    );

CREATE TABLE IF NOT EXISTS phieu_nhap (
                                          ma_phieu_nhap VARCHAR(30) PRIMARY KEY,
    ma_nha_cung_cap VARCHAR(20) NOT NULL,
    ma_nhan_vien VARCHAR(20) NOT NULL,
    ngay_nhap DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tong_tien DECIMAL(14,2) NOT NULL DEFAULT 0,
    da_thanh_toan DECIMAL(14,2) NOT NULL DEFAULT 0,
    cong_no_con_lai DECIMAL(14,2) DEFAULT 0,
    trang_thai VARCHAR(30) NOT NULL DEFAULT 'Hoan_thanh',
    ghi_chu TEXT,
    CONSTRAINT fk_pn_ncc FOREIGN KEY (ma_nha_cung_cap) REFERENCES nha_cung_cap(ma_nha_cung_cap),
    CONSTRAINT fk_pn_nv FOREIGN KEY (ma_nhan_vien) REFERENCES nhan_vien(ma_nhan_vien)
    );

CREATE TABLE IF NOT EXISTS chi_tiet_phieu_nhap (
                                                   ma_chi_tiet_nhap INT AUTO_INCREMENT PRIMARY KEY,
                                                   ma_phieu_nhap VARCHAR(30) NOT NULL,
    ma_san_pham VARCHAR(20) NOT NULL,
    so_luong INT NOT NULL,
    don_gia_nhap DECIMAL(14,2) NOT NULL,
    thanh_tien DECIMAL(14,2) DEFAULT 0,
    CONSTRAINT fk_ctpn_pn FOREIGN KEY (ma_phieu_nhap) REFERENCES phieu_nhap(ma_phieu_nhap) ON DELETE CASCADE,
    CONSTRAINT fk_ctpn_sp FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham)
    );

CREATE TABLE IF NOT EXISTS imei_san_pham (
                                             ma_imei VARCHAR(30) PRIMARY KEY,
    ma_san_pham VARCHAR(20) NOT NULL,
    ma_phieu_nhap VARCHAR(30) NOT NULL,
    ma_hoa_don VARCHAR(30),
    trang_thai VARCHAR(30) NOT NULL DEFAULT 'Trong_kho',
    ngay_nhap DATETIME DEFAULT CURRENT_TIMESTAMP,
    ngay_ban DATETIME,
    han_bao_hanh DATE,
    CONSTRAINT fk_im_sp FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham),
    CONSTRAINT fk_im_pn FOREIGN KEY (ma_phieu_nhap) REFERENCES phieu_nhap(ma_phieu_nhap)
    );

CREATE TABLE IF NOT EXISTS phieu_kiem_ke (
                                             ma_phieu_kiem_ke VARCHAR(30) PRIMARY KEY,
    ma_nhan_vien VARCHAR(20) NOT NULL,
    ngay_kiem_ke DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trang_thai VARCHAR(30) DEFAULT 'Da_can_bang',
    ghi_chu TEXT,
    CONSTRAINT fk_kk_nv FOREIGN KEY (ma_nhan_vien) REFERENCES nhan_vien(ma_nhan_vien)
    );

CREATE TABLE IF NOT EXISTS chi_tiet_kiem_ke (
                                                ma_chi_tiet_kiem_ke INT AUTO_INCREMENT PRIMARY KEY,
                                                ma_phieu_kiem_ke VARCHAR(30) NOT NULL,
    ma_san_pham VARCHAR(20) NOT NULL,
    so_luong_he_thong INT NOT NULL,
    so_luong_thuc_te INT NOT NULL,
    chenh_lech INT DEFAULT 0,
    ly_do_chenh_lech VARCHAR(255),
    CONSTRAINT fk_ctkk_p FOREIGN KEY (ma_phieu_kiem_ke) REFERENCES phieu_kiem_ke(ma_phieu_kiem_ke) ON DELETE CASCADE,
    CONSTRAINT fk_ctkk_sp FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham)
    );

CREATE TABLE IF NOT EXISTS khach_hang (
                                          ma_khach_hang VARCHAR(20) PRIMARY KEY,
    ho_ten VARCHAR(100) NOT NULL,
    so_dien_thoai VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(100),
    dia_chi VARCHAR(255),
    diem_tich_luy INT DEFAULT 0,
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    trang_thai VARCHAR(30) NOT NULL DEFAULT 'Hoat_dong'
    );

CREATE TABLE IF NOT EXISTS khuyen_mai (
                                          ma_khuyen_mai VARCHAR(30) PRIMARY KEY,
    code_voucher VARCHAR(50) NOT NULL UNIQUE,
    ten_khuyen_mai VARCHAR(150) NOT NULL,
    loai_giam_gia VARCHAR(30) NOT NULL DEFAULT 'Phan_tram',
    gia_tri_giam DECIMAL(14,2) NOT NULL DEFAULT 0,
    gia_tri_don_hang_toi_thieu DECIMAL(14,2) NOT NULL DEFAULT 0,
    giam_toi_da DECIMAL(14,2),
    qua_tang VARCHAR(255),
    so_luong_phat_hanh INT NOT NULL DEFAULT 100,
    so_luong_da_dung INT NOT NULL DEFAULT 0,
    ngay_bat_dau DATETIME NOT NULL,
    ngay_ket_thuc DATETIME NOT NULL,
    ma_thuong_hieu_ap_dung INT,
    dong_may_ap_dung VARCHAR(100),
    ma_danh_muc_ap_dung INT,
    doi_tuong_khach_hang VARCHAR(30) NOT NULL DEFAULT 'Tat_ca',
    mo_ta VARCHAR(500),
    trang_thai VARCHAR(30) NOT NULL DEFAULT 'Dang_dien_ra',
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_km_th FOREIGN KEY (ma_thuong_hieu_ap_dung) REFERENCES thuong_hieu(ma_thuong_hieu)
    );

CREATE TABLE IF NOT EXISTS don_hang (
                                        ma_don_hang VARCHAR(30) PRIMARY KEY,
    ma_khach_hang VARCHAR(20),
    ma_nhan_vien VARCHAR(20) NOT NULL,
    ma_khuyen_mai VARCHAR(30),
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tong_tien_hang DECIMAL(14,2) NOT NULL DEFAULT 0,
    tien_giam_gia DECIMAL(14,2) NOT NULL DEFAULT 0,
    thue_vat DECIMAL(14,2) NOT NULL DEFAULT 0,
    tong_tien_can_tra DECIMAL(14,2) NOT NULL DEFAULT 0,
    trang_thai VARCHAR(30) NOT NULL DEFAULT 'Cho_thanh_toan',
    ghi_chu TEXT,
    CONSTRAINT fk_dh_kh FOREIGN KEY (ma_khach_hang) REFERENCES khach_hang(ma_khach_hang),
    CONSTRAINT fk_dh_nv FOREIGN KEY (ma_nhan_vien) REFERENCES nhan_vien(ma_nhan_vien),
    CONSTRAINT fk_dh_km FOREIGN KEY (ma_khuyen_mai) REFERENCES khuyen_mai(ma_khuyen_mai)
    );

CREATE TABLE IF NOT EXISTS chi_tiet_don_hang (
                                                 ma_chi_tiet_don INT AUTO_INCREMENT PRIMARY KEY,
                                                 ma_don_hang VARCHAR(30) NOT NULL,
    ma_san_pham VARCHAR(20) NOT NULL,
    so_luong INT NOT NULL,
    don_gia DECIMAL(14,2) NOT NULL,
    thanh_tien DECIMAL(14,2) DEFAULT 0,
    CONSTRAINT fk_ctdh_dh FOREIGN KEY (ma_don_hang) REFERENCES don_hang(ma_don_hang) ON DELETE CASCADE,
    CONSTRAINT fk_ctdh_sp FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham)
    );

CREATE TABLE IF NOT EXISTS hoa_don (
                                       ma_hoa_don VARCHAR(30) PRIMARY KEY,
    ma_don_hang VARCHAR(30) NOT NULL UNIQUE,
    ma_khach_hang VARCHAR(20),
    ma_nhan_vien VARCHAR(20) NOT NULL,
    ngay_lap DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tong_tien_hang DECIMAL(14,2) NOT NULL,
    thue_vat_phan_tram DECIMAL(5,2) DEFAULT 0.00,
    tien_thue_vat DECIMAL(14,2) DEFAULT 0.00,
    tien_giam_gia DECIMAL(14,2) DEFAULT 0.00,
    tong_thanh_toan DECIMAL(14,2) NOT NULL,
    phuong_thuc_thanh_toan VARCHAR(30) NOT NULL DEFAULT 'Tien_mat',
    tien_khach_dua DECIMAL(14,2) NOT NULL,
    tien_thua DECIMAL(14,2) DEFAULT 0.00,
    trang_thai_thanh_toan VARCHAR(30) NOT NULL DEFAULT 'Thanh_cong',
    ghi_chu TEXT,
    CONSTRAINT fk_hd_dh FOREIGN KEY (ma_don_hang) REFERENCES don_hang(ma_don_hang),
    CONSTRAINT fk_hd_kh FOREIGN KEY (ma_khach_hang) REFERENCES khach_hang(ma_khach_hang),
    CONSTRAINT fk_hd_nv FOREIGN KEY (ma_nhan_vien) REFERENCES nhan_vien(ma_nhan_vien)
    );

CREATE TABLE IF NOT EXISTS chi_tiet_hoa_don (
                                                ma_chi_tiet_hd INT AUTO_INCREMENT PRIMARY KEY,
                                                ma_hoa_don VARCHAR(30) NOT NULL,
    ma_san_pham VARCHAR(20) NOT NULL,
    ma_imei VARCHAR(30) NOT NULL UNIQUE,
    don_gia DECIMAL(14,2) NOT NULL,
    thoi_gian_bao_hanh_thang INT NOT NULL DEFAULT 12,
    CONSTRAINT fk_cthd_hd FOREIGN KEY (ma_hoa_don) REFERENCES hoa_don(ma_hoa_don) ON DELETE CASCADE,
    CONSTRAINT fk_cthd_sp FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham),
    CONSTRAINT fk_cthd_im FOREIGN KEY (ma_imei) REFERENCES imei_san_pham(ma_imei)
    );

CREATE TABLE IF NOT EXISTS phieu_bao_hanh (
                                              ma_phieu_bao_hanh VARCHAR(30) PRIMARY KEY,
    ma_imei VARCHAR(30),
    ma_san_pham VARCHAR(20),
    ma_khach_hang VARCHAR(20) NOT NULL,
    ma_hoa_don VARCHAR(30),
    ma_nhan_vien_tiep_nhan VARCHAR(20) NOT NULL,
    ma_ky_thuat_vien VARCHAR(20),
    ngay_tiep_nhan DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tinh_trang_tiep_nhan TEXT NOT NULL,
    loi_khach_phan_anh TEXT NOT NULL,
    chan_doan_ky_thuat TEXT,
    noi_dung_sua_chua TEXT,
    chi_phi_sua_chua DECIMAL(14,2) DEFAULT 0.00,
    ngay_hen_tra DATE,
    ngay_tra_thuc_te DATETIME,
    trang_thai VARCHAR(30) NOT NULL DEFAULT 'Tiep_nhan',
    ghi_chu TEXT
    );

CREATE TABLE IF NOT EXISTS lich_su_bao_hanh (
                                                ma_lich_su INT AUTO_INCREMENT PRIMARY KEY,
                                                ma_phieu_bao_hanh VARCHAR(30) NOT NULL,
    ma_nhan_vien_cap_nhat VARCHAR(20) NOT NULL,
    thoi_gian_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trang_thai_moi VARCHAR(30) NOT NULL,
    ghi_chu_tien_do TEXT NOT NULL
    );

CREATE TABLE IF NOT EXISTS nhat_ky_bao_cao (
                                               ma_bao_cao INT AUTO_INCREMENT PRIMARY KEY,
                                               loai_bao_cao VARCHAR(50) NOT NULL,
    ma_nhan_vien_lap VARCHAR(20) NOT NULL,
    ngay_lap DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tu_ngay DATE,
    den_ngay DATE,
    dinh_dang_xuat VARCHAR(20) NOT NULL DEFAULT 'EXCEL',
    duong_dan_file VARCHAR(255),
    tieu_chi_loc VARCHAR(500),
    ghi_chu VARCHAR(255),
    CONSTRAINT fk_bc_nv FOREIGN KEY (ma_nhan_vien_lap) REFERENCES nhan_vien(ma_nhan_vien)
    );

-- ============================================================================
-- SEED DATA FOR H2 TEST & DEMO
-- ============================================================================
MERGE INTO vai_tro (ma_vai_tro, ten_vai_tro, mo_ta) KEY(ma_vai_tro) VALUES
    (1, 'Admin', 'Quản trị hệ thống'),
    (2, 'Quản lý', 'Quản lý cửa hàng & khuyến mãi & báo cáo'),
    (3, 'Nhân viên bán hàng', 'Thu ngân & POS'),
    (4, 'Nhân viên kho', 'Quản lý nhập kho & kiểm kê');

MERGE INTO nhan_vien (ma_nhan_vien, ho_ten, gioi_tinh, so_dien_thoai, email, chuc_vu, luong_co_ban, trang_thai, ten_dang_nhap, mat_khau) KEY(ma_nhan_vien) VALUES
    ('NV001', 'Hoàng Mạnh Hà', 'Nam', '0901234567', 'admin@cuahangdienthoai.vn', 'Quản lý', 25000000, 'Dang_lam_viec', 'admin', '123456'),
    ('NV002', 'Lê Tạ Khánh', 'Nam', '0912345678', 'khanh.lt@cuahangdienthoai.vn', 'Quản lý', 18000000, 'Dang_lam_viec', 'quanly', 'quanly123'),
    ('NV003', 'Đặng Hoàng Nam', 'Nam', '0923456789', 'nam.dh@cuahangdienthoai.vn', 'Bán hàng', 11000000, 'Dang_lam_viec', 'banhang_nam', '123456'),
    ('NV004', 'Nguyễn Thành Sáng', 'Nam', '0934567890', 'sang.nt@cuahangdienthoai.vn', 'Kho', 12000000, 'Dang_lam_viec', 'kho_sang', '123456'),
    ('NV006', 'Huỳnh Trọng Duy', 'Nam', '0956789012', 'duy.ht@cuahangdienthoai.vn', 'Bán hàng', 10500000, 'Dang_lam_viec', 'duy_banhang', '123456');

MERGE INTO tai_khoan (ma_tai_khoan, ma_nhan_vien, ten_dang_nhap, mat_khau, ma_vai_tro, trang_thai) KEY(ma_tai_khoan) VALUES
    (1, 'NV001', 'admin', 'admin123', 1, 'Hoat_dong'),
    (2, 'NV002', 'quanly', 'quanly123', 2, 'Hoat_dong'),
    (3, 'NV003', 'banhang_nam', '123456', 3, 'Hoat_dong'),
    (4, 'NV004', 'kho_sang', '123456', 4, 'Hoat_dong');

MERGE INTO thuong_hieu (ma_thuong_hieu, ten_thuong_hieu, quoc_gia, mo_ta) KEY(ma_thuong_hieu) VALUES
    (1, 'Apple', 'Mỹ', 'Điện thoại iPhone chính hãng VN/A'),
    (2, 'Samsung', 'Hàn Quốc', 'Galaxy S & Z Series chính hãng'),
    (3, 'Xiaomi', 'Trung Quốc', 'Flagship & Redmi chính hãng'),
    (4, 'OPPO', 'Trung Quốc', 'Find N & Reno Series');

MERGE INTO danh_muc (ma_danh_muc, ten_danh_muc, mo_ta) KEY(ma_danh_muc) VALUES
    (1, 'Flagship Cao Cấp', 'Smartphone hiệu năng cao'),
    (2, 'Tầm Trung Gaming', 'Hiệu năng chơi game tốt'),
    (3, 'Chuyên Chụp Ảnh', 'Camera chân dung'),
    (4, 'Màn Hình Gập', 'Foldable cao cấp');

MERGE INTO san_pham (ma_san_pham, ten_san_pham, ma_thuong_hieu, ma_danh_muc, dong_may, mau_sac, ram, rom, man_hinh, chip_xu_ly, pin, camera, he_dieu_hanh, gia_nhap_tham_khao, gia_ban, thoi_gian_bao_hanh_thang, so_luong_ton, dinh_muc_ton_toi_thieu, hinh_anh, trang_thai) KEY(ma_san_pham) VALUES
    ('SP_IP15PM_256TN', 'iPhone 15 Pro Max 256GB Titan Tự Nhiên', 1, 1, 'iPhone 15 Series', 'Titan Tự Nhiên', '8GB', '256GB', 'OLED 6.7 inch 120Hz', 'Apple A17 Pro', '4422 mAh', '48MP + 12MP + 12MP', 'iOS 17', 26500000, 29990000, 12, 12, 3, '/uploads/products/iphone-15-pro-max.svg', 'Dang_kinh_doanh'),
    ('SP_IP15PM_512DEN', 'iPhone 15 Pro Max 512GB Titan Đen', 1, 1, 'iPhone 15 Series', 'Titan Đen', '8GB', '512GB', 'OLED 6.7 inch 120Hz', 'Apple A17 Pro', '4422 mAh', '48MP + 12MP + 12MP', 'iOS 17', 31000000, 34990000, 12, 1, 3, '/uploads/products/iphone-15-pro-max.svg', 'Dang_kinh_doanh'),
    ('SP_SS_S24U_256AM', 'Samsung Galaxy S24 Ultra 256GB Xám Titan', 2, 1, 'Galaxy S24 Series', 'Xám Titan', '12GB', '256GB', 'Dynamic AMOLED 2X 6.8 inch', 'Snapdragon 8 Gen 3', '5000 mAh', '200MP + 50MP + 12MP', 'Android 14', 24000000, 27990000, 12, 8, 3, '/uploads/products/galaxy-s24-ultra.svg', 'Dang_kinh_doanh'),
    ('SP_SS_ZFLIP5_256', 'Samsung Galaxy Z Flip5 256GB Xanh Mint', 2, 4, 'Galaxy Z Series', 'Xanh Mint', '8GB', '256GB', 'AMOLED 6.7 inch', 'Snapdragon 8 Gen 2', '3700 mAh', '12MP + 12MP', 'Android 14', 15500000, 18990000, 12, 2, 4, '/uploads/products/galaxy-s24-ultra.svg', 'Dang_kinh_doanh'),
    ('SP_MI_14_256TRANG', 'Xiaomi 14 256GB Trắng Tuyết', 3, 1, 'Xiaomi 14 Series', 'Trắng Tuyết', '12GB', '256GB', 'LTPO AMOLED 6.36 inch', 'Snapdragon 8 Gen 3', '4610 mAh', '50MP Leica', 'HyperOS', 17000000, 22990000, 12, 10, 3, '/uploads/products/xiaomi-14.svg', 'Dang_kinh_doanh'),
    ('SP_OPPO_RENO11_256', 'OPPO Reno11 5G 256GB Xanh Sóng Biển', 4, 3, 'Reno Series', 'Xanh Sóng Biển', '8GB', '256GB', 'AMOLED 6.7 inch', 'Dimensity 7050', '5000 mAh', '50MP + 32MP', 'ColorOS 14', 8500000, 10990000, 12, 5, 2, '/uploads/products/oppo-reno11.svg', 'Dang_kinh_doanh'),
    ('SP_OPPO_FINDN3', 'OPPO Find N3 5G 512GB Vàng Champange', 4, 4, 'Find N Series', 'Vàng', '16GB', '512GB', 'AMOLED 7.8 inch 120Hz', 'Snapdragon 8 Gen 2', '4805 mAh', '48MP + 64MP + 48MP', 'Android 14', 32000000, 38990000, 12, 4, 2, '/uploads/products/oppo-reno11.svg', 'Dang_kinh_doanh'),
    ('SP_XIAOMI_14U_OUT', 'Xiaomi 14 Ultra 512GB Đen Da', 3, 1, 'Xiaomi 14 Series', 'Đen', '16GB', '512GB', 'AMOLED 6.73 inch 120Hz', 'Snapdragon 8 Gen 3', '5000 mAh', '50MP Leica Quad', 'Android 14', 25000000, 29990000, 12, 0, 2, '/uploads/products/xiaomi-14.svg', 'Dang_kinh_doanh');

MERGE INTO hinh_anh_san_pham (ma_hinh_anh, ma_san_pham, duong_dan_anh, la_anh_chinh, thu_tu) KEY(ma_hinh_anh) VALUES
    (1, 'SP_IP15PM_256TN', '/uploads/products/iphone-15-pro-max.svg', TRUE, 1),
    (2, 'SP_IP15PM_512DEN', '/uploads/products/iphone-15-pro-max.svg', TRUE, 1),
    (3, 'SP_SS_S24U_256AM', '/uploads/products/galaxy-s24-ultra.svg', TRUE, 1),
    (4, 'SP_SS_ZFLIP5_256', '/uploads/products/galaxy-s24-ultra.svg', TRUE, 1),
    (5, 'SP_MI_14_256TRANG', '/uploads/products/xiaomi-14.svg', TRUE, 1),
    (6, 'SP_OPPO_RENO11_256', '/uploads/products/oppo-reno11.svg', TRUE, 1),
    (7, 'SP_OPPO_FINDN3', '/uploads/products/oppo-reno11.svg', TRUE, 1),
    (8, 'SP_XIAOMI_14U_OUT', '/uploads/products/xiaomi-14.svg', TRUE, 1);

MERGE INTO nha_cung_cap (ma_nha_cung_cap, ten_nha_cung_cap, so_dien_thoai, email, dia_chi, ma_so_thue, cong_no) KEY(ma_nha_cung_cap) VALUES
    ('NCC01', 'Công ty Cổ phần Phân phối Synnex FPT', '02873001010', 'contact@fpt.com.vn', 'Tòa nhà FPT Tân Thuận, Q.7, TP.HCM', '0101234567', 32500000),
    ('NCC02', 'Công ty Cổ phần Thế Giới Số (Digiworld)', '02839290059', 'info@digiworld.com.vn', '195 Điện Biên Phủ, Bình Thạnh, TP.HCM', '0302345678', 0),
    ('NCC03', 'Công ty TNHH Phân phối Dầu khí (PHTD)', '02839115588', 'support@phtd.com.vn', 'Q.1, TP.HCM', '0303456789', 0);

MERGE INTO phieu_nhap (ma_phieu_nhap, ma_nha_cung_cap, ma_nhan_vien, ngay_nhap, tong_tien, da_thanh_toan, cong_no_con_lai, trang_thai, ghi_chu) KEY(ma_phieu_nhap) VALUES
    ('PN20261001_01', 'NCC01', 'NV004', '2026-10-01 09:30:00', 132500000, 100000000, 32500000, 'Hoan_thanh', 'Nhập lô iPhone 15 Pro Max'),
    ('PN20261005_02', 'NCC02', 'NV004', '2026-10-05 14:15:00', 96000000, 96000000, 0, 'Hoan_thanh', 'Nhập lô Samsung Galaxy'),
    ('PN20261010_03', 'NCC03', 'NV004', '2026-10-10 10:00:00', 51000000, 51000000, 0, 'Hoan_thanh', 'Nhập Xiaomi & OPPO'),
    ('PN_OLD_2026_01', 'NCC03', 'NV004', DATEADD('DAY', -78, CURRENT_TIMESTAMP), 128000000, 128000000, 0, 'Hoan_thanh', 'Nhập lô OPPO Find N3 tồn kho lâu (>60 ngày)'),
    ('PN_NOV_2026_04', 'NCC01', 'NV004', DATEADD('DAY', -3, CURRENT_TIMESTAMP), 106000000, 106000000, 0, 'Hoan_thanh', 'Bổ sung iPhone 15 Pro Max');

MERGE INTO imei_san_pham (ma_imei, ma_san_pham, ma_phieu_nhap, ma_hoa_don, trang_thai, ngay_nhap, ngay_ban, han_bao_hanh) KEY(ma_imei) VALUES
    ('358941091234561', 'SP_IP15PM_256TN', 'PN20261001_01', 'HD20261012_001', 'Da_ban', '2026-10-01 09:30:00', '2026-10-12 10:20:00', '2027-10-12'),
    ('358941091234562', 'SP_IP15PM_256TN', 'PN20261001_01', NULL, 'Trong_kho', DATEADD('DAY', -12, CURRENT_TIMESTAMP), NULL, NULL),
    ('358941091234563', 'SP_IP15PM_256TN', 'PN20261001_01', NULL, 'Trong_kho', DATEADD('DAY', -12, CURRENT_TIMESTAMP), NULL, NULL),
    ('358941091234566', 'SP_IP15PM_256TN', 'PN_NOV_2026_04', NULL, 'Trong_kho', DATEADD('DAY', -3, CURRENT_TIMESTAMP), NULL, NULL),
    ('358941091234567', 'SP_IP15PM_256TN', 'PN_NOV_2026_04', NULL, 'Trong_kho', DATEADD('DAY', -3, CURRENT_TIMESTAMP), NULL, NULL),
    ('358941099900111', 'SP_IP15PM_512DEN', 'PN20261001_01', NULL, 'Trong_kho', DATEADD('DAY', -25, CURRENT_TIMESTAMP), NULL, NULL),
    ('864219082345671', 'SP_SS_S24U_256AM', 'PN20261005_02', 'HD20261014_002', 'Da_ban', '2026-10-05 14:15:00', '2026-10-14 15:50:00', '2027-10-14'),
    ('864219082345675', 'SP_SS_S24U_256AM', 'PN20261005_02', NULL, 'Trong_kho', DATEADD('DAY', -20, CURRENT_TIMESTAMP), NULL, NULL),
    ('864219082345676', 'SP_SS_S24U_256AM', 'PN20261005_02', NULL, 'Trong_kho', DATEADD('DAY', -20, CURRENT_TIMESTAMP), NULL, NULL),
    ('864219088800221', 'SP_SS_ZFLIP5_256', 'PN20261005_02', NULL, 'Trong_kho', DATEADD('DAY', -68, CURRENT_TIMESTAMP), NULL, NULL),
    ('864219088800222', 'SP_SS_ZFLIP5_256', 'PN20261005_02', NULL, 'Trong_kho', DATEADD('DAY', -68, CURRENT_TIMESTAMP), NULL, NULL),
    ('865123073456784', 'SP_MI_14_256TRANG', 'PN20261010_03', NULL, 'Trong_kho', DATEADD('DAY', -15, CURRENT_TIMESTAMP), NULL, NULL),
    ('865123073456785', 'SP_MI_14_256TRANG', 'PN20261010_03', NULL, 'Trong_kho', DATEADD('DAY', -15, CURRENT_TIMESTAMP), NULL, NULL),
    ('869000112233401', 'SP_OPPO_FINDN3', 'PN_OLD_2026_01', NULL, 'Trong_kho', DATEADD('DAY', -78, CURRENT_TIMESTAMP), NULL, NULL),
    ('869000112233402', 'SP_OPPO_FINDN3', 'PN_OLD_2026_01', NULL, 'Trong_kho', DATEADD('DAY', -78, CURRENT_TIMESTAMP), NULL, NULL),
    ('869000112233403', 'SP_OPPO_FINDN3', 'PN_OLD_2026_01', NULL, 'Trong_kho', DATEADD('DAY', -78, CURRENT_TIMESTAMP), NULL, NULL),
    ('869000112233404', 'SP_OPPO_FINDN3', 'PN_OLD_2026_01', NULL, 'Trong_kho', DATEADD('DAY', -78, CURRENT_TIMESTAMP), NULL, NULL),
    ('869000556677801', 'SP_OPPO_RENO11_256', 'PN20261010_03', NULL, 'Trong_kho', DATEADD('DAY', -10, CURRENT_TIMESTAMP), NULL, NULL),
    ('865123073456791', 'SP_MI_14_256TRANG', 'PN20261010_03', 'HD20260918_001', 'Da_ban', '2026-09-10 09:00:00', '2026-09-18 11:05:00', '2027-09-18'),
    ('358941099900199', 'SP_IP15PM_512DEN', 'PN20261001_01', 'HD20261025_003', 'Da_ban', '2026-10-01 09:30:00', '2026-10-25 16:25:00', '2027-10-25'),
    ('358941091234592', 'SP_IP15PM_256TN', 'PN20261001_01', 'HD20261105_004', 'Da_ban', '2026-10-01 09:30:00', DATEADD('DAY', -7, CURRENT_TIMESTAMP), DATEADD('YEAR', 1, CURRENT_DATE)),
    ('865123073456792', 'SP_MI_14_256TRANG', 'PN20261010_03', 'HD20261110_005', 'Da_ban', '2026-10-10 10:00:00', DATEADD('DAY', -3, CURRENT_TIMESTAMP), DATEADD('YEAR', 1, CURRENT_DATE)),
    ('869000556677892', 'SP_OPPO_RENO11_256', 'PN20261010_03', 'HD20261110_005', 'Da_ban', '2026-10-10 10:00:00', DATEADD('DAY', -3, CURRENT_TIMESTAMP), DATEADD('YEAR', 1, CURRENT_DATE)),
    ('358941091234593', 'SP_IP15PM_256TN', 'PN_NOV_2026_04', 'HD20261112_006', 'Da_ban', DATEADD('DAY', -5, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP), DATEADD('YEAR', 1, CURRENT_DATE)),
    ('864219082345692', 'SP_SS_S24U_256AM', 'PN20261005_02', 'HD20261113_007', 'Da_ban', '2026-10-05 14:15:00', CURRENT_TIMESTAMP, DATEADD('YEAR', 1, CURRENT_DATE));

MERGE INTO khach_hang (ma_khach_hang, ho_ten, so_dien_thoai, email, dia_chi, diem_tich_luy) KEY(ma_khach_hang) VALUES
    ('KH001', 'Trần Văn An', '0981112233', 'an.tran@gmail.com', '12 Nguyễn Trãi, Q.5, TP.HCM', 120),
    ('KH002', 'Nguyễn Thị Bích', '0982223344', 'bich.nguyen@yahoo.com', '45 Hùng Vương, Q.5, TP.HCM', 250),
    ('KH003', 'Phạm Quốc Cường', '0983334455', 'cuong.pham@outlook.com', '78 Lý Thường Kiệt, Q.10, TP.HCM', 50),
    ('KH004', 'Vũ Mai Linh', '0984445566', 'linh.vu@gmail.com', '101 Cách Mạng Tháng 8, Q.3, TP.HCM', 300);

MERGE INTO khuyen_mai (ma_khuyen_mai, code_voucher, ten_khuyen_mai, loai_giam_gia, gia_tri_giam, gia_tri_don_hang_toi_thieu, giam_toi_da, qua_tang, so_luong_phat_hanh, so_luong_da_dung, ngay_bat_dau, ngay_ket_thuc, ma_thuong_hieu_ap_dung, dong_may_ap_dung, ma_danh_muc_ap_dung, doi_tuong_khach_hang, mo_ta, trang_thai) KEY(ma_khuyen_mai) VALUES
    ('KM_CHAOTHU2026', 'CHAOTHU26', 'Chào Thu 2026 - Giảm 500k cho đơn từ 20tr', 'Tien_mat', 500000, 20000000, NULL, NULL, 500, 18, DATEADD('DAY', -30, CURRENT_TIMESTAMP), DATEADD('DAY', 45, CURRENT_TIMESTAMP), NULL, NULL, NULL, 'Tat_ca', 'Giảm trực tiếp 500.000đ cho mọi đơn hàng từ 20 triệu', 'Dang_dien_ra'),
    ('KM_APPLE_VIP', 'APPLEVip5', 'Tri ân tín đồ Apple - Giảm 5% dòng iPhone', 'Phan_tram', 5, 25000000, 1500000, NULL, 200, 42, DATEADD('DAY', -20, CURRENT_TIMESTAMP), DATEADD('DAY', 30, CURRENT_TIMESTAMP), 1, 'iPhone 15 Series', NULL, 'Tat_ca', 'Giảm 5% tối đa 1.5 triệu cho các sản phẩm Apple', 'Dang_dien_ra'),
    ('KM_SS_MEMBER', 'SAMSUNGS24', 'Ưu đãi đặt trước Galaxy S24 Series', 'Tien_mat', 1000000, 25000000, NULL, NULL, 100, 12, DATEADD('DAY', -10, CURRENT_TIMESTAMP), DATEADD('DAY', 20, CURRENT_TIMESTAMP), 2, 'Galaxy S24 Series', NULL, 'Thanh_vien', 'Đang tạm dừng để điều chỉnh ngân sách', 'Tam_dung'),
    ('KM_FLASH_12H', 'FLASH12H', 'Flash Sale cuối tuần - Sắp hết hạn < 24h', 'Phan_tram', 8, 10000000, 1200000, NULL, 100, 25, DATEADD('DAY', -2, CURRENT_TIMESTAMP), DATEADD('HOUR', 14, CURRENT_TIMESTAMP), NULL, NULL, NULL, 'Tat_ca', 'Cảnh báo sắp hết hạn dưới 24 giờ', 'Dang_dien_ra'),
    ('KM_VIP_SAPHET', 'VIPSAP97', 'Ưu đãi Khách VIP - Số lượng sắp hết < 5%', 'Tien_mat', 1200000, 15000000, NULL, NULL, 100, 97, DATEADD('DAY', -15, CURRENT_TIMESTAMP), DATEADD('DAY', 15, CURRENT_TIMESTAMP), NULL, NULL, NULL, 'Khach_VIP', 'Chỉ còn 3% số lượng voucher khả dụng', 'Dang_dien_ra'),
    ('KM_COMBO_PK', 'GIFTSAC20W', 'Combo Tặng Sạc Nhanh 20W + Cường lực', 'Qua_tang', 450000, 10000000, 450000, 'Củ sạc nhanh PD 20W + Kính cường lực KingKong', 200, 18, DATEADD('DAY', -5, CURRENT_TIMESTAMP), DATEADD('DAY', 25, CURRENT_TIMESTAMP), NULL, NULL, NULL, 'Tat_ca', 'Tặng combo phụ kiện trị giá 450.000đ cho đơn từ 10 triệu', 'Dang_dien_ra'),
    ('KM_EXPIRED_25', 'HET_HAN_2025', 'Khuyến mãi Tết cũ đã hết hạn', 'Tien_mat', 300000, 5000000, NULL, NULL, 50, 50, '2025-12-01 00:00:00', '2026-01-31 23:59:59', NULL, NULL, NULL, 'Tat_ca', 'Voucher test trường hợp hết hạn ở POS', 'Het_han');

MERGE INTO don_hang (ma_don_hang, ma_khach_hang, ma_nhan_vien, ma_khuyen_mai, ngay_tao, tong_tien_hang, tien_giam_gia, thue_vat, tong_tien_can_tra, trang_thai, ghi_chu) KEY(ma_don_hang) VALUES
    ('DH20260918_001', 'KH001', 'NV003', 'KM_CHAOTHU2026', '2026-09-18 11:00:00', 22990000, 500000, 0, 22490000, 'Da_thanh_toan', 'Bán Xiaomi 14 tháng 9'),
    ('DH20261012_001', 'KH001', 'NV003', 'KM_APPLE_VIP', '2026-10-12 10:15:00', 29990000, 1499500, 0, 28490500, 'Da_thanh_toan', 'Khách mua trực tiếp'),
    ('DH20261014_002', 'KH002', 'NV003', 'KM_SS_MEMBER', '2026-10-14 15:45:00', 27990000, 1000000, 0, 26990000, 'Da_thanh_toan', 'Khách VIP'),
    ('DH20261025_003', 'KH004', 'NV006', 'KM_APPLE_VIP', '2026-10-25 16:20:00', 34990000, 1500000, 0, 33490000, 'Da_thanh_toan', 'Bán iPhone 15 PM 512GB'),
    ('DH20261105_004', 'KH002', 'NV003', 'KM_CHAOTHU2026', DATEADD('DAY', -7, CURRENT_TIMESTAMP), 29990000, 500000, 0, 29490000, 'Da_thanh_toan', 'Đơn bán 7 ngày trước'),
    ('DH20261110_005', 'KH003', 'NV006', NULL, DATEADD('DAY', -3, CURRENT_TIMESTAMP), 33980000, 0, 0, 33980000, 'Da_thanh_toan', 'Bán Xiaomi 14 + OPPO Reno11'),
    ('DH20261112_006', 'KH004', 'NV003', 'KM_APPLE_VIP', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 29990000, 1499500, 0, 28490500, 'Da_thanh_toan', 'Khách VIP Apple'),
    ('DH20261113_007', 'KH001', 'NV003', 'KM_COMBO_PK', CURRENT_TIMESTAMP, 27990000, 0, 0, 27990000, 'Da_thanh_toan', 'Giao dịch hôm nay'),
    ('DH_GIUCHO_001', 'KH003', 'NV003', NULL, CURRENT_TIMESTAMP, 57980000, 0, 0, 57980000, 'Cho_thanh_toan', 'Khách đặt cọc giữ máy chờ nhận');

MERGE INTO chi_tiet_don_hang (ma_chi_tiet_don, ma_don_hang, ma_san_pham, so_luong, don_gia, thanh_tien) KEY(ma_chi_tiet_don) VALUES
    (1, 'DH20260918_001', 'SP_MI_14_256TRANG', 1, 22990000, 22990000),
    (2, 'DH20261012_001', 'SP_IP15PM_256TN', 1, 29990000, 29990000),
    (3, 'DH20261014_002', 'SP_SS_S24U_256AM', 1, 27990000, 27990000),
    (4, 'DH20261025_003', 'SP_IP15PM_512DEN', 1, 34990000, 34990000),
    (5, 'DH20261105_004', 'SP_IP15PM_256TN', 1, 29990000, 29990000),
    (6, 'DH20261110_005', 'SP_MI_14_256TRANG', 1, 22990000, 22990000),
    (7, 'DH20261110_005', 'SP_OPPO_RENO11_256', 1, 10990000, 10990000),
    (8, 'DH20261112_006', 'SP_IP15PM_256TN', 1, 29990000, 29990000),
    (9, 'DH20261113_007', 'SP_SS_S24U_256AM', 1, 27990000, 27990000),
    (10, 'DH_GIUCHO_001', 'SP_IP15PM_256TN', 1, 29990000, 29990000),
    (11, 'DH_GIUCHO_001', 'SP_SS_S24U_256AM', 1, 27990000, 27990000);

MERGE INTO hoa_don (ma_hoa_don, ma_don_hang, ma_khach_hang, ma_nhan_vien, ngay_lap, tong_tien_hang, thue_vat_phan_tram, tien_thue_vat, tien_giam_gia, tong_thanh_toan, phuong_thuc_thanh_toan, tien_khach_dua, tien_thua, trang_thai_thanh_toan, ghi_chu) KEY(ma_hoa_don) VALUES
    ('HD20260918_001', 'DH20260918_001', 'KH001', 'NV003', '2026-09-18 11:05:00', 22990000, 0, 0, 500000, 22490000, 'Chuyen_khoan_QR', 22490000, 0, 'Thanh_cong', 'Thanh toán QR tháng 9'),
    ('HD20261012_001', 'DH20261012_001', 'KH001', 'NV003', '2026-10-12 10:20:00', 29990000, 0, 0, 1499500, 28490500, 'Tien_mat', 29000000, 509500, 'Thanh_cong', 'Đơn bán đầu tiên'),
    ('HD20261014_002', 'DH20261014_002', 'KH002', 'NV003', '2026-10-14 15:50:00', 27990000, 0, 0, 1000000, 26990000, 'Chuyen_khoan_QR', 26990000, 0, 'Thanh_cong', 'Thanh toán QR'),
    ('HD20261025_003', 'DH20261025_003', 'KH004', 'NV006', '2026-10-25 16:25:00', 34990000, 0, 0, 1500000, 33490000, 'The', 33490000, 0, 'Thanh_cong', 'Thanh toán thẻ cuối tháng 10'),
    ('HD20261105_004', 'DH20261105_004', 'KH002', 'NV003', DATEADD('DAY', -7, CURRENT_TIMESTAMP), 29990000, 0, 0, 500000, 29490000, 'Tien_mat', 30000000, 510000, 'Thanh_cong', 'Khách trả tiền mặt'),
    ('HD20261110_005', 'DH20261110_005', 'KH003', 'NV006', DATEADD('DAY', -3, CURRENT_TIMESTAMP), 33980000, 0, 0, 0, 33980000, 'Chuyen_khoan_QR', 33980000, 0, 'Thanh_cong', 'Đơn combo 2 máy'),
    ('HD20261112_006', 'DH20261112_006', 'KH004', 'NV003', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 29990000, 0, 0, 1499500, 28490500, 'Chuyen_khoan_QR', 28490500, 0, 'Thanh_cong', 'Khách VIP Apple'),
    ('HD20261113_007', 'DH20261113_007', 'KH001', 'NV003', CURRENT_TIMESTAMP, 27990000, 0, 0, 0, 27990000, 'Tien_mat', 28000000, 10000, 'Thanh_cong', 'Đơn hôm nay tặng kèm combo sạc 20W');

MERGE INTO chi_tiet_hoa_don (ma_chi_tiet_hd, ma_hoa_don, ma_san_pham, ma_imei, don_gia, thoi_gian_bao_hanh_thang) KEY(ma_chi_tiet_hd) VALUES
    (1, 'HD20260918_001', 'SP_MI_14_256TRANG', '865123073456791', 22990000, 12),
    (2, 'HD20261012_001', 'SP_IP15PM_256TN', '358941091234561', 29990000, 12),
    (3, 'HD20261014_002', 'SP_SS_S24U_256AM', '864219082345671', 27990000, 12),
    (4, 'HD20261025_003', 'SP_IP15PM_512DEN', '358941099900199', 34990000, 12),
    (5, 'HD20261105_004', 'SP_IP15PM_256TN', '358941091234592', 29990000, 12),
    (6, 'HD20261110_005', 'SP_MI_14_256TRANG', '865123073456792', 22990000, 12),
    (7, 'HD20261110_005', 'SP_OPPO_RENO11_256', '869000556677892', 10990000, 12),
    (8, 'HD20261112_006', 'SP_IP15PM_256TN', '358941091234593', 29990000, 12),
    (9, 'HD20261113_007', 'SP_SS_S24U_256AM', '864219082345692', 27990000, 12);

MERGE INTO phieu_kiem_ke (ma_phieu_kiem_ke, ma_nhan_vien, ngay_kiem_ke, trang_thai, ghi_chu) KEY(ma_phieu_kiem_ke) VALUES
    ('PKK20261015_01', 'NV004', DATEADD('DAY', -10, CURRENT_TIMESTAMP), 'Da_can_bang', 'Kiểm kê định kỳ giữa tháng');

MERGE INTO chi_tiet_kiem_ke (ma_chi_tiet_kiem_ke, ma_phieu_kiem_ke, ma_san_pham, so_luong_he_thong, so_luong_thuc_te, chenh_lech, ly_do_chenh_lech) KEY(ma_chi_tiet_kiem_ke) VALUES
    (1, 'PKK20261015_01', 'SP_IP15PM_256TN', 12, 12, 0, 'Khớp tồn kho'),
    (2, 'PKK20261015_01', 'SP_OPPO_FINDN3', 4, 4, 0, 'Hàng trưng bày tồn lâu >60 ngày');

MERGE INTO nhat_ky_bao_cao (ma_bao_cao, loai_bao_cao, ma_nhan_vien_lap, ngay_lap, tu_ngay, den_ngay, dinh_dang_xuat, duong_dan_file, tieu_chi_loc, ghi_chu) KEY(ma_bao_cao) VALUES
    (1, 'Doanh_thu', 'NV001', DATEADD('DAY', -2, CURRENT_TIMESTAMP), DATEADD('DAY', -30, CURRENT_DATE), CURRENT_DATE, 'EXCEL', 'bao_cao_doanh_thu_thang.xlsx', 'Thương hiệu: Tất cả | Kỳ: 30 ngày', 'Tổng hợp doanh thu định kỳ'),
    (2, 'Kiem_ke_kho', 'NV004', DATEADD('DAY', -1, CURRENT_TIMESTAMP), DATEADD('DAY', -14, CURRENT_DATE), CURRENT_DATE, 'PDF', 'bao_cao_ton_kho_tuan.pdf', 'Cảnh báo định mức tồn kho', 'Báo cáo tồn kho & cảnh báo nhập hàng');

-- Bảng invoices tương thích entity Invoice (dùng cho DashboardController)
CREATE TABLE IF NOT EXISTS invoices (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        invoice_code VARCHAR(100) NOT NULL UNIQUE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DOUBLE,
    status VARCHAR(50) DEFAULT 'COMPLETED'
    );

MERGE INTO invoices (id, invoice_code, created_date, total_amount, status) KEY(id) VALUES
    (1, 'HD20260918_001', CURRENT_TIMESTAMP, 22990000, 'COMPLETED'),
    (2, 'HD20261012_001', CURRENT_TIMESTAMP, 29990000, 'COMPLETED'),
    (3, 'HD20261014_002', CURRENT_TIMESTAMP, 27990000, 'COMPLETED'),
    (4, 'HD20261025_003', CURRENT_TIMESTAMP, 34990000, 'COMPLETED'),
    (5, 'HD20261105_004', CURRENT_TIMESTAMP, 29490000, 'COMPLETED'),
    (6, 'HD20261110_005', CURRENT_TIMESTAMP, 33980000, 'COMPLETED'),
    (7, 'HD20261112_006', CURRENT_TIMESTAMP, 28490500, 'COMPLETED'),
    (8, 'HD20261113_007', CURRENT_TIMESTAMP, 27990000, 'COMPLETED');

-- Bảng roles & users tương thích Spring Security và User entity
CREATE TABLE IF NOT EXISTS roles (
                                     id INT AUTO_INCREMENT PRIMARY KEY,
                                     name VARCHAR(50) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    status BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT NOT NULL,
                                          role_id INT NOT NULL,
                                          PRIMARY KEY (user_id, role_id)
    );

MERGE INTO roles (id, name) KEY(id) VALUES (1, 'ROLE_ADMIN'), (2, 'ROLE_STAFF');
MERGE INTO users (id, username, password, status) KEY(id) VALUES (1, 'admin', '123456', true);
MERGE INTO user_roles (user_id, role_id) KEY(user_id, role_id) VALUES (1, 1);