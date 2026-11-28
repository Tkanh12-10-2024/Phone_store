package com.phonestore.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DonHang {
    private String maDonHang;
    private LocalDateTime ngayTao = LocalDateTime.now();
    private BigDecimal tongTienHang = BigDecimal.ZERO;
    private BigDecimal tienGiamGia = BigDecimal.ZERO;
    private BigDecimal thueVat = BigDecimal.ZERO;
    private BigDecimal tongTienCanTra = BigDecimal.ZERO;
    private String trangThai = "Moi_tao";
    private String ghiChu;
    private List<ChiTietDonHang> chiTietList = new ArrayList<>();

    public void taoDonHang() {
        this.ngayTao = LocalDateTime.now();
        this.trangThai = "Cho_xac_nhan";
        tinhTongTienCanTra();
    }

    public void themChiTietDonHang(ChiTietDonHang chiTiet) {
        if (chiTiet != null) {
            chiTiet.tinhThanhTien();
            this.chiTietList.add(chiTiet);
            tinhTongTienCanTra();
        }
    }

    public BigDecimal tinhTongTienHang() {
        this.tongTienHang = this.chiTietList.stream()
                .map(ChiTietDonHang::tinhThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return this.tongTienHang;
    }

    public void apDungKhuyenMai(KhuyenMai km) {
        tinhTongTienHang();
        this.tienGiamGia = km != null ? km.tinhGiaTriGiam(this.tongTienHang) : BigDecimal.ZERO;
        tinhTongTienCanTra();
    }

    public BigDecimal tinhTongTienCanTra() {
        tinhTongTienHang();
        BigDecimal giam = this.tienGiamGia != null ? this.tienGiamGia : BigDecimal.ZERO;
        BigDecimal vatRate = this.thueVat != null ? this.thueVat : BigDecimal.ZERO;
        BigDecimal sauGiam = this.tongTienHang.subtract(giam).max(BigDecimal.ZERO);
        BigDecimal tienVat = sauGiam.multiply(vatRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        this.tongTienCanTra = sauGiam.add(tienVat);
        return this.tongTienCanTra;
    }

    public void huyDonHang() {
        this.trangThai = "Da_huy";
    }

    public void capNhatTrangThai(String trangThaiMoi) {
        if (trangThaiMoi != null && !trangThaiMoi.isBlank()) {
            this.trangThai = trangThaiMoi.trim();
        }
    }

    public String getMaDonHang() { return maDonHang; }
    public void setMaDonHang(String maDonHang) { this.maDonHang = maDonHang; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
    public BigDecimal getTongTienHang() { return tongTienHang; }
    public void setTongTienHang(BigDecimal tongTienHang) { this.tongTienHang = tongTienHang; }
    public BigDecimal getTienGiamGia() { return tienGiamGia; }
    public void setTienGiamGia(BigDecimal tienGiamGia) { this.tienGiamGia = tienGiamGia; }
    public BigDecimal getThueVat() { return thueVat; }
    public void setThueVat(BigDecimal thueVat) { this.thueVat = thueVat; }
    public BigDecimal getTongTienCanTra() { return tongTienCanTra; }
    public void setTongTienCanTra(BigDecimal tongTienCanTra) { this.tongTienCanTra = tongTienCanTra; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public List<ChiTietDonHang> getChiTietList() { return chiTietList; }
    public void setChiTietList(List<ChiTietDonHang> chiTietList) { this.chiTietList = chiTietList; }
}