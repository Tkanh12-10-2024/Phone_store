package com.phonestore.dto.response;

public class DashboardStatsResponseDTO {
    private Double totalRevenue;
    private Long totalOrders;
    private Long totalPhonesSold;
    private Integer stockAlertCount;
    private Long totalEmployees; // 👇 Thêm trường số lượng nhân viên

    public DashboardStatsResponseDTO() {}

    public DashboardStatsResponseDTO(Double totalRevenue, Long totalOrders, Long totalPhonesSold, Integer stockAlertCount, Long totalEmployees) {
        this.totalRevenue = totalRevenue;
        this.totalOrders = totalOrders;
        this.totalPhonesSold = totalPhonesSold;
        this.stockAlertCount = stockAlertCount;
        this.totalEmployees = totalEmployees;
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
}