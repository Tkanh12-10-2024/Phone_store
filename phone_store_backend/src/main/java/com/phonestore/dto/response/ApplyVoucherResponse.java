package com.phonestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyVoucherResponse {
    private boolean valid;
    private String errorCode;
    private String message;
    private String maKhuyenMai;
    private String codeVoucher;
    private String tenKhuyenMai;
    private String loaiGiamGia;
    private String quaTang;
    private BigDecimal tamTinh;
    private BigDecimal soTienGiam;
    private BigDecimal tienVat;
    private BigDecimal tongThanhToan;
}

