package com.phonestore.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderCreateRequest {
    private String maPhieuNhap;
    private String maNhaCungCap;
    private String maNhanVien;
    private String ghiChu;
    private Double daThanhToan;
    private List<PurchaseItemRequest> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseItemRequest {
        private String maSanPham;
        private Integer soLuong;
        private Double donGiaNhap;
        private List<String> imeis; // Danh sách các mã IMEI đã quét cho sản phẩm này
    }
}
