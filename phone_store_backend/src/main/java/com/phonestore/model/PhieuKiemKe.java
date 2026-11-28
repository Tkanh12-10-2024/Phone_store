package com.phonestore.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PhieuKiemKe {
    private String maPhieuKiemKe;
    private LocalDateTime ngayKiemKe = LocalDateTime.now();
    private String trangThai = "Moi_tao";
    private String ghiChu;
    private List<ChiTietKiemKe> chiTietList = new ArrayList<>();

    public void taoPhieuKiemKe() {
        this.ngayKiemKe = LocalDateTime.now();
        this.trangThai = "Dang_kiem_ke";
    }

    public void themChiTietKiemKe(ChiTietKiemKe chiTiet) {
        if (chiTiet != null) {
            chiTiet.tinhChenhLech();
            this.chiTietList.add(chiTiet);
        }
    }

    public void xacNhanKiemKe() {
        this.chiTietList.forEach(ChiTietKiemKe::tinhChenhLech);
        this.trangThai = "Da_can_bang";
    }

    public int xuLyChenhLech() {
        int tongLech = 0;
        for (ChiTietKiemKe ct : this.chiTietList) {
            tongLech += ct.tinhChenhLech();
        }
        return tongLech;
    }

    public String getMaPhieuKiemKe() { return maPhieuKiemKe; }
    public void setMaPhieuKiemKe(String maPhieuKiemKe) { this.maPhieuKiemKe = maPhieuKiemKe; }
    public LocalDateTime getNgayKiemKe() { return ngayKiemKe; }
    public void setNgayKiemKe(LocalDateTime ngayKiemKe) { this.ngayKiemKe = ngayKiemKe; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public List<ChiTietKiemKe> getChiTietList() { return chiTietList; }
    public void setChiTietList(List<ChiTietKiemKe> chiTietList) { this.chiTietList = chiTietList; }
}