package com.phonestore.model;

import java.math.BigDecimal;

public class ChiTietDonHang {
    private int maChiTietDon;
    private int soLuong = 1;
    private BigDecimal donGia = BigDecimal.ZERO;
    private BigDecimal thanhTien = BigDecimal.ZERO;

    public BigDecimal tinhThanhTien() {
        BigDecimal gia = this.donGia != null ? this.donGia : BigDecimal.ZERO;
        this.thanhTien = gia.multiply(BigDecimal.valueOf(Math.max(0, this.soLuong)));
        return this.thanhTien;
    }

    public void capNhatSoLuong(int soLuongMoi) {
        if (soLuongMoi > 0) {
            this.soLuong = soLuongMoi;
            tinhThanhTien();
        }
    }

    public int getMaChiTietDon() { return maChiTietDon; }
    public void setMaChiTietDon(int maChiTietDon) { this.maChiTietDon = maChiTietDon; }
    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }
    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
    public BigDecimal getThanhTien() { return thanhTien; }
    public void setThanhTien(BigDecimal thanhTien) { this.thanhTien = thanhTien; }
}