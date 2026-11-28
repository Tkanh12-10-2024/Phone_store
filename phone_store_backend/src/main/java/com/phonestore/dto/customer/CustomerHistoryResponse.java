package com.phonestore.dto.customer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerHistoryResponse(
        String maHoaDon,
        String tenSanPham,
        BigDecimal giaBan,
        LocalDateTime ngayMua,
        String maImei,
        int thoiGianBaoHanhThang,
        LocalDate hanBaoHanh
) {
}
