package com.phonestore.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TaiKhoan {
    private int maTaiKhoan;
    private String tenDangNhap;
    private String matKhau;
    private String trangThai = "Hoat_dong";
    private LocalDateTime ngayTao = LocalDateTime.now();
    private LocalDateTime lanDangNhapCuoi;
    private LocalDate hanBaoHanh;

    public boolean dangNhap(String inputPassword) {
        if (!kiemTraTrangThai() || inputPassword == null) return false;
        boolean hopLe = inputPassword.equals(this.matKhau);
        if (hopLe) {
            this.lanDangNhapCuoi = LocalDateTime.now();
        }
        return hopLe;
    }

    public void dangXuat() {
        // Cập nhật trạng thái phiên đăng xuất nếu cần
    }

    public boolean doiMatKhau(String matKhauCu, String matKhauMoi) {
        if (matKhauCu == null || !matKhauCu.equals(this.matKhau)) return false;
        if (matKhauMoi == null || matKhauMoi.isBlank()) return false;
        this.matKhau = matKhauMoi;
        return true;
    }

    public void khoaTaiKhoan() {
        this.trangThai = "Bi_khoa";
    }

    public boolean kiemTraTrangThai() {
        return "Hoat_dong".equalsIgnoreCase(this.trangThai);
    }

    public int getMaTaiKhoan() { return maTaiKhoan; }
    public void setMaTaiKhoan(int maTaiKhoan) { this.maTaiKhoan = maTaiKhoan; }
    public String getTenDangNhap() { return tenDangNhap; }
    public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }
    public String getMatKhau() { return matKhau; }
    public void setMatKhau(String matKhau) { this.matKhau = matKhau; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
    public LocalDateTime getLanDangNhapCuoi() { return lanDangNhapCuoi; }
    public void setLanDangNhapCuoi(LocalDateTime lanDangNhapCuoi) { this.lanDangNhapCuoi = lanDangNhapCuoi; }
    public LocalDate getHanBaoHanh() { return hanBaoHanh; }
    public void setHanBaoHanh(LocalDate hanBaoHanh) { this.hanBaoHanh = hanBaoHanh; }
}