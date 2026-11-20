package com.phonestore.dto.response;

import com.phonestore.entity.InventoryCheck;
import com.phonestore.entity.InventoryCheckDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCheckDetailResponse {
    private InventoryCheck inventoryCheck;
    private List<ItemDetail> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemDetail {
        private InventoryCheckDetail detail;
        private String tenSanPham;
    }
}
