package com.phonestore.dto.request;

import lombok.Data;

@Data
public class WarrantyCreateRequest {
    private String maKhachHang;
    private String maImei;
    private String tenThietBi;
    private String tinhTrangTiepNhan;
    private String loiPhanAnh;
    private String yeuCauSuaChua;
    private String maNhanVien;
}
