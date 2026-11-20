package com.phonestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosCheckoutResponse {
    private String maHoaDon;
    private String maDonHang;
    private LocalDateTime ngayLap;
    private String maKhachHang;
    private String tenKhachHang;
    private String maNhanVien;
    private String tenNhanVien;
    private String phuongThucThanhToan;
    private String codeVoucher;
    private String quaTang;
    private BigDecimal tongTienHang;
    private BigDecimal tienGiamGia;
    private BigDecimal thueVatPhanTram;
    private BigDecimal tienThueVat;
    private BigDecimal tongThanhToan;
    private BigDecimal tienKhachDua;
    private BigDecimal tienThua;
    private String trangThaiThanhToan;
    private List<InvoiceLineDTO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceLineDTO {
        private String maSanPham;
        private String tenSanPham;
        private String maImei;
        private BigDecimal donGia;
        private Integer thoiGianBaoHanhThang;
    }
}

