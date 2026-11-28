package com.phonestore.dto.pos;

public class PosEmployeeResponse {

    private final String maNhanVien;
    private final String hoTen;

    public PosEmployeeResponse(String maNhanVien, String hoTen) {
        this.maNhanVien = maNhanVien;
        this.hoTen = hoTen;
    }

    public String getMaNhanVien() {
        return maNhanVien;
    }

    public String getHoTen() {
        return hoTen;
    }
}
