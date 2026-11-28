package com.phonestore.dto.warranty;

public record WarrantyProgressRequest(
        String ketQuaKiemTra,
        String nguyenNhanLoi,
        String noiDungSuaChua,
        String ghiChu,
        String trangThaiMoi,
        String maNhanVien
) {
}
