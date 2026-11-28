package com.phonestore.dto.product;

import java.math.BigDecimal;

public record ProductVariantRequest(
        String maSanPham,
        String mauSac,
        String ram,
        String rom,
        BigDecimal giaNhapThamKhao,
        BigDecimal giaBan,
        Integer dinhMucTonToiThieu
) {
}
