package com.phonestore.service;

import com.phonestore.dto.response.DashboardStatsResponseDTO;
import com.phonestore.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    public DashboardStatsResponseDTO getDashboardStats() {
        Double revenue = invoiceRepository.getTotalRevenue();
        if (revenue == null) {
            revenue = 0.0;
        }

        long completedOrders = invoiceRepository.countByStatus("COMPLETED");
        long phonesSold = completedOrders * 1;
        int stockAlertCount = 4;

        return new DashboardStatsResponseDTO(revenue, completedOrders, phonesSold, stockAlertCount);
    }
}