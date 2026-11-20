package com.phonestore.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantItemRequest {
    private String maSanPham;
    private String tenSanPham;
    private String mauSac;
    private String ram;
    private String rom;
    private BigDecimal giaNhapThamKhao;
    private BigDecimal giaBan;
    private Integer dinhMucTonToiThieu;
    private String hinhAnh;
}
