package com.phonestore.model;

import java.math.BigDecimal;

public class ChiTietGioHang {
    private int maChiTietGioHang;
    private int soLuong = 1;
    private BigDecimal donGia = BigDecimal.ZERO;

    public BigDecimal tinhThanhTien() {
        BigDecimal gia = this.donGia != null ? this.donGia : BigDecimal.ZERO;
        return gia.multiply(BigDecimal.valueOf(Math.max(0, this.soLuong)));
    }

    public void capNhatSoLuong(int soLuongMoi) {
        if (soLuongMoi > 0) {
            this.soLuong = soLuongMoi;
        }
    }

    public int getMaChiTietGioHang() { return maChiTietGioHang; }
    public void setMaChiTietGioHang(int maChiTietGioHang) { this.maChiTietGioHang = maChiTietGioHang; }
    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }
    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
}