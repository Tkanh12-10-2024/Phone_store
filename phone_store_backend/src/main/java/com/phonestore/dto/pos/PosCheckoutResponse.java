
package com.phonestore.dto.pos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO trả kết quả thanh toán POS.
 */
public class PosCheckoutResponse {

    private boolean success;

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

    private BigDecimal tienKhachDua;
    private BigDecimal tienThua;

    private String phuongThucThanhToan;
    private String codeVoucher;

    private String quaTang;

    private List<PosInvoiceDetailResponse> items = new ArrayList<>();

    private String message;

    public PosCheckoutResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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

    public BigDecimal getTienKhachDua() {
        return tienKhachDua;
    }

    public void setTienKhachDua(BigDecimal tienKhachDua) {
        this.tienKhachDua = tienKhachDua;
    }

    public BigDecimal getTienThua() {
        return tienThua;
    }

    public void setTienThua(BigDecimal tienThua) {
        this.tienThua = tienThua;
    }

    public String getPhuongThucThanhToan() {
        return phuongThucThanhToan;
    }

    public void setPhuongThucThanhToan(String phuongThucThanhToan) {
        this.phuongThucThanhToan = phuongThucThanhToan;
    }

    public String getCodeVoucher() {
        return codeVoucher;
    }

    public void setCodeVoucher(String codeVoucher) {
        this.codeVoucher = codeVoucher;
    }

    public String getQuaTang() {
        return quaTang;
    }

    public void setQuaTang(String quaTang) {
        this.quaTang = quaTang;
    }

    public List<PosInvoiceDetailResponse> getItems() {
        return items;
    }

    public void setItems(List<PosInvoiceDetailResponse> items) {
        this.items = items;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getTongTienHang() {
        return tamTinh;
    }

    public BigDecimal getTienGiamGia() {
        return soTienGiam;
    }

    public BigDecimal getTienThueVat() {
        return tienVat;
    }
}
