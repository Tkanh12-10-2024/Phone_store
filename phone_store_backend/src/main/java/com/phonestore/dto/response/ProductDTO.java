package com.phonestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
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
    private String manHinh;
    private String chipXuLy;
    private String pin;
    private String camera;
    private String heDieuHanh;
    private BigDecimal giaNhapThamKhao;
    private BigDecimal giaBan;
    private Integer thoiGianBaoHanhThang;
    private Integer soLuongTon;
    private Integer dinhMucTonToiThieu;
    private String hinhAnh;
    private String trangThai;
    private LocalDateTime ngayTao;
    private List<ProductGalleryDTO> galleries;
    private List<ProductVariantSummaryDTO> siblingVariants;
    private boolean coLichSuGiaoDich;
}
