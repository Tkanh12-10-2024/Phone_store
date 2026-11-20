package com.phonestore.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductGalleryRequest {
    private String duongDanAnh;
    private Boolean laAnhChinh;
    private Integer thuTu;
}
