package com.phonestore.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class KhuyenMai {
    private String maKhuyenMai;
    private String codeVoucher;
    private String tenKhuyenMai;
    private String loaiGiamGia;
    private BigDecimal giaTriGiam = BigDecimal.ZERO;
    private BigDecimal giaTriDonHangToiThieu = BigDecimal.ZERO;
    private BigDecimal giamToiDa = BigDecimal.ZERO;
    private int soLuongPhatHanh;
    private int soLuongDaDung;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private String trangThai = "Dang_dien_ra";

    public void taoKhuyenMai() {
        this.trangThai = "Dang_dien_ra";
    }

    public void capNhatKhuyenMai(KhuyenMai moi) {
        if (moi == null) return;
        this.tenKhuyenMai = moi.tenKhuyenMai;
        this.loaiGiamGia = moi.loaiGiamGia;
        this.giaTriGiam = moi.giaTriGiam;
        this.giaTriDonHangToiThieu = moi.giaTriDonHangToiThieu;
        this.giamToiDa = moi.giamToiDa;
        this.soLuongPhatHanh = moi.soLuongPhatHanh;
        this.ngayBatDau = moi.ngayBatDau;
        this.ngayKetThuc = moi.ngayKetThuc;
        if (moi.trangThai != null) {
            this.trangThai = moi.trangThai;
        }
    }

    public boolean kiemTraHieuLuc() {
        return kiemTraHieuLucChoDonHang(BigDecimal.ZERO, false);
    }

    public boolean kiemTraHieuLucChoDonHang(BigDecimal tongTienHang, boolean kiemTraDonToiThieu) {
        if (!"Dang_dien_ra".equals(this.trangThai)) return false;
        LocalDateTime now = LocalDateTime.now();
        if (ngayBatDau != null && now.isBefore(ngayBatDau)) return false;
        if (ngayKetThuc != null && now.isAfter(ngayKetThuc)) return false;
        if (soLuongPhatHanh > 0 && soLuongDaDung >= soLuongPhatHanh) return false;
        if (kiemTraDonToiThieu && giaTriDonHangToiThieu != null
                && tongTienHang != null && tongTienHang.compareTo(giaTriDonHangToiThieu) < 0) {
            return false;
        }
        return true;
    }

    public BigDecimal tinhGiaTriGiam() {
        return tinhGiaTriGiam(BigDecimal.ZERO);
    }

    public BigDecimal tinhGiaTriGiam(BigDecimal tongTienHang) {
        BigDecimal tienHang = tongTienHang != null ? tongTienHang : BigDecimal.ZERO;
        if (!kiemTraHieuLucChoDonHang(tienHang, true) || giaTriGiam == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal giam;
        if ("Phan_tram".equalsIgnoreCase(loaiGiamGia)) {
            giam = tienHang.multiply(giaTriGiam)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else if ("Qua_tang".equalsIgnoreCase(loaiGiamGia)) {
            giam = BigDecimal.ZERO;
        } else {
            giam = giaTriGiam;
        }
        if (giamToiDa != null && giamToiDa.compareTo(BigDecimal.ZERO) > 0 && giam.compareTo(giamToiDa) > 0) {
            giam = giamToiDa;
        }
        return giam.min(tienHang);
    }

    public boolean suDungKhuyenMai() {
        if (!kiemTraHieuLuc()) return false;
        this.soLuongDaDung += 1;
        return true;
    }

    public String getMaKhuyenMai() { return maKhuyenMai; }
    public void setMaKhuyenMai(String maKhuyenMai) { this.maKhuyenMai = maKhuyenMai; }
    public String getCodeVoucher() { return codeVoucher; }
    public void setCodeVoucher(String codeVoucher) { this.codeVoucher = codeVoucher; }
    public String getTenKhuyenMai() { return tenKhuyenMai; }
    public void setTenKhuyenMai(String tenKhuyenMai) { this.tenKhuyenMai = tenKhuyenMai; }
    public String getLoaiGiamGia() { return loaiGiamGia; }
    public void setLoaiGiamGia(String loaiGiamGia) { this.loaiGiamGia = loaiGiamGia; }
    public BigDecimal getGiaTriGiam() { return giaTriGiam; }
    public void setGiaTriGiam(BigDecimal giaTriGiam) { this.giaTriGiam = giaTriGiam; }
    public BigDecimal getGiaTriDonHangToiThieu() { return giaTriDonHangToiThieu; }
    public void setGiaTriDonHangToiThieu(BigDecimal giaTriDonHangToiThieu) { this.giaTriDonHangToiThieu = giaTriDonHangToiThieu; }
    public BigDecimal getGiamToiDa() { return giamToiDa; }
    public void setGiamToiDa(BigDecimal giamToiDa) { this.giamToiDa = giamToiDa; }
    public int getSoLuongPhatHanh() { return soLuongPhatHanh; }
    public void setSoLuongPhatHanh(int soLuongPhatHanh) { this.soLuongPhatHanh = soLuongPhatHanh; }
    public int getSoLuongDaDung() { return soLuongDaDung; }
    public void setSoLuongDaDung(int soLuongDaDung) { this.soLuongDaDung = soLuongDaDung; }
    public LocalDateTime getNgayBatDau() { return ngayBatDau; }
    public void setNgayBatDau(LocalDateTime ngayBatDau) { this.ngayBatDau = ngayBatDau; }
    public LocalDateTime getNgayKetThuc() { return ngayKetThuc; }
    public void setNgayKetThuc(LocalDateTime ngayKetThuc) { this.ngayKetThuc = ngayKetThuc; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
}