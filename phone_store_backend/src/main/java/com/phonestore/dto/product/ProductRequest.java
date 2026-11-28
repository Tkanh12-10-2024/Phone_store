package com.phonestore.dto.product;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        String tenSanPham,
        Integer maThuongHieu,
        Integer maDanhMuc,
        String dongMay,
        String manHinh,
        String chipXuLy,
        String pin,
        String camera,
        String heDieuHanh,
        Integer thoiGianBaoHanhThang,
        String trangThai,
        String hinhAnh,
        List<String> galleryUrls,
        List<ProductVariantRequest> variants,
        String maSanPham,
        String mauSac,
        String ram,
        String rom,
        BigDecimal giaNhapThamKhao,
        BigDecimal giaBan,
        Integer dinhMucTonToiThieu
) {
}
