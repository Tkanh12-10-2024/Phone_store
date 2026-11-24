package com.phonestore.service;

import com.phonestore.dto.response.DashboardStatsResponseDTO;
import com.phonestore.entity.Product;
import com.phonestore.repository.EmployeeRepository;
import com.phonestore.repository.InvoiceRepository;
import com.phonestore.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private EmployeeRepository employeeRepository; // 👇 Inject EmployeeRepository

    @Autowired
    private ProductRepository productRepository;

    public DashboardStatsResponseDTO getDashboardStats() {
        Double revenue = invoiceRepository.getTotalRevenue();
        if (revenue == null) {
            revenue = 0.0;
        }

        long completedOrders = invoiceRepository.countByStatus("COMPLETED");
        long phonesSold = completedOrders * 1;

        List<Product> lowStockProducts = productRepository.findLowStockProducts();
        int stockAlertCount = lowStockProducts != null ? lowStockProducts.size() : 0;
        List<DashboardStatsResponseDTO.StockAlertItem> stockAlerts = lowStockProducts == null ? List.of() :
                lowStockProducts.stream()
                        .map(p -> new DashboardStatsResponseDTO.StockAlertItem(
                                p.getTenSanPham(),
                                p.getSoLuongTon() != null ? p.getSoLuongTon() : 0,
                                p.getDinhMucTonToiThieu() != null ? p.getDinhMucTonToiThieu() : 0
                        ))
                        .collect(Collectors.toList());

        // 👇 Đếm tổng số nhân viên thực tế từ bảng nhan_vien
        long totalEmployees = employeeRepository.count();

        return new DashboardStatsResponseDTO(revenue, completedOrders, phonesSold, stockAlertCount, totalEmployees, stockAlerts);
    }
}