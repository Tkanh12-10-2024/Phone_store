package com.phonestore.dto.response;

import java.util.List;

public class DashboardStatsResponseDTO {
    private Double totalRevenue;
    private Long totalOrders;
    private Long totalPhonesSold;
    private Integer stockAlertCount;
    private Long totalEmployees; // 👇 Thêm trường số lượng nhân viên
    private List<StockAlertItem> stockAlerts;

    public DashboardStatsResponseDTO() {}

    public DashboardStatsResponseDTO(Double totalRevenue, Long totalOrders, Long totalPhonesSold, Integer stockAlertCount, Long totalEmployees) {
        this.totalRevenue = totalRevenue;
        this.totalOrders = totalOrders;
        this.totalPhonesSold = totalPhonesSold;
        this.stockAlertCount = stockAlertCount;
        this.totalEmployees = totalEmployees;
    }

    public DashboardStatsResponseDTO(Double totalRevenue, Long totalOrders, Long totalPhonesSold, Integer stockAlertCount, Long totalEmployees, List<StockAlertItem> stockAlerts) {
        this.totalRevenue = totalRevenue;
        this.totalOrders = totalOrders;
        this.totalPhonesSold = totalPhonesSold;
        this.stockAlertCount = stockAlertCount;
        this.totalEmployees = totalEmployees;
        this.stockAlerts = stockAlerts;
    }

    // Getters and Setters
    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }

    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }

    public Long getTotalPhonesSold() { return totalPhonesSold; }
    public void setTotalPhonesSold(Long totalPhonesSold) { this.totalPhonesSold = totalPhonesSold; }

    public Integer getStockAlertCount() { return stockAlertCount; }
    public void setStockAlertCount(Integer stockAlertCount) { this.stockAlertCount = stockAlertCount; }

    public Long getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(Long totalEmployees) { this.totalEmployees = totalEmployees; }

    public List<StockAlertItem> getStockAlerts() { return stockAlerts; }
    public void setStockAlerts(List<StockAlertItem> stockAlerts) { this.stockAlerts = stockAlerts; }

    public static class StockAlertItem {
        private String productName;
        private Integer currentStock;
        private Integer minStock;

        public StockAlertItem() {}

        public StockAlertItem(String productName, Integer currentStock, Integer minStock) {
            this.productName = productName;
            this.currentStock = currentStock;
            this.minStock = minStock;
        }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public Integer getCurrentStock() { return currentStock; }
        public void setCurrentStock(Integer currentStock) { this.currentStock = currentStock; }

        public Integer getMinStock() { return minStock; }
        public void setMinStock(Integer minStock) { this.minStock = minStock; }
    }
}