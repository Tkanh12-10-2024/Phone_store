
package com.phonestore.dto.pos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO nhận request thanh toán POS từ frontend.
 *
 * Frontend gửi:
 * - Thông tin khách hàng.
 * - Thông tin nhân viên.
 * - Voucher (nếu có).
 * - Phương thức thanh toán.
 * - VAT.
 * - Tiền khách đưa.
 * - Danh sách sản phẩm và số lượng.
 *
 * Backend không tin tưởng đơn giá từ frontend.
 * Giá bán thực tế phải được truy vấn lại từ database.
 */
public class PosCheckoutRequest {

    private String maKhachHang;

    private String maNhanVien;

    private String codeVoucher;

    private String phuongThucThanhToan;

    private BigDecimal vatPhanTram = BigDecimal.ZERO;

    private BigDecimal tienKhachDua = BigDecimal.ZERO;

    private List<ItemRequest> items = new ArrayList<>();

    public PosCheckoutRequest() {
    }

    public String getMaKhachHang() {
        return maKhachHang;
    }

    public void setMaKhachHang(String maKhachHang) {
        this.maKhachHang = maKhachHang;
    }

    public String getMaNhanVien() {
        return maNhanVien;
    }

    public void setMaNhanVien(String maNhanVien) {
        this.maNhanVien = maNhanVien;
    }

    public String getCodeVoucher() {
        return codeVoucher;
    }

    public void setCodeVoucher(String codeVoucher) {
        this.codeVoucher = codeVoucher;
    }

    public String getPhuongThucThanhToan() {
        return phuongThucThanhToan;
    }

    public void setPhuongThucThanhToan(String phuongThucThanhToan) {
        this.phuongThucThanhToan = phuongThucThanhToan;
    }

    public BigDecimal getVatPhanTram() {
        return vatPhanTram;
    }

    public void setVatPhanTram(BigDecimal vatPhanTram) {
        this.vatPhanTram = vatPhanTram != null
                ? vatPhanTram
                : BigDecimal.ZERO;
    }

    public BigDecimal getTienKhachDua() {
        return tienKhachDua;
    }

    public void setTienKhachDua(BigDecimal tienKhachDua) {
        this.tienKhachDua = tienKhachDua != null
                ? tienKhachDua
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
     * DTO cho từng sản phẩm trong giỏ hàng.
     */
    public static class ItemRequest {

        private String maSanPham;

        private int soLuong;

        /*
         * Frontend hiện vẫn gửi donGia.
         * Backend chỉ nhận để tương thích payload,
         * tuyệt đối không dùng giá trị này để tính tiền.
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
