package com.phonestore.model;

import java.math.BigDecimal;

public class ChiTietPhieuNhap {
    private int maChiTietNhap;
    private int soLuong;
    private BigDecimal donGiaNhap = BigDecimal.ZERO;
    private BigDecimal thanhTien = BigDecimal.ZERO;

    public BigDecimal tinhThanhTien() {
        BigDecimal gia = this.donGiaNhap != null ? this.donGiaNhap : BigDecimal.ZERO;
        this.thanhTien = gia.multiply(BigDecimal.valueOf(Math.max(0, this.soLuong)));
        return this.thanhTien;
    }

    public void capNhatSoLuong(int soLuongMoi) {
        if (soLuongMoi >= 0) {
            this.soLuong = soLuongMoi;
            tinhThanhTien();
        }
    }

    public int getMaChiTietNhap() { return maChiTietNhap; }
    public void setMaChiTietNhap(int maChiTietNhap) { this.maChiTietNhap = maChiTietNhap; }
    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }
    public BigDecimal getDonGiaNhap() { return donGiaNhap; }
    public void setDonGiaNhap(BigDecimal donGiaNhap) { this.donGiaNhap = donGiaNhap; }
    public BigDecimal getThanhTien() { return thanhTien; }
    public void setThanhTien(BigDecimal thanhTien) { this.thanhTien = thanhTien; }
}