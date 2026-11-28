package com.phonestore.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class NhanVien {
    private String maNhanVien;
    private String hoTen;
    private String gioiTinh;
    private LocalDate ngaySinh;
    private String soDienThoai;
    private String email;
    private String diaChi;
    private String chucVu;
    private LocalDate ngayVaoLam;
    private BigDecimal luongCoBan = BigDecimal.ZERO;
    private String trangThai = "Dang_lam_viec";

    public void themNhanVien() {
        if (this.hoTen == null || this.hoTen.isBlank()) {
            throw new IllegalArgumentException("Họ tên nhân viên không được để trống");
        }
        if (this.ngayVaoLam == null) {
            this.ngayVaoLam = LocalDate.now();
        }
        if (this.trangThai == null || this.trangThai.isBlank()) {
            this.trangThai = "Dang_lam_viec";
        }
    }

    public void capNhatThongTin(NhanVien moi) {
        if (moi == null) return;
        this.hoTen = moi.hoTen;
        this.gioiTinh = moi.gioiTinh;
        this.ngaySinh = moi.ngaySinh;
        this.soDienThoai = moi.soDienThoai;
        this.email = moi.email;
        this.diaChi = moi.diaChi;
        this.chucVu = moi.chucVu;
        if (moi.luongCoBan != null) {
            this.luongCoBan = moi.luongCoBan;
        }
    }

    public void xoaNhanVien() {
        this.trangThai = "Nghi_viec";
    }

    public NhanVien timKiemNhanVien(List<NhanVien> danhSach, String maHoacTen) {
        if (danhSach == null || maHoacTen == null || maHoacTen.isBlank()) return null;
        String key = maHoacTen.trim().toLowerCase();
        return danhSach.stream()
                .filter(nv -> (nv.maNhanVien != null && nv.maNhanVien.toLowerCase().equals(key))
                        || (nv.hoTen != null && nv.hoTen.toLowerCase().contains(key)))
                .findFirst()
                .orElse(null);
    }

    public void thayDoiTrangThai(String trangThaiMoi) {
        if (trangThaiMoi != null && !trangThaiMoi.isBlank()) {
            this.trangThai = trangThaiMoi.trim();
        }
    }

    public String getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(String maNhanVien) { this.maNhanVien = maNhanVien; }
    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    public String getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(String gioiTinh) { this.gioiTinh = gioiTinh; }
    public LocalDate getNgaySinh() { return ngaySinh; }
    public void setNgaySinh(LocalDate ngaySinh) { this.ngaySinh = ngaySinh; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public String getChucVu() { return chucVu; }
    public void setChucVu(String chucVu) { this.chucVu = chucVu; }
    public LocalDate getNgayVaoLam() { return ngayVaoLam; }
    public void setNgayVaoLam(LocalDate ngayVaoLam) { this.ngayVaoLam = ngayVaoLam; }
    public BigDecimal getLuongCoBan() { return luongCoBan; }
    public void setLuongCoBan(BigDecimal luongCoBan) { this.luongCoBan = luongCoBan; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
}