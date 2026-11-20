package com.phonestore.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheckCreateRequest {
    private String maPhieuKiemKe;
    private String maNhanVien;
    private String ghiChu;
    private String trangThai; // Da_can_bang, Cho_xu_ly
    private List<CheckItemRequest> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckItemRequest {
        private String maSanPham;
        private Integer soLuongThucTe;
        private String lyDoChenhLech;
    }
}
