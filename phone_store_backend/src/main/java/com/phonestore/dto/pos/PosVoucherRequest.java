
package com.phonestore.dto.pos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO nhận request kiểm tra và áp dụng voucher trong POS.
 */
public class PosVoucherRequest {

    private String codeVoucher;

    private String maKhachHang;

    private BigDecimal vatPhanTram = BigDecimal.ZERO;

    private List<ItemRequest> items = new ArrayList<>();

    public PosVoucherRequest() {
    }

    public String getCodeVoucher() {
        return codeVoucher;
    }

    public void setCodeVoucher(String codeVoucher) {
        this.codeVoucher = codeVoucher;
    }

    public String getMaKhachHang() {
        return maKhachHang;
    }

    public void setMaKhachHang(String maKhachHang) {
        this.maKhachHang = maKhachHang;
    }

    public BigDecimal getVatPhanTram() {
        return vatPhanTram;
    }

    public void setVatPhanTram(BigDecimal vatPhanTram) {
        this.vatPhanTram = vatPhanTram != null
                ? vatPhanTram
                : BigDecimal.ZERO;
    }

    public List<ItemRequest> getItems() {
        return items;
    }

    public void setItems(List<ItemRequest> items) {
        this.items = items != null
                ? items
                : new ArrayList<>();
    }

    /**
     * Sản phẩm trong giỏ hàng dùng để kiểm tra điều kiện voucher.
     */
    public static class ItemRequest {

        private String maSanPham;

        private int soLuong;

        /*
         * Chỉ giữ để tương thích payload frontend.
         * Backend phải truy vấn giá thực tế từ database.
         */
        private BigDecimal donGia;

        public ItemRequest() {
        }

        public String getMaSanPham() {
            return maSanPham;
        }

        public void setMaSanPham(String maSanPham) {
            this.maSanPham = maSanPham;
        }

        public int getSoLuong() {
            return soLuong;
        }

        public void setSoLuong(int soLuong) {
            this.soLuong = soLuong;
        }

        public BigDecimal getDonGia() {
            return donGia;
        }

        public void setDonGia(BigDecimal donGia) {
            this.donGia = donGia;
        }
    }
}
