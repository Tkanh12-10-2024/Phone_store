
package com.phonestore.dto.pos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO trả thông tin hóa đơn trong danh sách POS.
 */
public class PosInvoiceResponse {

    private String maHoaDon;

    private String maDonHang;

    private String maKhachHang;

    private String tenKhachHang;

    private String maNhanVien;

    private String tenNhanVien;

    private LocalDateTime ngayLap;

    private BigDecimal tamTinh;

    private BigDecimal soTienGiam;

    private BigDecimal tienVat;

    private BigDecimal tongThanhToan;

    private String phuongThucThanhToan;

    private String trangThai;

    public PosInvoiceResponse() {
    }

    public PosInvoiceResponse(
            String maHoaDon,
            String maDonHang,
            String maKhachHang,
            String tenKhachHang,
            String maNhanVien,
            String tenNhanVien,
            LocalDateTime ngayLap,
            BigDecimal tamTinh,
            BigDecimal soTienGiam,
            BigDecimal tienVat,
            BigDecimal tongThanhToan,
            String phuongThucThanhToan,
            String trangThai
    ) {
        this.maHoaDon = maHoaDon;
        this.maDonHang = maDonHang;
        this.maKhachHang = maKhachHang;
        this.tenKhachHang = tenKhachHang;
        this.maNhanVien = maNhanVien;
        this.tenNhanVien = tenNhanVien;
        this.ngayLap = ngayLap;
        this.tamTinh = tamTinh;
        this.soTienGiam = soTienGiam;
        this.tienVat = tienVat;
        this.tongThanhToan = tongThanhToan;
        this.phuongThucThanhToan = phuongThucThanhToan;
        this.trangThai = trangThai;
    }

    public String getMaHoaDon() {
        return maHoaDon;
    }

    public void setMaHoaDon(String maHoaDon) {
        this.maHoaDon = maHoaDon;
    }

    public String getMaDonHang() {
        return maDonHang;
    }

    public void setMaDonHang(String maDonHang) {
        this.maDonHang = maDonHang;
    }

    public String getMaKhachHang() {
        return maKhachHang;
    }

    public void setMaKhachHang(String maKhachHang) {
        this.maKhachHang = maKhachHang;
    }

    public String getTenKhachHang() {
        return tenKhachHang;
    }

    public void setTenKhachHang(String tenKhachHang) {
        this.tenKhachHang = tenKhachHang;
    }

    public String getMaNhanVien() {
        return maNhanVien;
    }

    public void setMaNhanVien(String maNhanVien) {
        this.maNhanVien = maNhanVien;
    }

    public String getTenNhanVien() {
        return tenNhanVien;
    }

    public void setTenNhanVien(String tenNhanVien) {
        this.tenNhanVien = tenNhanVien;
    }

    public LocalDateTime getNgayLap() {
        return ngayLap;
    }

    public void setNgayLap(LocalDateTime ngayLap) {
        this.ngayLap = ngayLap;
    }

    public BigDecimal getTamTinh() {
        return tamTinh;
    }

    public void setTamTinh(BigDecimal tamTinh) {
        this.tamTinh = tamTinh;
    }

    public BigDecimal getSoTienGiam() {
        return soTienGiam;
    }

    public void setSoTienGiam(BigDecimal soTienGiam) {
        this.soTienGiam = soTienGiam;
    }

    public BigDecimal getTienVat() {
        return tienVat;
    }

    public void setTienVat(BigDecimal tienVat) {
        this.tienVat = tienVat;
    }

    public BigDecimal getTongThanhToan() {
        return tongThanhToan;
    }

    public void setTongThanhToan(BigDecimal tongThanhToan) {
        this.tongThanhToan = tongThanhToan;
    }

    public String getPhuongThucThanhToan() {
        return phuongThucThanhToan;
    }

    public void setPhuongThucThanhToan(String phuongThucThanhToan) {
        this.phuongThucThanhToan = phuongThucThanhToan;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
}
