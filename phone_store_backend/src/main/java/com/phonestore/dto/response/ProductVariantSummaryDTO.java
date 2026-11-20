package com.phonestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantSummaryDTO {
    private String maSanPham;
    private String tenSanPham;
    private String mauSac;
    private String ram;
    private String rom;
    private BigDecimal giaBan;
    private Integer soLuongTon;
    private String trangThai;
    private String hinhAnh;
}
