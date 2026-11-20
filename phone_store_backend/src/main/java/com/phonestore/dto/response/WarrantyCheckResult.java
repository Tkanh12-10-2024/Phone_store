package com.phonestore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class WarrantyCheckResult {
    private boolean found;
    private String maImei;
    private String maSanPham;
    private String tenSanPham;
    private String maKhachHang;
    private String tenKhachHang;
    private String soDienThoai;
    private LocalDateTime ngayBan;
    private LocalDate hanBaoHanh;
    private boolean conBaoHanh;
    private long soNgayConLai;
    private String lyDo;
    private String trangThaiImei;
}
