package com.phonestore.dto.response;

import com.phonestore.entity.PurchaseOrder;
import com.phonestore.entity.PurchaseOrderDetail;
import com.phonestore.entity.Supplier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderDetailResponse {
    private PurchaseOrder purchaseOrder;
    private Supplier supplier;
    private List<ItemDetail> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemDetail {
        private PurchaseOrderDetail detail;
        private String tenSanPham;
        private List<String> imeis;
    }
}
