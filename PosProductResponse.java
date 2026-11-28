
package com.phonestore.dto.pos;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO trả thông tin sản phẩm cho màn hình POS.
 */
public class PosProductResponse {

    private String maSanPham;
    private String tenSanPham;

    private String maThuongHieu;
    private String tenThuongHieu;

    private String dongMay;
    private String ram;
    private String rom;
    private String mauSac;

    private BigDecimal giaBan;

    private int soLuongTon;

    /**
     * Chỉ dùng để tương thích dữ liệu frontend cũ.
     * Không trả danh sách IMEI thực tế từ kho.
     * Backend tự phân bổ IMEI khi thanh toán.
     */
    private List<String> imeis;

    public PosProductResponse() {
    }

    public PosProductResponse(
            String maSanPham,
            String tenSanPham,
            String maThuongHieu,
            String tenThuongHieu,
            String dongMay,
            String ram,
            String rom,
            String mauSac,
            BigDecimal giaBan,
            int soLuongTon
    ) {
        this.maSanPham = maSanPham;
        this.tenSanPham = tenSanPham;
        this.maThuongHieu = maThuongHieu;
        this.tenThuongHieu = tenThuongHieu;
        this.dongMay = dongMay;
        this.ram = ram;
        this.rom = rom;
        this.mauSac = mauSac;
        this.giaBan = giaBan;
        this.soLuongTon = soLuongTon;
        this.imeis = soLuongTon > 0 ? List.of("AVAILABLE") : List.of();
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

    public String getDongMay() {
        return dongMay;
    }

    public void setDongMay(String dongMay) {
        this.dongMay = dongMay;
    }

    public String getRam() {
        return ram;
    }

    public void setRam(String ram) {
        this.ram = ram;
    }

    public String getRom() {
        return rom;
    }

    public void setRom(String rom) {
        this.rom = rom;
    }

    public String getMauSac() {
        return mauSac;
    }

    public void setMauSac(String mauSac) {
        this.mauSac = mauSac;
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

    public List<String> getImeis() {
        return imeis;
    }

    public void setImeis(List<String> imeis) {
        this.imeis = imeis;
    }
}
