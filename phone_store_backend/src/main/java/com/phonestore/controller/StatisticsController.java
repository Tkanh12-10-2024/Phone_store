package com.phonestore.controller;

import com.phonestore.dto.response.InventoryStatsResponse;
import com.phonestore.dto.response.ProductStatsResponse;
import com.phonestore.dto.response.RevenueStatsResponse;
import com.phonestore.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        return ResponseEntity.ok(statisticsService.getDashboardOverview());
    }

    @GetMapping("/revenue")
    public ResponseEntity<RevenueStatsResponse> getRevenueStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(defaultValue = "DAY") String groupBy
    ) {
        return ResponseEntity.ok(statisticsService.getRevenueStatistics(tuNgay, denNgay, groupBy));
    }

    @GetMapping("/products")
    public ResponseEntity<ProductStatsResponse> getProductStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Integer maThuongHieu
    ) {
        return ResponseEntity.ok(statisticsService.getProductStatistics(tuNgay, denNgay, maThuongHieu));
    }

    @GetMapping("/inventory")
    public ResponseEntity<InventoryStatsResponse> getInventoryStats(
            @RequestParam(required = false) Integer maThuongHieu,
            @RequestParam(required = false) String dongMay,
            @RequestParam(required = false) String ram,
            @RequestParam(required = false) String rom,
            @RequestParam(required = false) String mauSac,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean onlyAlert
    ) {
        return ResponseEntity.ok(statisticsService.getInventoryStatistics(
                maThuongHieu, dongMay, ram, rom, mauSac, keyword, onlyAlert));
    }

    @GetMapping("/inventory/filters")
    public ResponseEntity<Map<String, Object>> getInventoryFilters() {
        return ResponseEntity.ok(statisticsService.getInventoryFilters());
    }
}

