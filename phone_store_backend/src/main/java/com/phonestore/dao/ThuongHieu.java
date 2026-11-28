package com.phonestore.model;

public class ThuongHieu {
    private int maThuongHieu;
    private String tenThuongHieu;
    private String quocGia;
    private String logoUrl;
    private String moTa;

    public void themThuongHieu() {
        if (this.tenThuongHieu == null || this.tenThuongHieu.isBlank()) {
            throw new IllegalArgumentException("Tên thương hiệu không được để trống");
        }
        this.tenThuongHieu = this.tenThuongHieu.trim();
    }

    public void capNhatThuongHieu(ThuongHieu moi) {
        if (moi == null) return;
        if (moi.tenThuongHieu != null && !moi.tenThuongHieu.isBlank()) {
            this.tenThuongHieu = moi.tenThuongHieu.trim();
        }
        this.quocGia = moi.quocGia;
        this.logoUrl = moi.logoUrl;
        this.moTa = moi.moTa;
    }

    public void xoaThuongHieu() {
        this.moTa = null;
    }

    public int getMaThuongHieu() { return maThuongHieu; }
    public void setMaThuongHieu(int maThuongHieu) { this.maThuongHieu = maThuongHieu; }
    public String getTenThuongHieu() { return tenThuongHieu; }
    public void setTenThuongHieu(String tenThuongHieu) { this.tenThuongHieu = tenThuongHieu; }
    public String getQuocGia() { return quocGia; }
    public void setQuocGia(String quocGia) { this.quocGia = quocGia; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
}