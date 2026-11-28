package com.phonestore.model;

import java.util.HashSet;
import java.util.Set;

public class VaiTro {
    private int maVaiTro;
    private String tenVaiTro;
    private String moTa;
    private Set<String> danhSachQuyen = new HashSet<>();

    public void themVaiTro() {
        if (this.tenVaiTro == null || this.tenVaiTro.isBlank()) {
            throw new IllegalArgumentException("Tên vai trò không được để trống");
        }
        this.tenVaiTro = this.tenVaiTro.trim();
    }

    public void capNhatVaiTro(VaiTro moi) {
        if (moi == null) return;
        if (moi.tenVaiTro != null && !moi.tenVaiTro.isBlank()) {
            this.tenVaiTro = moi.tenVaiTro.trim();
        }
        this.moTa = moi.moTa;
    }

    public void xoaVaiTro() {
        this.danhSachQuyen.clear();
    }

    public boolean kiemTraQuyen(String maQuyen) {
        if (maQuyen == null || maQuyen.isBlank()) return false;
        return danhSachQuyen.contains(maQuyen.trim());
    }

    public void capQuyen(String maQuyen) {
        if (maQuyen != null && !maQuyen.isBlank()) {
            danhSachQuyen.add(maQuyen.trim());
        }
    }

    public int getMaVaiTro() { return maVaiTro; }
    public void setMaVaiTro(int maVaiTro) { this.maVaiTro = maVaiTro; }
    public String getTenVaiTro() { return tenVaiTro; }
    public void setTenVaiTro(String tenVaiTro) { this.tenVaiTro = tenVaiTro; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public Set<String> getDanhSachQuyen() { return danhSachQuyen; }
    public void setDanhSachQuyen(Set<String> danhSachQuyen) { this.danhSachQuyen = danhSachQuyen; }
}