package com.phonestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatsResponse {
    private String groupBy; // DAY, MONTH, YEAR
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private Integer tongHoaDon;
    private Integer tongSanPhamBan;
    private BigDecimal doanhThuGop;
    private BigDecimal tongChietKhau;
    private BigDecimal tongVat;
    private BigDecimal doanhThuThucThu;
    private BigDecimal tongTienVon;
    private BigDecimal loiNhuanGop;
    private BigDecimal tyLeTangTruongPhanTram;
    private BigDecimal doanhThuKyTruoc;
    private String emptyMessage;
    private List<RevenuePointDTO> series;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenuePointDTO {
        private String nhanThoiGian;
        private Integer soHoaDon;
        private BigDecimal doanhThuGop;
        private BigDecimal chietKhau;
        private BigDecimal doanhThuThucThu;
        private BigDecimal tienVon;
        private BigDecimal loiNhuanGop;
        private BigDecimal tangTruongPhanTram;
    }
}

