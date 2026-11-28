package com.phonestore.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class KhachHang {
    private String maKhachHang;
    private String hoTen;
    private String soDienThoai;
    private String email;
    private String diaChi;
    private int diemTichLuy;
    private LocalDateTime ngayTao = LocalDateTime.now();
    private String hangKhachHang = "Khach_moi";

    public void themKhachHang() {
        if (this.hoTen == null || this.hoTen.isBlank()) {
            throw new IllegalArgumentException("Họ tên khách hàng không được để trống");
        }
        if (this.ngayTao == null) {
            this.ngayTao = LocalDateTime.now();
        }
        if (this.hangKhachHang == null || this.hangKhachHang.isBlank()) {
            this.hangKhachHang = "Khach_moi";
        }
    }

    public void capNhatKhachHang(KhachHang moi) {
        if (moi == null) return;
        this.hoTen = moi.hoTen;
        this.soDienThoai = moi.soDienThoai;
        this.email = moi.email;
        this.diaChi = moi.diaChi;
    }

    public KhachHang timKiemKhachHang(List<KhachHang> danhSach, String tuKhoa) {
        if (danhSach == null || tuKhoa == null || tuKhoa.isBlank()) return null;
        String key = tuKhoa.trim().toLowerCase();
        return danhSach.stream()
                .filter(kh -> (kh.maKhachHang != null && kh.maKhachHang.toLowerCase().equals(key))
                        || (kh.soDienThoai != null && kh.soDienThoai.toLowerCase().contains(key))
                        || (kh.hoTen != null && kh.hoTen.toLowerCase().contains(key)))
                .findFirst()
                .orElse(null);
    }

    public void tichDiem(BigDecimal giaTriDonHang) {
        if (giaTriDonHang == null || giaTriDonHang.compareTo(BigDecimal.ZERO) <= 0) return;
        int diemCong = giaTriDonHang.divideToIntegralValue(BigDecimal.valueOf(100000)).intValue();
        this.diemTichLuy += Math.max(0, diemCong);
        capNhatHangKhachHang();
    }

    public void suDungDiem(int soDiem) {
        if (soDiem < 0 || soDiem > this.diemTichLuy) {
            throw new IllegalArgumentException("Số điểm sử dụng không hợp lệ");
        }
        this.diemTichLuy -= soDiem;
        capNhatHangKhachHang();
    }

    private void capNhatHangKhachHang() {
        if (this.diemTichLuy >= 1000) {
            this.hangKhachHang = "Khach_VIP";
        } else if (this.diemTichLuy >= 200) {
            this.hangKhachHang = "Thanh_vien";
        } else {
            this.hangKhachHang = "Khach_moi";
        }
    }

    public String getMaKhachHang() { return maKhachHang; }
    public void setMaKhachHang(String maKhachHang) { this.maKhachHang = maKhachHang; }
    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public int getDiemTichLuy() { return diemTichLuy; }
    public void setDiemTichLuy(int diemTichLuy) { this.diemTichLuy = diemTichLuy; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
    public String getHangKhachHang() { return hangKhachHang; }
    public void setHangKhachHang(String hangKhachHang) { this.hangKhachHang = hangKhachHang; }
}