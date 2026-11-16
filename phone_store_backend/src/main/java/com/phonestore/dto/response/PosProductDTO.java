package com.phonestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosProductDTO {
    private String maSanPham;
    private String tenSanPham;
    private Integer maThuongHieu;
    private String tenThuongHieu;
    private Integer maDanhMuc;
    private String tenDanhMuc;
    private String dongMay;
    private String mauSac;
    private String ram;
    private String rom;
    private BigDecimal giaBan;
    private Integer soLuongTon;
    private Integer dinhMucTonToiThieu;
    private Integer thoiGianBaoHanhThang;
    private List<String> imeis;
}

