package com.phonestore.model;

public class ChiTietKiemKe {
    private int maChiTietKiemKe;
    private int soLuongHeThong;
    private int soLuongThucTe;
    private int chenhLech;
    private String lyDoChenhLech;

    public int tinhChenhLech() {
        this.chenhLech = this.soLuongThucTe - this.soLuongHeThong;
        return this.chenhLech;
    }

    public void capNhatSoLuongThucTe(int soLuongMoi) {
        if (soLuongMoi >= 0) {
            this.soLuongThucTe = soLuongMoi;
            tinhChenhLech();
        }
    }

    public int getMaChiTietKiemKe() { return maChiTietKiemKe; }
    public void setMaChiTietKiemKe(int maChiTietKiemKe) { this.maChiTietKiemKe = maChiTietKiemKe; }
    public int getSoLuongHeThong() { return soLuongHeThong; }
    public void setSoLuongHeThong(int soLuongHeThong) { this.soLuongHeThong = soLuongHeThong; }
    public int getSoLuongThucTe() { return soLuongThucTe; }
    public void setSoLuongThucTe(int soLuongThucTe) { this.soLuongThucTe = soLuongThucTe; }
    public int getChenhLech() { return chenhLech; }
    public void setChenhLech(int chenhLech) { this.chenhLech = chenhLech; }
    public String getLyDoChenhLech() { return lyDoChenhLech; }
    public void setLyDoChenhLech(String lyDoChenhLech) { this.lyDoChenhLech = lyDoChenhLech; }
}