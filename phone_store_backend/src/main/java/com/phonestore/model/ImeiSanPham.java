package com.phonestore.model;

import java.time.LocalDateTime;

public class ImeiSanPham {
    private String maImei;
    private String trangThai = "Ton_kho";
    private LocalDateTime ngayNhap = LocalDateTime.now();
    private LocalDateTime ngayXuat;

    public void taoMaImei(String maImeiMoi) {
        if (maImeiMoi == null || maImeiMoi.isBlank()) {
            throw new IllegalArgumentException("Mã IMEI không được để trống");
        }
        this.maImei = maImeiMoi.trim();
        this.trangThai = "Ton_kho";
        if (this.ngayNhap == null) {
            this.ngayNhap = LocalDateTime.now();
        }
    }

    public void capNhatTrangThai(String trangThaiMoi) {
        if (trangThaiMoi != null && !trangThaiMoi.isBlank()) {
            this.trangThai = trangThaiMoi.trim();
            if ("Da_ban".equalsIgnoreCase(this.trangThai) && this.ngayXuat == null) {
                this.ngayXuat = LocalDateTime.now();
            }
        }
    }

    public boolean kiemTraTrangThai(String trangThaiKiemTra) {
        if (trangThaiKiemTra == null) return false;
        return trangThaiKiemTra.equalsIgnoreCase(this.trangThai);
    }

    public String getMaImei() { return maImei; }
    public void setMaImei(String maImei) { this.maImei = maImei; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public LocalDateTime getNgayNhap() { return ngayNhap; }
    public void setNgayNhap(LocalDateTime ngayNhap) { this.ngayNhap = ngayNhap; }
    public LocalDateTime getNgayXuat() { return ngayXuat; }
    public void setNgayXuat(LocalDateTime ngayXuat) { this.ngayXuat = ngayXuat; }
}