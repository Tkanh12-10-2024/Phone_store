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
public class InventoryStatsResponse {
    private Integer tongMaSanPham;
    private Integer tongSoLuongTon;
    private Integer tongDangGiuCho;
    private Integer tongKhaDung;
    private Integer soSanPhamCanhBao;
    private BigDecimal tongGiaTriTonKho;
    private List<InventoryStatItemDTO> items;
    private List<InventoryStatItemDTO> alerts;
    private List<StockMovementDTO> movements;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryStatItemDTO {
        private String maSanPham;
        private String tenSanPham;
        private Integer maThuongHieu;
        private String tenThuongHieu;
        private String dongMay;
        private String mauSac;
        private String ram;
        private String rom;
        private Integer soLuongTon;
        private Integer soLuongGiuCho;
        private Integer soLuongKhaDung;
        private Integer dinhMucTonToiThieu;
        private BigDecimal giaNhapThamKhao;
        private BigDecimal giaBan;
        private BigDecimal giaTriTonKho;
        private String mucDoCanhBao;
        private String canhBaoThongDiep;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockMovementDTO {
        private LocalDateTime thoiGian;
        private String loaiBienDong;
        private String maChungTu;
        private String maSanPham;
        private String tenSanPham;
        private Integer thayDoiSoLuong;
        private BigDecimal donGia;
        private String nhanVienThucHien;
        private String ghiChu;
    }
}

