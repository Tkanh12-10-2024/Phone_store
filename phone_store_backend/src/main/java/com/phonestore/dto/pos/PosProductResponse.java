
package com.phonestore.dto.pos;

import java.math.BigDecimal;

/**
 * DTO trả thông tin sản phẩm cho màn hình POS.
 */
public class PosProductResponse {

    private String maSanPham;

    private String tenSanPham;

    private String dongMay;

    private String maThuongHieu;

    private String tenThuongHieu;

    private BigDecimal giaBan;

    private int soLuongTon;

    private String hinhAnh;

    private String trangThai;

    public PosProductResponse() {
    }

    public PosProductResponse(
            String maSanPham,
            String tenSanPham,
            String dongMay,
            String maThuongHieu,
            String tenThuongHieu,
            BigDecimal giaBan,
            int soLuongTon,
            String hinhAnh,
            String trangThai
    ) {
        this.maSanPham = maSanPham;
        this.tenSanPham = tenSanPham;
        this.dongMay = dongMay;
        this.maThuongHieu = maThuongHieu;
        this.tenThuongHieu = tenThuongHieu;
        this.giaBan = giaBan;
        this.soLuongTon = soLuongTon;
        this.hinhAnh = hinhAnh;
        this.trangThai = trangThai;
    }

    public PosProductResponse(String maSanPham, String tenSanPham, String maThuongHieu, String tenThuongHieu, String dongMay, String ram, String rom, String mauSac, BigDecimal giaBan, int soLuongTon) {
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

    public String getDongMay() {
        return dongMay;
    }

    public void setDongMay(String dongMay) {
        this.dongMay = dongMay;
    }

    public String getMaThuongHieu() {
        return maThuongHieu;
    }

    public void setMaThuongHieu(String maThuongHieu) {
        this.maThuongHieu = maThuongHieu;
    }

    public String getTenThuongHieu() {
        return tenThuongHieu;
    }

    public void setTenThuongHieu(String tenThuongHieu) {
        this.tenThuongHieu = tenThuongHieu;
    }

    public BigDecimal getGiaBan() {
        return giaBan;
    }

    public void setGiaBan(BigDecimal giaBan) {
        this.giaBan = giaBan;
    }

    public int getSoLuongTon() {
        return soLuongTon;
    }

    public void setSoLuongTon(int soLuongTon) {
        this.soLuongTon = soLuongTon;
    }

    public String getHinhAnh() {
        return hinhAnh;
    }

    public void setHinhAnh(String hinhAnh) {
        this.hinhAnh = hinhAnh;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
}
