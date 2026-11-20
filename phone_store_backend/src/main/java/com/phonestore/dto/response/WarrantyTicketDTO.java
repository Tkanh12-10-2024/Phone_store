package com.phonestore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WarrantyTicketDTO {
    private String maPhieuBh;
    private String maKhachHang;
    private String tenKhachHang;
    private String soDienThoai;
    private String maNhanVienTiepNhan;
    private String tenNhanVienTiepNhan;
    private String maImei;
    private String maSanPham;
    private String tenThietBi;
    private String tinhTrangTiepNhan;
    private String loiPhanAnh;
    private String yeuCauSuaChua;
    private String ketQuaKiemTra;
    private String nguyenNhanLoi;
    private String noiDungSuaChua;
    private String ghiChu;
    private LocalDateTime ngayTiepNhan;
    private LocalDateTime ngayCapNhat;
    private LocalDateTime ngayBanGiao;
    private String trangThai;
    private String maNhanVienCapNhat;
    private String tenNhanVienCapNhat;
}
