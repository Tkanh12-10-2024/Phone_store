
package com.phonestore.dto.pos;

/**
 * DTO trả thông tin khách hàng cho màn hình POS.
 */
public class PosCustomerResponse {

    private String maKhachHang;

    private String hoTen;

    private String soDienThoai;

    private String email;

    private String hangKhachHang;

    private int diemTichLuy;

    public PosCustomerResponse() {
    }

    public PosCustomerResponse(
            String maKhachHang,
            String hoTen,
            String soDienThoai,
            String email,
            String hangKhachHang,
            int diemTichLuy
    ) {
        this.maKhachHang = maKhachHang;
        this.hoTen = hoTen;
        this.soDienThoai = soDienThoai;
        this.email = email;
        this.hangKhachHang = hangKhachHang;
        this.diemTichLuy = diemTichLuy;
    }

    public String getMaKhachHang() {
        return maKhachHang;
    }

    public void setMaKhachHang(String maKhachHang) {
        this.maKhachHang = maKhachHang;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHangKhachHang() {
        return hangKhachHang;
    }

    public void setHangKhachHang(String hangKhachHang) {
        this.hangKhachHang = hangKhachHang;
    }

    public int getDiemTichLuy() {
        return diemTichLuy;
    }

    public void setDiemTichLuy(int diemTichLuy) {
        this.diemTichLuy = diemTichLuy;
    }
}
