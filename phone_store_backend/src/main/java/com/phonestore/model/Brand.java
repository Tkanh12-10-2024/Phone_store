package com.phonestore.model;

/**
 * Model đại diện dữ liệu thương hiệu trong hệ thống.
 * Không chứa logic truy vấn SQL hoặc xử lý HTTP.
 */
public class Brand {
    private Integer maThuongHieu;
    private String tenThuongHieu;
    private String quocGia;
    private String logoUrl;
    private String moTa;
    private Long soLuongSanPham;

    public Integer getMaThuongHieu() { return maThuongHieu; }
    public void setMaThuongHieu(Integer maThuongHieu) { this.maThuongHieu = maThuongHieu; }
    public String getTenThuongHieu() { return tenThuongHieu; }
    public void setTenThuongHieu(String tenThuongHieu) { this.tenThuongHieu = tenThuongHieu; }
    public String getQuocGia() { return quocGia; }
    public void setQuocGia(String quocGia) { this.quocGia = quocGia; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public Long getSoLuongSanPham() { return soLuongSanPham; }
    public void setSoLuongSanPham(Long soLuongSanPham) { this.soLuongSanPham = soLuongSanPham; }
}