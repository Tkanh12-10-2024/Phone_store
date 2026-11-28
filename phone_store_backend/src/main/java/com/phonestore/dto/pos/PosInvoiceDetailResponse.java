
package com.phonestore.dto.pos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO trả thông tin chi tiết sản phẩm trong hóa đơn POS.
 */
public class PosInvoiceDetailResponse {

    private String maSanPham;

    private String tenSanPham;

    private int soLuong;

    private BigDecimal donGia;

    private BigDecimal thanhTien;

    /**
     * IMEI thực tế đã được xuất bán.
     */
    private List<String> danhSachImei = new ArrayList<>();

    /**
     * Thời hạn bảo hành tính theo tháng.
     */
    private int thoiGianBaoHanhThang;

    public PosInvoiceDetailResponse() {
    }

    public PosInvoiceDetailResponse(
            String maSanPham,
            String tenSanPham,
            int soLuong,
            BigDecimal donGia,
            BigDecimal thanhTien,
            List<String> danhSachImei,
            int thoiGianBaoHanhThang
    ) {
        this.maSanPham = maSanPham;
        this.tenSanPham = tenSanPham;
        this.soLuong = soLuong;
        this.donGia = donGia;
        this.thanhTien = thanhTien;
        this.danhSachImei = danhSachImei;
        this.thoiGianBaoHanhThang = thoiGianBaoHanhThang;
    }

    public String getMaSanPham() {
        return maSanPham;
    }

    public void setMaSanPham(String maSanPham) {
        this.maSanPham = maSanPham;
    }

    public String getTenSanPham() {
        return tenSanPham;
    }

    public void setTenSanPham(String tenSanPham) {
        this.tenSanPham = tenSanPham;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = soLuong;
    }

    public BigDecimal getDonGia() {
        return donGia;
    }

    public void setDonGia(BigDecimal donGia) {
        this.donGia = donGia;
    }

    public BigDecimal getThanhTien() {
        return thanhTien;
    }

    public void setThanhTien(BigDecimal thanhTien) {
        this.thanhTien = thanhTien;
    }

    public List<String> getDanhSachImei() {
        return danhSachImei;
    }

    public void setDanhSachImei(List<String> danhSachImei) {
        this.danhSachImei = danhSachImei;
    }

    public String getMaImei() {
        return danhSachImei == null || danhSachImei.isEmpty()
                ? null
                : danhSachImei.get(0);
    }

    public int getThoiGianBaoHanhThang() {
        return thoiGianBaoHanhThang;
    }

    public void setThoiGianBaoHanhThang(int thoiGianBaoHanhThang) {
        this.thoiGianBaoHanhThang = thoiGianBaoHanhThang;
    }
}
