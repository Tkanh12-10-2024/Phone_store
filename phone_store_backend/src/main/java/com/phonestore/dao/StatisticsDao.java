package com.phonestore.dao;

import com.phonestore.dto.response.InventoryStatsResponse;
import com.phonestore.dto.response.ProductStatsResponse;
import com.phonestore.dto.response.RevenueStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class StatisticsDao {

    private final DataSource dataSource;

    /**
     * UC-25: Thống kê doanh thu, tiền vốn, lợi nhuận gộp và % tăng trưởng theo ngày/tháng/năm.
     */
    public RevenueStatsResponse getRevenueStatistics(LocalDate tuNgay, LocalDate denNgay, String groupBy) {
        String normalizedGroup = normalizeGroupBy(groupBy);
        Timestamp startTs = Timestamp.valueOf(tuNgay.atStartOfDay());
        Timestamp endTs = Timestamp.valueOf(denNgay.atTime(LocalTime.MAX));

        RevenueAggregate current = queryRevenueAggregate(startTs, endTs);

        long daysBetween = Math.max(1, ChronoUnit.DAYS.between(tuNgay, denNgay) + 1);
        LocalDate prevEnd = tuNgay.minusDays(1);
        LocalDate prevStart = prevEnd.minusDays(daysBetween - 1);
        RevenueAggregate previous = queryRevenueAggregate(
                Timestamp.valueOf(prevStart.atStartOfDay()),
                Timestamp.valueOf(prevEnd.atTime(LocalTime.MAX))
        );

        BigDecimal growthPct = computeGrowthPercent(current.netRevenue, previous.netRevenue);
        List<RevenueStatsResponse.RevenuePointDTO> series = queryRevenueSeries(startTs, endTs, normalizedGroup);

        String emptyMsg = current.invoiceCount == 0
                ? "Không có doanh thu phát sinh trong khoảng thời gian này"
                : null;

        return RevenueStatsResponse.builder()
                .groupBy(normalizedGroup)
                .tuNgay(tuNgay)
                .denNgay(denNgay)
                .tongHoaDon(current.invoiceCount)
                .tongSanPhamBan(current.itemCount)
                .doanhThuGop(current.grossRevenue)
                .tongChietKhau(current.discount)
                .tongVat(current.vat)
                .doanhThuThucThu(current.netRevenue)
                .tongTienVon(current.cogs)
                .loiNhuanGop(current.netRevenue.subtract(current.cogs))
                .tyLeTangTruongPhanTram(growthPct)
                .doanhThuKyTruoc(previous.netRevenue)
                .emptyMessage(emptyMsg)
                .series(series)
                .build();
    }

    /**
     * UC-26: Thống kê sản phẩm bán chạy & hàng tồn đọng (> 60 ngày chưa xuất).
     */
    public ProductStatsResponse getProductStatistics(LocalDate tuNgay, LocalDate denNgay, Integer maThuongHieu) {
        Timestamp startTs = Timestamp.valueOf(tuNgay.atStartOfDay());
        Timestamp endTs = Timestamp.valueOf(denNgay.atTime(LocalTime.MAX));

        StringBuilder bestSql = new StringBuilder("""
                SELECT sp.ma_san_pham, sp.ten_san_pham, th.ten_thuong_hieu,
                       sp.dong_may, sp.mau_sac, sp.ram, sp.rom, sp.so_luong_ton,
                       COUNT(cthd.ma_chi_tiet_hd) AS so_luong_da_ban,
                       COALESCE(SUM(cthd.don_gia), 0) AS doanh_thu,
                       COALESCE(SUM(sp.gia_nhap_tham_khao), 0) AS tien_von
                FROM san_pham sp
                JOIN thuong_hieu th ON sp.ma_thuong_hieu = th.ma_thuong_hieu
                JOIN chi_tiet_hoa_don cthd ON sp.ma_san_pham = cthd.ma_san_pham
                JOIN hoa_don hd ON cthd.ma_hoa_don = hd.ma_hoa_don
                WHERE hd.trang_thai_thanh_toan = 'Thanh_cong'
                  AND hd.ngay_lap BETWEEN ? AND ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(startTs);
        params.add(endTs);

        if (maThuongHieu != null) {
            bestSql.append(" AND sp.ma_thuong_hieu = ?");
            params.add(maThuongHieu);
        }
        bestSql.append("""
                 GROUP BY sp.ma_san_pham, sp.ten_san_pham, th.ten_thuong_hieu,
                          sp.dong_may, sp.mau_sac, sp.ram, sp.rom, sp.so_luong_ton
                 ORDER BY so_luong_da_ban DESC, doanh_thu DESC
                """);

        List<ProductStatsResponse.ProductSalesStatDTO> bestSellers = new ArrayList<>();
        int totalQty = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(bestSql.toString())) {
            DaoJdbcHelper.bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int qty = rs.getInt("so_luong_da_ban");
                    BigDecimal rev = DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("doanh_thu"));
                    BigDecimal cost = DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("tien_von"));
                    totalQty += qty;
                    totalRevenue = totalRevenue.add(rev);
                    bestSellers.add(ProductStatsResponse.ProductSalesStatDTO.builder()
                            .maSanPham(rs.getString("ma_san_pham"))
                            .tenSanPham(rs.getString("ten_san_pham"))
                            .tenThuongHieu(rs.getString("ten_thuong_hieu"))
                            .dongMay(rs.getString("dong_may"))
                            .mauSac(rs.getString("mau_sac"))
                            .ram(rs.getString("ram"))
                            .rom(rs.getString("rom"))
                            .soLuongDaBan(qty)
                            .doanhThu(rev)
                            .tienVon(cost)
                            .loiNhuan(rev.subtract(cost))
                            .tonKhoHienTai(rs.getInt("so_luong_ton"))
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi thống kê sản phẩm bán chạy", e);
        }

        List<ProductStatsResponse.StagnantProductDTO> stagnant = queryStagnantProducts(startTs, endTs, maThuongHieu);
        String brandName = maThuongHieu == null ? "Tất cả thương hiệu" : findBrandName(maThuongHieu);
        String emptyMsg = bestSellers.isEmpty()
                ? "Chưa có phát sinh giao dịch bán hàng đối với nhóm sản phẩm này"
                : null;

        return ProductStatsResponse.builder()
                .tuNgay(tuNgay)
                .denNgay(denNgay)
                .maThuongHieu(maThuongHieu)
                .tenThuongHieu(brandName)
                .tongSoLuongBan(totalQty)
                .tongDoanhThu(totalRevenue)
                .emptyMessage(emptyMsg)
                .bestSellers(bestSellers)
                .stagnantProducts(stagnant)
                .build();
    }

    /**
     * UC-27: Thống kê tồn kho theo model, dung lượng, màu sắc, giữ chỗ & cảnh báo hết hàng.
     */
    public InventoryStatsResponse getInventoryStatistics(
            Integer maThuongHieu,
            String dongMay,
            String ram,
            String rom,
            String mauSac,
            String keyword,
            boolean onlyAlert
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT sp.ma_san_pham, sp.ten_san_pham, sp.ma_thuong_hieu, th.ten_thuong_hieu,
                       sp.dong_may, sp.ram, sp.rom, sp.mau_sac,
                       sp.so_luong_ton, sp.dinh_muc_ton_toi_thieu,
                       sp.gia_nhap_tham_khao, sp.gia_ban,
                       COALESCE(res.qty_reserved, 0) AS so_luong_giu_cho
                FROM san_pham sp
                JOIN thuong_hieu th ON sp.ma_thuong_hieu = th.ma_thuong_hieu
                LEFT JOIN (
                    SELECT ctdh.ma_san_pham, SUM(ctdh.so_luong) AS qty_reserved
                    FROM chi_tiet_don_hang ctdh
                    JOIN don_hang dh ON ctdh.ma_don_hang = dh.ma_don_hang
                    WHERE dh.trang_thai = 'Cho_thanh_toan'
                    GROUP BY ctdh.ma_san_pham
                ) res ON sp.ma_san_pham = res.ma_san_pham
                WHERE sp.trang_thai = 'Dang_kinh_doanh'
                """);
        List<Object> params = new ArrayList<>();

        if (maThuongHieu != null) {
            sql.append(" AND sp.ma_thuong_hieu = ?");
            params.add(maThuongHieu);
        }
        if (dongMay != null && !dongMay.isBlank()) {
            sql.append(" AND sp.dong_may = ?");
            params.add(dongMay.trim());
        }
        if (ram != null && !ram.isBlank()) {
            sql.append(" AND sp.ram = ?");
            params.add(ram.trim());
        }
        if (rom != null && !rom.isBlank()) {
            sql.append(" AND sp.rom = ?");
            params.add(rom.trim());
        }
        if (mauSac != null && !mauSac.isBlank()) {
            sql.append(" AND sp.mau_sac = ?");
            params.add(mauSac.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(sp.ten_san_pham) LIKE ? OR LOWER(sp.ma_san_pham) LIKE ? OR LOWER(sp.dong_may) LIKE ?)");
            String p = "%" + keyword.trim().toLowerCase() + "%";
            params.add(p);
            params.add(p);
            params.add(p);
        }
        if (onlyAlert) {
            sql.append(" AND sp.so_luong_ton <= sp.dinh_muc_ton_toi_thieu");
        }
        sql.append(" ORDER BY (sp.so_luong_ton - sp.dinh_muc_ton_toi_thieu) ASC, sp.so_luong_ton ASC, sp.ten_san_pham ASC");

        List<InventoryStatsResponse.InventoryStatItemDTO> items = new ArrayList<>();
        List<InventoryStatsResponse.InventoryStatItemDTO> alerts = new ArrayList<>();
        int totalStock = 0;
        int totalReserved = 0;
        int totalAvailable = 0;
        BigDecimal totalValue = BigDecimal.ZERO;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoJdbcHelper.bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int stock = rs.getInt("so_luong_ton");
                    int reserved = rs.getInt("so_luong_giu_cho");
                    int available = Math.max(0, stock - reserved);
                    int minStock = rs.getInt("dinh_muc_ton_toi_thieu");
                    BigDecimal importPrice = DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("gia_nhap_tham_khao"));
                    BigDecimal salePrice = DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("gia_ban"));
                    BigDecimal stockValue = importPrice.multiply(BigDecimal.valueOf(stock));
                    String name = rs.getString("ten_san_pham");

                    String level;
                    String alertMsg = null;
                    if (stock == 0) {
                        level = "Hết hàng";
                        alertMsg = "Thiết bị " + name + " đã hết hàng, cần bổ sung hàng hóa ngay";
                    } else if (stock <= minStock) {
                        level = "Sắp hết hàng";
                        alertMsg = "Thiết bị " + name + " sắp hết hàng, cần bổ sung hàng hóa";
                    } else {
                        level = "Bình thường";
                    }

                    InventoryStatsResponse.InventoryStatItemDTO item = InventoryStatsResponse.InventoryStatItemDTO.builder()
                            .maSanPham(rs.getString("ma_san_pham"))
                            .tenSanPham(name)
                            .maThuongHieu(rs.getInt("ma_thuong_hieu"))
                            .tenThuongHieu(rs.getString("ten_thuong_hieu"))
                            .dongMay(rs.getString("dong_may"))
                            .ram(rs.getString("ram"))
                            .rom(rs.getString("rom"))
                            .mauSac(rs.getString("mau_sac"))
                            .soLuongTon(stock)
                            .soLuongGiuCho(reserved)
                            .soLuongKhaDung(available)
                            .dinhMucTonToiThieu(minStock)
                            .giaNhapThamKhao(importPrice)
                            .giaBan(salePrice)
                            .giaTriTonKho(stockValue)
                            .mucDoCanhBao(level)
                            .canhBaoThongDiep(alertMsg)
                            .build();

                    items.add(item);
                    if (!"Bình thường".equals(level)) {
                        alerts.add(item);
                    }
                    totalStock += stock;
                    totalReserved += reserved;
                    totalAvailable += available;
                    totalValue = totalValue.add(stockValue);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi thống kê tồn kho", e);
        }

        List<InventoryStatsResponse.StockMovementDTO> movements = queryRecentStockMovements(15);

        return InventoryStatsResponse.builder()
                .tongMaSanPham(items.size())
                .tongSoLuongTon(totalStock)
                .tongDangGiuCho(totalReserved)
                .tongKhaDung(totalAvailable)
                .soSanPhamCanhBao(alerts.size())
                .tongGiaTriTonKho(totalValue)
                .items(items)
                .alerts(alerts)
                .movements(movements)
                .build();
    }

    public Map<String, List<String>> getInventoryAttributeOptions() {
        Map<String, List<String>> opts = new LinkedHashMap<>();
        opts.put("dongMay", queryDistinctStrings("SELECT DISTINCT dong_may FROM san_pham WHERE trang_thai = 'Dang_kinh_doanh' ORDER BY dong_may"));
        opts.put("ram", queryDistinctStrings("SELECT DISTINCT ram FROM san_pham WHERE trang_thai = 'Dang_kinh_doanh' ORDER BY ram"));
        opts.put("rom", queryDistinctStrings("SELECT DISTINCT rom FROM san_pham WHERE trang_thai = 'Dang_kinh_doanh' ORDER BY rom"));
        opts.put("mauSac", queryDistinctStrings("SELECT DISTINCT mau_sac FROM san_pham WHERE trang_thai = 'Dang_kinh_doanh' ORDER BY mau_sac"));
        return opts;
    }

    private List<String> queryDistinctStrings(String sql) {
        List<String> values = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String v = rs.getString(1);
                if (v != null && !v.isBlank()) {
                    values.add(v);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi lấy danh sách thuộc tính lọc tồn kho", e);
        }
        return values;
    }

    private RevenueAggregate queryRevenueAggregate(Timestamp startTs, Timestamp endTs) {
        String headerSql = """
                SELECT COUNT(DISTINCT hd.ma_hoa_don) AS so_hoa_don,
                       COALESCE(SUM(hd.tong_tien_hang), 0) AS doanh_thu_gop,
                       COALESCE(SUM(hd.tien_giam_gia), 0) AS tong_chiet_khau,
                       COALESCE(SUM(hd.tien_thue_vat), 0) AS tong_vat,
                       COALESCE(SUM(hd.tong_thanh_toan), 0) AS doanh_thu_thuc_thu
                FROM hoa_don hd
                WHERE hd.trang_thai_thanh_toan = 'Thanh_cong'
                  AND hd.ngay_lap BETWEEN ? AND ?
                """;
        String cogsSql = """
                SELECT COUNT(cthd.ma_chi_tiet_hd) AS so_san_pham,
                       COALESCE(SUM(sp.gia_nhap_tham_khao), 0) AS tong_von
                FROM chi_tiet_hoa_don cthd
                JOIN hoa_don hd ON cthd.ma_hoa_don = hd.ma_hoa_don
                JOIN san_pham sp ON cthd.ma_san_pham = sp.ma_san_pham
                WHERE hd.trang_thai_thanh_toan = 'Thanh_cong'
                  AND hd.ngay_lap BETWEEN ? AND ?
                """;
        try (Connection conn = dataSource.getConnection()) {
            int invoiceCount = 0;
            BigDecimal gross = BigDecimal.ZERO;
            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal vat = BigDecimal.ZERO;
            BigDecimal net = BigDecimal.ZERO;
            int itemCount = 0;
            BigDecimal cogs = BigDecimal.ZERO;

            try (PreparedStatement ps = conn.prepareStatement(headerSql)) {
                ps.setTimestamp(1, startTs);
                ps.setTimestamp(2, endTs);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        invoiceCount = rs.getInt("so_hoa_don");
                        gross = DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("doanh_thu_gop"));
                        discount = DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("tong_chiet_khau"));
                        vat = DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("tong_vat"));
                        net = DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("doanh_thu_thuc_thu"));
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(cogsSql)) {
                ps.setTimestamp(1, startTs);
                ps.setTimestamp(2, endTs);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        itemCount = rs.getInt("so_san_pham");
                        cogs = DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("tong_von"));
                    }
                }
            }
            return new RevenueAggregate(invoiceCount, itemCount, gross, discount, vat, net, cogs);
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tổng hợp số liệu doanh thu", e);
        }
    }

    private List<RevenueStatsResponse.RevenuePointDTO> queryRevenueSeries(Timestamp startTs, Timestamp endTs, String groupBy) {
        String headerSql = switch (groupBy) {
            case "YEAR" -> """
                    SELECT YEAR(hd.ngay_lap) AS yr, 1 AS mo, 1 AS dy,
                           COUNT(DISTINCT hd.ma_hoa_don) AS so_hd,
                           COALESCE(SUM(hd.tong_tien_hang), 0) AS gop,
                           COALESCE(SUM(hd.tien_giam_gia), 0) AS giam,
                           COALESCE(SUM(hd.tong_thanh_toan), 0) AS thuc_thu
                    FROM hoa_don hd
                    WHERE hd.trang_thai_thanh_toan = 'Thanh_cong' AND hd.ngay_lap BETWEEN ? AND ?
                    GROUP BY YEAR(hd.ngay_lap)
                    ORDER BY yr ASC
                    """;
            case "MONTH" -> """
                    SELECT YEAR(hd.ngay_lap) AS yr, MONTH(hd.ngay_lap) AS mo, 1 AS dy,
                           COUNT(DISTINCT hd.ma_hoa_don) AS so_hd,
                           COALESCE(SUM(hd.tong_tien_hang), 0) AS gop,
                           COALESCE(SUM(hd.tien_giam_gia), 0) AS giam,
                           COALESCE(SUM(hd.tong_thanh_toan), 0) AS thuc_thu
                    FROM hoa_don hd
                    WHERE hd.trang_thai_thanh_toan = 'Thanh_cong' AND hd.ngay_lap BETWEEN ? AND ?
                    GROUP BY YEAR(hd.ngay_lap), MONTH(hd.ngay_lap)
                    ORDER BY yr ASC, mo ASC
                    """;
            default -> """
                    SELECT YEAR(hd.ngay_lap) AS yr, MONTH(hd.ngay_lap) AS mo, DAY(hd.ngay_lap) AS dy,
                           COUNT(DISTINCT hd.ma_hoa_don) AS so_hd,
                           COALESCE(SUM(hd.tong_tien_hang), 0) AS gop,
                           COALESCE(SUM(hd.tien_giam_gia), 0) AS giam,
                           COALESCE(SUM(hd.tong_thanh_toan), 0) AS thuc_thu
                    FROM hoa_don hd
                    WHERE hd.trang_thai_thanh_toan = 'Thanh_cong' AND hd.ngay_lap BETWEEN ? AND ?
                    GROUP BY YEAR(hd.ngay_lap), MONTH(hd.ngay_lap), DAY(hd.ngay_lap)
                    ORDER BY yr ASC, mo ASC, dy ASC
                    """;
        };

        String cogsSql = switch (groupBy) {
            case "YEAR" -> """
                    SELECT YEAR(hd.ngay_lap) AS yr, 1 AS mo, 1 AS dy,
                           COALESCE(SUM(sp.gia_nhap_tham_khao), 0) AS tien_von
                    FROM chi_tiet_hoa_don cthd
                    JOIN hoa_don hd ON cthd.ma_hoa_don = hd.ma_hoa_don
                    JOIN san_pham sp ON cthd.ma_san_pham = sp.ma_san_pham
                    WHERE hd.trang_thai_thanh_toan = 'Thanh_cong' AND hd.ngay_lap BETWEEN ? AND ?
                    GROUP BY YEAR(hd.ngay_lap)
                    """;
            case "MONTH" -> """
                    SELECT YEAR(hd.ngay_lap) AS yr, MONTH(hd.ngay_lap) AS mo, 1 AS dy,
                           COALESCE(SUM(sp.gia_nhap_tham_khao), 0) AS tien_von
                    FROM chi_tiet_hoa_don cthd
                    JOIN hoa_don hd ON cthd.ma_hoa_don = hd.ma_hoa_don
                    JOIN san_pham sp ON cthd.ma_san_pham = sp.ma_san_pham
                    WHERE hd.trang_thai_thanh_toan = 'Thanh_cong' AND hd.ngay_lap BETWEEN ? AND ?
                    GROUP BY YEAR(hd.ngay_lap), MONTH(hd.ngay_lap)
                    """;
            default -> """
                    SELECT YEAR(hd.ngay_lap) AS yr, MONTH(hd.ngay_lap) AS mo, DAY(hd.ngay_lap) AS dy,
                           COALESCE(SUM(sp.gia_nhap_tham_khao), 0) AS tien_von
                    FROM chi_tiet_hoa_don cthd
                    JOIN hoa_don hd ON cthd.ma_hoa_don = hd.ma_hoa_don
                    JOIN san_pham sp ON cthd.ma_san_pham = sp.ma_san_pham
                    WHERE hd.trang_thai_thanh_toan = 'Thanh_cong' AND hd.ngay_lap BETWEEN ? AND ?
                    GROUP BY YEAR(hd.ngay_lap), MONTH(hd.ngay_lap), DAY(hd.ngay_lap)
                    """;
        };

        Map<String, BigDecimal> cogsByLabel = new LinkedHashMap<>();
        List<RevenueStatsResponse.RevenuePointDTO> points = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(cogsSql)) {
                ps.setTimestamp(1, startTs);
                ps.setTimestamp(2, endTs);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String label = formatPeriodLabel(groupBy, rs.getInt("yr"), rs.getInt("mo"), rs.getInt("dy"));
                        cogsByLabel.put(label, DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("tien_von")));
                    }
                }
            }

            BigDecimal prevNet = null;
            try (PreparedStatement ps = conn.prepareStatement(headerSql)) {
                ps.setTimestamp(1, startTs);
                ps.setTimestamp(2, endTs);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String label = formatPeriodLabel(groupBy, rs.getInt("yr"), rs.getInt("mo"), rs.getInt("dy"));
                        BigDecimal net = DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("thuc_thu"));
                        BigDecimal cogs = cogsByLabel.getOrDefault(label, BigDecimal.ZERO);
                        BigDecimal stepGrowth = prevNet == null ? BigDecimal.ZERO : computeGrowthPercent(net, prevNet);
                        points.add(RevenueStatsResponse.RevenuePointDTO.builder()
                                .nhanThoiGian(label)
                                .soHoaDon(rs.getInt("so_hd"))
                                .doanhThuGop(DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("gop")))
                                .chietKhau(DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("giam")))
                                .doanhThuThucThu(net)
                                .tienVon(cogs)
                                .loiNhuanGop(net.subtract(cogs))
                                .tangTruongPhanTram(stepGrowth)
                                .build());
                        prevNet = net;
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tạo chuỗi biểu đồ doanh thu", e);
        }
        return points;
    }

    private List<ProductStatsResponse.StagnantProductDTO> queryStagnantProducts(Timestamp startTs, Timestamp endTs, Integer maThuongHieu) {
        StringBuilder sql = new StringBuilder("""
                SELECT sp.ma_san_pham, sp.ten_san_pham, th.ten_thuong_hieu, sp.dong_may,
                       sp.so_luong_ton, sp.gia_nhap_tham_khao,
                       COALESCE(MAX(im.ngay_nhap), sp.ngay_tao) AS ngay_nhap_gan_nhat,
                       COALESCE(sold.qty_period, 0) AS so_luong_ban_ky
                FROM san_pham sp
                JOIN thuong_hieu th ON sp.ma_thuong_hieu = th.ma_thuong_hieu
                LEFT JOIN imei_san_pham im ON sp.ma_san_pham = im.ma_san_pham AND im.trang_thai = 'Trong_kho'
                LEFT JOIN (
                    SELECT cthd.ma_san_pham, COUNT(cthd.ma_chi_tiet_hd) AS qty_period
                    FROM chi_tiet_hoa_don cthd
                    JOIN hoa_don hd ON cthd.ma_hoa_don = hd.ma_hoa_don
                    WHERE hd.trang_thai_thanh_toan = 'Thanh_cong' AND hd.ngay_lap BETWEEN ? AND ?
                    GROUP BY cthd.ma_san_pham
                ) sold ON sp.ma_san_pham = sold.ma_san_pham
                WHERE sp.trang_thai = 'Dang_kinh_doanh' AND sp.so_luong_ton > 0
                """);
        List<Object> params = new ArrayList<>();
        params.add(startTs);
        params.add(endTs);
        if (maThuongHieu != null) {
            sql.append(" AND sp.ma_thuong_hieu = ?");
            params.add(maThuongHieu);
        }
        sql.append(" GROUP BY sp.ma_san_pham, sp.ten_san_pham, th.ten_thuong_hieu, sp.dong_may, sp.so_luong_ton, sp.gia_nhap_tham_khao, sp.ngay_tao, sold.qty_period");

        List<ProductStatsResponse.StagnantProductDTO> stagnant = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoJdbcHelper.bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime importDate = DaoJdbcHelper.toLocalDateTime(rs.getTimestamp("ngay_nhap_gan_nhat"));
                    long daysInStock = importDate == null ? 0L : Math.max(0L, ChronoUnit.DAYS.between(importDate, now));
                    int soldQty = rs.getInt("so_luong_ban_ky");
                    if (daysInStock > 60 || soldQty == 0) {
                        int stock = rs.getInt("so_luong_ton");
                        BigDecimal importCost = DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("gia_nhap_tham_khao"));
                        stagnant.add(ProductStatsResponse.StagnantProductDTO.builder()
                                .maSanPham(rs.getString("ma_san_pham"))
                                .tenSanPham(rs.getString("ten_san_pham"))
                                .tenThuongHieu(rs.getString("ten_thuong_hieu"))
                                .dongMay(rs.getString("dong_may"))
                                .soLuongTon(stock)
                                .soNgayTonKho(daysInStock)
                                .ngayNhapGanNhat(importDate)
                                .giaTriTonKho(importCost.multiply(BigDecimal.valueOf(stock)))
                                .soLuongBanTrongKy(soldQty)
                                .build());
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi thống kê hàng tồn kho lâu", e);
        }
        stagnant.sort((a, b) -> Long.compare(b.getSoNgayTonKho(), a.getSoNgayTonKho()));
        return stagnant;
    }

    private List<InventoryStatsResponse.StockMovementDTO> queryRecentStockMovements(int limit) {
        String sql = """
                SELECT * FROM (
                    SELECT pn.ngay_nhap AS thoi_gian, 'NHAP_KHO' AS loai_bien_dong, pn.ma_phieu_nhap AS ma_chung_tu,
                           sp.ma_san_pham, sp.ten_san_pham, ctpn.so_luong AS thay_doi,
                           ctpn.don_gia_nhap AS don_gia, COALESCE(nv.ho_ten, pn.ma_nhan_vien) AS nhan_vien,
                           COALESCE(pn.ghi_chu, 'Nhập kho từ NCC') AS ghi_chu
                    FROM chi_tiet_phieu_nhap ctpn
                    JOIN phieu_nhap pn ON ctpn.ma_phieu_nhap = pn.ma_phieu_nhap
                    JOIN san_pham sp ON ctpn.ma_san_pham = sp.ma_san_pham
                    LEFT JOIN nhan_vien nv ON pn.ma_nhan_vien = nv.ma_nhan_vien
                    UNION ALL
                    SELECT hd.ngay_lap AS thoi_gian, 'XUAT_BAN' AS loai_bien_dong, hd.ma_hoa_don AS ma_chung_tu,
                           sp.ma_san_pham, sp.ten_san_pham, -COUNT(cthd.ma_chi_tiet_hd) AS thay_doi,
                           AVG(cthd.don_gia) AS don_gia, COALESCE(nv.ho_ten, hd.ma_nhan_vien) AS nhan_vien,
                           COALESCE(hd.ghi_chu, 'Xuất bán tại POS') AS ghi_chu
                    FROM chi_tiet_hoa_don cthd
                    JOIN hoa_don hd ON cthd.ma_hoa_don = hd.ma_hoa_don
                    JOIN san_pham sp ON cthd.ma_san_pham = sp.ma_san_pham
                    LEFT JOIN nhan_vien nv ON hd.ma_nhan_vien = nv.ma_nhan_vien
                    GROUP BY hd.ma_hoa_don, hd.ngay_lap, sp.ma_san_pham, sp.ten_san_pham, nv.ho_ten, hd.ma_nhan_vien, hd.ghi_chu
                ) AS movements
                ORDER BY thoi_gian DESC
                LIMIT ?
                """;
        List<InventoryStatsResponse.StockMovementDTO> movements = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movements.add(InventoryStatsResponse.StockMovementDTO.builder()
                            .thoiGian(DaoJdbcHelper.toLocalDateTime(rs.getTimestamp("thoi_gian")))
                            .loaiBienDong(rs.getString("loai_bien_dong"))
                            .maChungTu(rs.getString("ma_chung_tu"))
                            .maSanPham(rs.getString("ma_san_pham"))
                            .tenSanPham(rs.getString("ten_san_pham"))
                            .thayDoiSoLuong(rs.getInt("thay_doi"))
                            .donGia(DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("don_gia")))
                            .nhanVienThucHien(rs.getString("nhan_vien"))
                            .ghiChu(rs.getString("ghi_chu"))
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tải lịch sử biến động tồn kho", e);
        }
        return movements;
    }

    private String findBrandName(Integer maThuongHieu) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT ten_thuong_hieu FROM thuong_hieu WHERE ma_thuong_hieu = ?")) {
            ps.setInt(1, maThuongHieu);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "Thương hiệu #" + maThuongHieu;
            }
        } catch (SQLException e) {
            return "Thương hiệu #" + maThuongHieu;
        }
    }

    private String normalizeGroupBy(String groupBy) {
        if (groupBy == null) return "DAY";
        return switch (groupBy.trim().toUpperCase()) {
            case "MONTH", "THANG" -> "MONTH";
            case "YEAR", "NAM" -> "YEAR";
            default -> "DAY";
        };
    }

    private String formatPeriodLabel(String groupBy, int year, int month, int day) {
        return switch (groupBy) {
            case "YEAR" -> String.valueOf(year);
            case "MONTH" -> String.format("%02d/%d", month, year);
            default -> String.format("%02d/%02d/%d", day, month, year);
        };
    }

    private BigDecimal computeGrowthPercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private record RevenueAggregate(
            int invoiceCount,
            int itemCount,
            BigDecimal grossRevenue,
            BigDecimal discount,
            BigDecimal vat,
            BigDecimal netRevenue,
            BigDecimal cogs
    ) {
    }
}
