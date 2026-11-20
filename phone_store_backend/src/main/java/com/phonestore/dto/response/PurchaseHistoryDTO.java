package com.phonestore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PurchaseHistoryDTO {
    private String maHoaDon;
    private String maSanPham;
    private String tenSanPham;
    private String maImei;
    private LocalDateTime ngayMua;
    private BigDecimal giaBan;
    private LocalDate hanBaoHanh;
    private String trangThaiBaoHanh;
    private Integer thoiGianBaoHanhThang;
}
