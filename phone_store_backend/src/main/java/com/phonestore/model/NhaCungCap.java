package com.phonestore.model;

import java.math.BigDecimal;

public class NhaCungCap {
    private String maNhaCungCap;
    private String tenNhaCungCap;
    private String soDienThoai;
    private String email;
    private String diaChi;
    private String maSoThue;
    private BigDecimal congNo = BigDecimal.ZERO;
    private String trangThai = "Hoat_dong";

    public void themNhaCungCap() {
        if (this.tenNhaCungCap == null || this.tenNhaCungCap.isBlank()) {
            throw new IllegalArgumentException("Tên nhà cung cấp không được để trống");
        }
        if (this.trangThai == null || this.trangThai.isBlank()) {
            this.trangThai = "Hoat_dong";
        }
    }

    public void capNhatNhaCungCap(NhaCungCap moi) {
        if (moi == null) return;
        this.tenNhaCungCap = moi.tenNhaCungCap;
        this.soDienThoai = moi.soDienThoai;
        this.email = moi.email;
        this.diaChi = moi.diaChi;
        this.maSoThue = moi.maSoThue;
        if (moi.trangThai != null) {
            this.trangThai = moi.trangThai;
        }
    }

    public void xoaNhaCungCap() {
        this.trangThai = "Ngung_hop_tac";
    }

    public BigDecimal traCuuCongNo() {
        return this.congNo != null ? this.congNo : BigDecimal.ZERO;
    }

    public void capNhatCongNo(BigDecimal chenhLech) {
        BigDecimal hienTai = traCuuCongNo();
        BigDecimal thayDoi = chenhLech != null ? chenhLech : BigDecimal.ZERO;
        this.congNo = hienTai.add(thayDoi).max(BigDecimal.ZERO);
    }

    public String getMaNhaCungCap() { return maNhaCungCap; }
    public void setMaNhaCungCap(String maNhaCungCap) { this.maNhaCungCap = maNhaCungCap; }
    public String getTenNhaCungCap() { return tenNhaCungCap; }
    public void setTenNhaCungCap(String tenNhaCungCap) { this.tenNhaCungCap = tenNhaCungCap; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public String getMaSoThue() { return maSoThue; }
    public void setMaSoThue(String maSoThue) { this.maSoThue = maSoThue; }
    public BigDecimal getCongNo() { return congNo; }
    public void setCongNo(BigDecimal congNo) { this.congNo = congNo; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
}