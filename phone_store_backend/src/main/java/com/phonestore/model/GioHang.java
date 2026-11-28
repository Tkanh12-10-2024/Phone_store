package com.phonestore.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GioHang {
    private long maGio;
    private LocalDateTime ngayCapNhat = LocalDateTime.now();
    private LocalDateTime ngayTao = LocalDateTime.now();
    private List<ChiTietGioHang> chiTietList = new ArrayList<>();

    public void themSanPham(ChiTietGioHang item) {
        if (item != null) {
            item.tinhThanhTien();
            this.chiTietList.add(item);
            this.ngayCapNhat = LocalDateTime.now();
        }
    }

    public void xoaSanPham(int index) {
        if (index >= 0 && index < this.chiTietList.size()) {
            this.chiTietList.remove(index);
            this.ngayCapNhat = LocalDateTime.now();
        }
    }

    public void capNhatSoLuong(int index, int soLuongMoi) {
        if (index >= 0 && index < this.chiTietList.size()) {
            this.chiTietList.get(index).capNhatSoLuong(soLuongMoi);
            this.ngayCapNhat = LocalDateTime.now();
        }
    }

    public BigDecimal tinhTongTien() {
        return this.chiTietList.stream()
                .map(ChiTietGioHang::tinhThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void lamMoiGioHang() {
        this.chiTietList.clear();
        this.ngayCapNhat = LocalDateTime.now();
    }

    public long getMaGio() { return maGio; }
    public void setMaGio(long maGio) { this.maGio = maGio; }
    public LocalDateTime getNgayCapNhat() { return ngayCapNhat; }
    public void setNgayCapNhat(LocalDateTime ngayCapNhat) { this.ngayCapNhat = ngayCapNhat; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
    public List<ChiTietGioHang> getChiTietList() { return chiTietList; }
    public void setChiTietList(List<ChiTietGioHang> chiTietList) { this.chiTietList = chiTietList; }
}