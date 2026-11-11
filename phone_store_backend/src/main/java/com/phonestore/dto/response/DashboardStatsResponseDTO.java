package com.phonestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponseDTO {
    private Double totalRevenue;
    private Long totalOrders;
    private Long totalPhonesSold;
    private Integer stockAlertCount;
}