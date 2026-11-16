package com.phonestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatsResponse {
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private Integer maThuongHieu;
    private String tenThuongHieu;
    private Integer tongSoLuongBan;
    private BigDecimal tongDoanhThu;
    private String emptyMessage;
    private List<ProductSalesStatDTO> bestSellers;
    private List<StagnantProductDTO> stagnantProducts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSalesStatDTO {
        private String maSanPham;
        private String tenSanPham;
        private String tenThuongHieu;
        private String dongMay;
        private String mauSac;
        private String ram;
        private String rom;
        private Integer soLuongDaBan;
        private BigDecimal doanhThu;
        private BigDecimal tienVon;
        private BigDecimal loiNhuan;
        private Integer tonKhoHienTai;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StagnantProductDTO {
        private String maSanPham;
        private String tenSanPham;
        private String tenThuongHieu;
        private String dongMay;
        private Integer soLuongTon;
        private Long soNgayTonKho;
        private LocalDateTime ngayNhapGanNhat;
        private BigDecimal giaTriTonKho;
        private Integer soLuongBanTrongKy;
    }
}

