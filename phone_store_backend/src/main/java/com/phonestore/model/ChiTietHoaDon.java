package com.phonestore.model;

import java.math.BigDecimal;

public class ChiTietHoaDon {
    private int maChiTietHd;
    private String maSanPham;
    private String tenSanPham;
    private String maImei;
    private int soLuong = 1;
    private BigDecimal donGia = BigDecimal.ZERO;
    private int thoiGianBaoHanhThang = 12;

    public void ganImei(String maImeiMoi) {
        if (maImeiMoi != null && !maImeiMoi.isBlank()) {
            this.maImei = maImeiMoi.trim();
        }
    }

    public void capNhatBaoHanh(int soThangMoi) {
        if (soThangMoi >= 0) {
            this.thoiGianBaoHanhThang = soThangMoi;
        }
    }

    public BigDecimal tinhThanhTien() {
        BigDecimal gia = this.donGia != null ? this.donGia : BigDecimal.ZERO;
        return gia.multiply(BigDecimal.valueOf(Math.max(0, this.soLuong)));
    }

    public int getMaChiTietHd() { return maChiTietHd; }
    public void setMaChiTietHd(int maChiTietHd) { this.maChiTietHd = maChiTietHd; }
    public String getMaSanPham() { return maSanPham; }
    public void setMaSanPham(String maSanPham) { this.maSanPham = maSanPham; }
    public String getTenSanPham() { return tenSanPham; }
    public void setTenSanPham(String tenSanPham) { this.tenSanPham = tenSanPham; }
    public String getMaImei() { return maImei; }
    public void setMaImei(String maImei) { this.maImei = maImei; }
    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }
    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
    public int getThoiGianBaoHanhThang() { return thoiGianBaoHanhThang; }
    public void setThoiGianBaoHanhThang(int thoiGianBaoHanhThang) { this.thoiGianBaoHanhThang = thoiGianBaoHanhThang; }
}