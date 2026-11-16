package com.phonestore.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ApplyVoucherRequest {
    private String codeVoucher;
    private String maKhachHang;
    private BigDecimal tamTinh;
    private BigDecimal vatPhanTram;
    private List<CartItemLine> items;

    @Data
    public static class CartItemLine {
        private String maSanPham;
        private Integer soLuong;
        private BigDecimal donGia;
    }
}

