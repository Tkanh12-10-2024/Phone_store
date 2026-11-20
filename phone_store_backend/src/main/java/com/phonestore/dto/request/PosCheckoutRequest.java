package com.phonestore.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PosCheckoutRequest {
    private String maKhachHang;
    private String maNhanVien;
    private String codeVoucher;
    private String phuongThucThanhToan; // Tien_mat, Chuyen_khoan_QR, The
    private BigDecimal vatPhanTram;
    private BigDecimal tienKhachDua;
    private String ghiChu;
    private List<CheckoutItem> items;

    @Data
    public static class CheckoutItem {
        private String maSanPham;
        private String maImei;
        private Integer soLuong;
        private BigDecimal donGia;
    }
}

