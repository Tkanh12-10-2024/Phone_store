package com.phonestore.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandRequest {
    private String tenThuongHieu;
    private String quocGia;
    private String logoUrl;
    private String moTa;
}
