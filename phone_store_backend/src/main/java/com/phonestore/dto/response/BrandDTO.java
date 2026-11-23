package com.phonestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandDTO {
    private Integer maThuongHieu;
    private String tenThuongHieu;
    private String quocGia;
    private String logoUrl;
    private String moTa;
    private Long soLuongSanPham;
}
