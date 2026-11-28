-- Additive schema updates for warranty, promotions, and reports.
-- Safe to apply to an existing quanly_cuahang_dienthoai database.
-- This file does not drop tables or modify existing business rows.

USE quanly_cuahang_dienthoai;

ALTER TABLE khuyen_mai
    MODIFY COLUMN loai_giam_gia
        ENUM('Phan_tram', 'Tien_mat', 'Qua_tang') NOT NULL DEFAULT 'Phan_tram',
    ADD COLUMN IF NOT EXISTS qua_tang VARCHAR(255) NULL AFTER giam_toi_da,
    ADD COLUMN IF NOT EXISTS dong_may_ap_dung VARCHAR(100) NULL AFTER ma_thuong_hieu_ap_dung,
    ADD COLUMN IF NOT EXISTS ma_danh_muc_ap_dung INT NULL AFTER dong_may_ap_dung,
    ADD COLUMN IF NOT EXISTS doi_tuong_khach_hang
        ENUM('Tat_ca', 'Khach_moi', 'Thanh_vien', 'Khach_VIP') NOT NULL DEFAULT 'Tat_ca'
        AFTER ma_danh_muc_ap_dung,
    ADD COLUMN IF NOT EXISTS mo_ta VARCHAR(500) NULL AFTER doi_tuong_khach_hang,
    ADD COLUMN IF NOT EXISTS ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER trang_thai;

ALTER TABLE phieu_bao_hanh
    MODIFY COLUMN ma_khach_hang VARCHAR(20) NULL,
    MODIFY COLUMN trang_thai
        ENUM('Tiep_nhan', 'Dang_kiem_tra', 'Dang_sua_chua', 'Cho_tra_khach',
             'Hoan_tat', 'Tu_choi', 'Tu_choi_bao_hanh')
        NOT NULL DEFAULT 'Tiep_nhan';

ALTER TABLE lich_su_bao_hanh
    MODIFY COLUMN trang_thai_moi
        ENUM('Tiep_nhan', 'Dang_kiem_tra', 'Dang_sua_chua', 'Cho_tra_khach',
             'Hoan_tat', 'Tu_choi', 'Tu_choi_bao_hanh')
        NOT NULL;

ALTER TABLE nhat_ky_bao_cao
    MODIFY COLUMN loai_bao_cao
        ENUM('Doanh_thu', 'Kiem_ke_kho', 'SanPham_BanChay', 'Ton_kho',
             'Tong_hop', 'Bao_hanh')
        NOT NULL,
    ADD COLUMN IF NOT EXISTS tieu_chi_loc VARCHAR(500) NULL AFTER duong_dan_file;
