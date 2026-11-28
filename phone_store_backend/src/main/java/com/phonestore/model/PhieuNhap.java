package com.phonestore.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PhieuNhap {
    private String maPhieuNhap;
    private LocalDateTime ngayNhap = LocalDateTime.now();
    private BigDecimal tongTien = BigDecimal.ZERO;
    private BigDecimal daThanhToan = BigDecimal.ZERO;
    private BigDecimal congNoNhaCungCap = BigDecimal.ZERO;
    private String trangThai = "Moi_tao";
    private String ghiChu;
    private List<ChiTietPhieuNhap> chiTietList = new ArrayList<>();

    public void taoPhieuNhap() {
        this.ngayNhap = LocalDateTime.now();
        this.trangThai = "Cho_xac_nhan";
        tinhTongTien();
    }

    public void themChiTietNhap(ChiTietPhieuNhap chiTiet) {
        if (chiTiet != null) {
            this.chiTietList.add(chiTiet);
            tinhTongTien();
        }
    }

    public BigDecimal tinhTongTien() {
        this.tongTien = this.chiTietList.stream()
                .map(ChiTietPhieuNhap::tinhThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = this.daThanhToan != null ? this.daThanhToan : BigDecimal.ZERO;
        this.congNoNhaCungCap = this.tongTien.subtract(paid).max(BigDecimal.ZERO);
        return this.tongTien;
    }

    public void xacNhanNhapKho() {
        tinhTongTien();
        this.trangThai = "Da_nhap_kho";
    }

    public String getMaPhieuNhap() { return maPhieuNhap; }
    public void setMaPhieuNhap(String maPhieuNhap) { this.maPhieuNhap = maPhieuNhap; }
    public LocalDateTime getNgayNhap() { return ngayNhap; }
    public void setNgayNhap(LocalDateTime ngayNhap) { this.ngayNhap = ngayNhap; }
    public BigDecimal getTongTien() { return tongTien; }
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }
    public BigDecimal getDaThanhToan() { return daThanhToan; }
    public void setDaThanhToan(BigDecimal daThanhToan) { this.daThanhToan = daThanhToan; }
    public BigDecimal getCongNoNhaCungCap() { return congNoNhaCungCap; }
    public void setCongNoNhaCungCap(BigDecimal congNoNhaCungCap) { this.congNoNhaCungCap = congNoNhaCungCap; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public List<ChiTietPhieuNhap> getChiTietList() { return chiTietList; }
    public void setChiTietList(List<ChiTietPhieuNhap> chiTietList) { this.chiTietList = chiTietList; }
}