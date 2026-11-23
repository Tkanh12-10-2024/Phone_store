package com.phonestore.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {
    private String maSanPham;
    private String tenSanPham;
    private Integer maThuongHieu;
    private Integer maDanhMuc;
    private String dongMay;
    private String mauSac;
    private String ram;
    private String rom;
    private String manHinh;
    private String chipXuLy;
    private String pin;
    private String camera;
    private String heDieuHanh;
    private BigDecimal giaNhapThamKhao;
    private BigDecimal giaBan;
    private Integer thoiGianBaoHanhThang;
    private Integer dinhMucTonToiThieu;
    private String hinhAnh;
    private String trangThai;
    private List<String> galleryUrls;
    private List<ProductVariantItemRequest> variants;
}
