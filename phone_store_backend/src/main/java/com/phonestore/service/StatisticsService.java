package com.phonestore.service;

import com.phonestore.dao.PromotionDao;
import com.phonestore.dao.StatisticsDao;
import com.phonestore.dto.response.InventoryStatsResponse;
import com.phonestore.dto.response.ProductStatsResponse;
import com.phonestore.dto.response.PromotionResponse;
import com.phonestore.dto.response.RevenueStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatisticsDao statisticsDao;
    private final PromotionDao promotionDao;

    public RevenueStatsResponse getRevenueStatistics(LocalDate tuNgay, LocalDate denNgay, String groupBy) {
        LocalDate effectiveTo = denNgay != null ? denNgay : LocalDate.now();
        LocalDate effectiveFrom = tuNgay != null ? tuNgay : effectiveTo.minusDays(60);
        validateDateRange(effectiveFrom, effectiveTo);
        return statisticsDao.getRevenueStatistics(effectiveFrom, effectiveTo, groupBy);
    }

    public ProductStatsResponse getProductStatistics(LocalDate tuNgay, LocalDate denNgay, Integer maThuongHieu) {
        LocalDate effectiveTo = denNgay != null ? denNgay : LocalDate.now();
        LocalDate effectiveFrom = tuNgay != null ? tuNgay : effectiveTo.minusDays(90);
        validateDateRange(effectiveFrom, effectiveTo);
        return statisticsDao.getProductStatistics(effectiveFrom, effectiveTo, maThuongHieu);
    }

    public InventoryStatsResponse getInventoryStatistics(
            Integer maThuongHieu,
            String dongMay,
            String ram,
            String rom,
            String mauSac,
            String keyword,
            boolean onlyAlert
    ) {
        return statisticsDao.getInventoryStatistics(maThuongHieu, dongMay, ram, rom, mauSac, keyword, onlyAlert);
    }

    public Map<String, Object> getInventoryFilters() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("brands", promotionDao.findBrandOptions());
        filters.putAll(statisticsDao.getInventoryAttributeOptions());
        return filters;
    }

    public Map<String, Object> getDashboardOverview() {
        LocalDate today = LocalDate.now();
        RevenueStatsResponse monthRevenue = statisticsDao.getRevenueStatistics(today.minusDays(30), today, "DAY");
        InventoryStatsResponse inventory = statisticsDao.getInventoryStatistics(null, null, null, null, null, null, false);
        ProductStatsResponse products = statisticsDao.getProductStatistics(today.minusDays(90), today, null);
        List<PromotionResponse> promoAlerts = promotionDao.findAll(null, "Dang_dien_ra", null).stream()
                .filter(PromotionResponse::isCoCanhBao)
                .toList();

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("revenueStats", monthRevenue);
        overview.put("totalOrders30d", monthRevenue.getTongHoaDon());
        overview.put("totalStock", inventory.getTongSoLuongTon());
        overview.put("stockAlertCount", inventory.getSoSanPhamCanhBao());
        overview.put("stockAlerts", inventory.getAlerts());
        overview.put("bestSellers", products.getBestSellers());
        overview.put("promoAlertCount", promoAlerts.size());
        overview.put("promoAlerts", promoAlerts);
        return overview;
    }

    public void validateDateRange(LocalDate tuNgay, LocalDate denNgay) {
        if (tuNgay != null && denNgay != null && denNgay.isBefore(tuNgay)) {
            throw new IllegalArgumentException("Khoảng thời gian không hợp lệ: Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu");
        }
    }
}

