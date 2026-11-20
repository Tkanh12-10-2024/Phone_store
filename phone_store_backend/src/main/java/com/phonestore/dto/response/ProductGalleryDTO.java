package com.phonestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductGalleryDTO {
    private Integer maHinhAnh;
    private String maSanPham;
    private String duongDanAnh;
    private Boolean laAnhChinh;
    private Integer thuTu;
    private LocalDateTime ngayTao;
}
