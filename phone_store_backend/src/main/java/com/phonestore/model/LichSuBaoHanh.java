package com.phonestore.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LichSuBaoHanh {
    private int maLichSu;
    private LocalDateTime thoiGianCapNhat = LocalDateTime.now();
    private String trangThaiMoi;
    private String ghiChuTienDo;
    private BigDecimal tongTienDaNhan = BigDecimal.ZERO;
    private List<String> danhSachBaoHanh = new ArrayList<>();

    public void ghiNhanTienDo(String trangThai, String ghiChu) {
        this.thoiGianCapNhat = LocalDateTime.now();
        this.trangThaiMoi = trangThai;
        this.ghiChuTienDo = ghiChu;
    }

    public void ghiNhanTienDo(BigDecimal soTien) {
        if (soTien != null && soTien.compareTo(BigDecimal.ZERO) > 0) {
            this.tongTienDaNhan = this.tongTienDaNhan.add(soTien);
        }
        this.thoiGianCapNhat = LocalDateTime.now();
    }

    public List<String> layDanhSachBaoHanh() {
        return this.danhSachBaoHanh;
    }

    public int getMaLichSu() { return maLichSu; }
    public void setMaLichSu(int maLichSu) { this.maLichSu = maLichSu; }
    public LocalDateTime getThoiGianCapNhat() { return thoiGianCapNhat; }
    public void setThoiGianCapNhat(LocalDateTime thoiGianCapNhat) { this.thoiGianCapNhat = thoiGianCapNhat; }
    public String getTrangThaiMoi() { return trangThaiMoi; }
    public void setTrangThaiMoi(String trangThaiMoi) { this.trangThaiMoi = trangThaiMoi; }
    public String getGhiChuTienDo() { return ghiChuTienDo; }
    public void setGhiChuTienDo(String ghiChuTienDo) { this.ghiChuTienDo = ghiChuTienDo; }
    public BigDecimal getTongTienDaNhan() { return tongTienDaNhan; }
    public void setTongTienDaNhan(BigDecimal tongTienDaNhan) { this.tongTienDaNhan = tongTienDaNhan; }
    public List<String> getDanhSachBaoHanh() { return danhSachBaoHanh; }
    public void setDanhSachBaoHanh(List<String> danhSachBaoHanh) { this.danhSachBaoHanh = danhSachBaoHanh; }
}