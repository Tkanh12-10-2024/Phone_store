package com.phonestore.dto.warranty;

public record WarrantyReceiveRequest(
        String maKhachHang,
        String maImei,
        String tenThietBi,
        String tinhTrangTiepNhan,
        String loiPhanAnh,
        String yeuCauSuaChua,
        String maNhanVien
) {
}
