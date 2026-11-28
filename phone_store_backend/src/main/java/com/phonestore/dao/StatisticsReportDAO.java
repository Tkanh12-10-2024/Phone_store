package com.phonestore.dao;

import com.phonestore.dto.ReportRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Repository
public class StatisticsReportDAO {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final JdbcTemplate jdbc;

    public StatisticsReportDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> revenue(LocalDate from, LocalDate to, String groupBy) {
        validateDates(from, to);
        String grouping = groupBy == null || groupBy.isBlank() ? "MONTH" : groupBy.toUpperCase(Locale.ROOT);
        if (!List.of("DAY", "MONTH", "YEAR").contains(grouping)) {
            throw new IllegalArgumentException("groupBy phải là DAY, MONTH hoặc YEAR");
        }

        List<Map<String, Object>> invoiceRows = invoiceTotals(from, to);
        Map<String, RevenueBucket> buckets = new LinkedHashMap<>();
        BigDecimal gross = ZERO, discount = ZERO, net = ZERO;
        int invoiceCount = 0;
        for (Map<String, Object> row : invoiceRows) {
            LocalDateTime at = timestamp(row.get("ngay_lap"));
            RevenueBucket bucket = buckets.computeIfAbsent(periodKey(at.toLocalDate(), grouping),
                    key -> new RevenueBucket(periodLabel(at.toLocalDate(), grouping)));
            BigDecimal invoiceGross = money(row.get("tong_tien_hang"));
            BigDecimal invoiceDiscount = money(row.get("tien_giam_gia"));
            BigDecimal invoiceNet = money(row.get("tong_thanh_toan"));
            BigDecimal beforeTaxNet = invoiceGross.subtract(invoiceDiscount);
            bucket.gross = bucket.gross.add(invoiceGross);
            bucket.discount = bucket.discount.add(invoiceDiscount);
            bucket.net = bucket.net.add(invoiceNet);
            bucket.profitRevenue = bucket.profitRevenue.add(beforeTaxNet);
            bucket.invoiceCount++;
            gross = gross.add(invoiceGross);
            discount = discount.add(invoiceDiscount);
            net = net.add(invoiceNet);
            invoiceCount++;
        }

        Map<String, BigDecimal> costs = soldCostByPeriod(from, to, grouping);
        List<RevenueBucket> ordered = new ArrayList<>(buckets.values());
        ordered.sort(Comparator.comparing(b -> periodSortValue(b.label, grouping)));
        List<Map<String, Object>> series = new ArrayList<>();
        BigDecimal allCost = ZERO;
        BigDecimal priorNet = ZERO;
        for (RevenueBucket bucket : ordered) {
            bucket.cost = costs.getOrDefault(bucket.label, ZERO);
            bucket.profit = bucket.profitRevenue.subtract(bucket.cost);
            allCost = allCost.add(bucket.cost);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("nhanThoiGian", bucket.label);
            point.put("soHoaDon", bucket.invoiceCount);
            point.put("doanhThuGop", bucket.gross);
            point.put("chietKhau", bucket.discount);
            point.put("doanhThuThucThu", bucket.net);
            point.put("tienVon", bucket.cost);
            point.put("loiNhuanGop", bucket.profit);
            point.put("tangTruongPhanTram", percentageChange(bucket.net, priorNet));
            series.add(point);
            priorNet = bucket.net;
        }

        long days = from != null && to != null ? ChronoUnit.DAYS.between(from, to) + 1 : 0;
        BigDecimal previousNet = ZERO;
        if (from != null && to != null && days > 0) {
            LocalDate previousTo = from.minusDays(1);
            previousNet = invoiceNet(from.minusDays(days), previousTo);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tongHoaDon", invoiceCount);
        result.put("tongSanPhamBan", soldUnitCount(from, to));
        result.put("doanhThuGop", gross);
        result.put("tongChietKhau", discount);
        result.put("doanhThuThucThu", net);
        result.put("tongTienVon", allCost);
        result.put("loiNhuanGop", ordered.stream().map(bucket -> bucket.profit)
                .reduce(ZERO, BigDecimal::add));
        result.put("tyLeTangTruongPhanTram", percentageChange(net, previousNet));
        result.put("series", series);
        if (invoiceCount == 0) {
            result.put("emptyMessage", "Không có doanh thu phát sinh trong khoảng thời gian này");
        }
        return result;
    }

    public Map<String, Object> products(LocalDate from, LocalDate to, String brandId) {
        validateDates(from, to);
        List<Map<String, Object>> rows = soldItems(from, to, brandId);
        Map<String, ProductSales> sales = new LinkedHashMap<>();
        BigDecimal totalRevenue = ZERO;
        int totalUnits = 0;
        for (Map<String, Object> row : rows) {
            String id = (String) row.get("ma_san_pham");
            ProductSales item = sales.computeIfAbsent(id, key -> new ProductSales(row));
            BigDecimal price = money(row.get("don_gia"));
            BigDecimal cost = money(row.get("don_gia_nhap"));
            BigDecimal invoiceGross = money(row.get("hoa_don_tong_tien_hang"));
            BigDecimal allocatedDiscount = invoiceGross.signum() == 0 ? ZERO
                    : money(row.get("hoa_don_tien_giam_gia")).multiply(price)
                    .divide(invoiceGross, 2, RoundingMode.HALF_UP);
            item.units++;
            item.revenue = item.revenue.add(price.subtract(allocatedDiscount));
            item.cost = item.cost.add(cost);
            totalRevenue = totalRevenue.add(price.subtract(allocatedDiscount));
            totalUnits++;
        }

        Map<String, Map<String, Object>> stock = inventorySnapshots();
        List<Map<String, Object>> bestSellers = new ArrayList<>();
        for (ProductSales item : sales.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("maSanPham", item.id);
            row.put("tenSanPham", item.name);
            row.put("tenThuongHieu", item.brand);
            row.put("ram", item.ram);
            row.put("rom", item.rom);
            row.put("soLuongDaBan", item.units);
            row.put("doanhThu", item.revenue);
            row.put("loiNhuan", item.revenue.subtract(item.cost));
            row.put("tonKhoHienTai", stock.containsKey(item.id) ? stock.get(item.id).get("soLuongTon") : 0);
            bestSellers.add(row);
        }
        bestSellers.sort(Comparator.<Map<String, Object>>comparingInt(m -> (Integer) m.get("soLuongDaBan"))
                .reversed().thenComparing(m -> String.valueOf(m.get("tenSanPham"))));

        List<Map<String, Object>> stagnant = new ArrayList<>();
        for (Map<String, Object> snapshot : stock.values()) {
            int onHand = (Integer) snapshot.get("soLuongTon");
            if (onHand <= 0 || (brandId != null && !brandId.isBlank()
                    && !Objects.equals(String.valueOf(snapshot.get("maThuongHieu")), brandId))) {
                continue;
            }
            LocalDateTime oldest = (LocalDateTime) snapshot.get("ngayNhapCuNhat");
            int days = oldest == null ? 0 : (int) Math.max(0, ChronoUnit.DAYS.between(oldest.toLocalDate(), LocalDate.now()));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("maSanPham", snapshot.get("maSanPham"));
            row.put("tenSanPham", snapshot.get("tenSanPham"));
            row.put("tenThuongHieu", snapshot.get("tenThuongHieu"));
            row.put("dongMay", snapshot.get("dongMay"));
            row.put("soLuongTon", onHand);
            row.put("soNgayTonKho", days);
            row.put("soLuongBanTrongKy", sales.containsKey(snapshot.get("maSanPham"))
                    ? sales.get(snapshot.get("maSanPham")).units : 0);
            row.put("giaTriTonKho", snapshot.get("giaTriTonKho"));
            stagnant.add(row);
        }
        stagnant.sort(Comparator.<Map<String, Object>>comparingInt(m -> (Integer) m.get("soNgayTonKho"))
                .reversed().thenComparing(m -> String.valueOf(m.get("tenSanPham"))));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tongSoLuongBan", totalUnits);
        result.put("tongDoanhThu", totalRevenue);
        result.put("bestSellers", bestSellers);
        result.put("stagnantProducts", stagnant);
        if (totalUnits == 0) {
            result.put("emptyMessage", "Chưa có phát sinh giao dịch bán hàng trong khoảng thời gian này");
        }
        return result;
    }

    public Map<String, Object> inventory(String brandId, String line, String ram, String rom, String color,
                                         String keyword, boolean onlyAlert) {
        List<Map<String, Object>> items = inventoryItems(brandId, line, ram, rom, color, keyword);
        List<Map<String, Object>> filtered = onlyAlert
                ? items.stream().filter(item -> !"Bình thường".equals(item.get("mucDoCanhBao"))).toList()
                : items;
        int stock = 0, reserved = 0, alerts = 0;
        BigDecimal value = ZERO;
        for (Map<String, Object> item : filtered) {
            stock += (Integer) item.get("soLuongTon");
            reserved += (Integer) item.get("soLuongGiuCho");
            value = value.add((BigDecimal) item.get("giaTriTonKho"));
            if (!"Bình thường".equals(item.get("mucDoCanhBao"))) alerts++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tongMaSanPham", filtered.size());
        result.put("tongSoLuongTon", stock);
        result.put("tongDangGiuCho", reserved);
        result.put("tongKhaDung", Math.max(0, stock - reserved));
        result.put("tongGiaTriTonKho", value);
        result.put("soSanPhamCanhBao", alerts);
        result.put("alerts", filtered.stream().filter(item -> !"Bình thường".equals(item.get("mucDoCanhBao")))
                .map(item -> {
                    Map<String, Object> alert = new LinkedHashMap<>();
                    alert.put("maSanPham", item.get("maSanPham"));
                    alert.put("tenSanPham", item.get("tenSanPham"));
                    alert.put("soLuongTon", item.get("soLuongTon"));
                    alert.put("dinhMucTonToiThieu", item.get("dinhMucTonToiThieu"));
                    alert.put("canhBaoThongDiep", item.get("mucDoCanhBao"));
                    return alert;
                }).toList());
        result.put("items", filtered);
        result.put("movements", inventoryMovements());
        return result;
    }

    public Map<String, Object> inventoryFilters() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("brands", jdbc.query("""
                SELECT ma_thuong_hieu AS brand_value, ten_thuong_hieu AS brand_label
                FROM thuong_hieu ORDER BY ten_thuong_hieu
                """, (rs, row) -> Map.of("value", rs.getString("brand_value"), "label", rs.getString("brand_label"))));
        result.put("dongMay", distinctProducts("dong_may"));
        result.put("ram", distinctProducts("ram"));
        result.put("rom", distinctProducts("rom"));
        result.put("mauSac", distinctProducts("mau_sac"));
        return result;
    }

    public Map<String, Object> preview(ReportRequest request) {
        validateRequest(request);
        String reportType = normalizeType(request.loaiBaoCao());
        LocalDate from = request.tuNgay(), to = request.denNgay();
        String brand = request.maThuongHieu() == null ? null : request.maThuongHieu().toString();
        String employee = request.maNhanVien() == null || request.maNhanVien().isBlank() ? "NV001" : request.maNhanVien();
        String employeeName = jdbc.query("""
                SELECT ho_ten FROM nhan_vien WHERE ma_nhan_vien = ?
                """, rs -> rs.next() ? rs.getString(1) : null, employee);
        if (employeeName == null) throw new IllegalArgumentException("Không tìm thấy nhân viên lập báo cáo");

        List<String> columns;
        List<List<String>> rows = new ArrayList<>();
        Map<String, String> summary = new LinkedHashMap<>();
        String title;
        if ("Doanh_thu".equals(reportType)) {
            Map<String, Object> revenue = revenue(from, to, "DAY");
            columns = List.of("Thời gian", "Số hóa đơn", "Doanh thu gộp", "Chiết khấu", "Thực thu", "Tiền vốn", "Lợi nhuận");
            for (Map<String, Object> point : (List<Map<String, Object>>) revenue.get("series")) {
                rows.add(List.of(text(point.get("nhanThoiGian")), text(point.get("soHoaDon")),
                        text(point.get("doanhThuGop")), text(point.get("chietKhau")),
                        text(point.get("doanhThuThucThu")), text(point.get("tienVon")), text(point.get("loiNhuanGop"))));
            }
            summary.put("Số hóa đơn", text(revenue.get("tongHoaDon")));
            summary.put("Doanh thu thực thu", text(revenue.get("doanhThuThucThu")) + " đ");
            summary.put("Tiền vốn", text(revenue.get("tongTienVon")) + " đ");
            summary.put("Lợi nhuận gộp", text(revenue.get("loiNhuanGop")) + " đ");
            title = "Báo cáo Doanh thu & Lợi nhuận";
        } else if ("SanPham_BanChay".equals(reportType)) {
            Map<String, Object> products = products(from, to, brand);
            columns = List.of("Mã sản phẩm", "Tên sản phẩm", "Thương hiệu", "RAM/ROM", "Đã bán", "Doanh thu", "Lợi nhuận", "Tồn kho");
            for (Map<String, Object> product : (List<Map<String, Object>>) products.get("bestSellers")) {
                rows.add(List.of(text(product.get("maSanPham")), text(product.get("tenSanPham")), text(product.get("tenThuongHieu")),
                        text(product.get("ram")) + "/" + text(product.get("rom")), text(product.get("soLuongDaBan")),
                        text(product.get("doanhThu")), text(product.get("loiNhuan")), text(product.get("tonKhoHienTai"))));
            }
            summary.put("Tổng số lượng bán", text(products.get("tongSoLuongBan")));
            summary.put("Tổng doanh thu", text(products.get("tongDoanhThu")) + " đ");
            title = "Báo cáo Sản phẩm bán chạy";
        } else if ("Bao_hanh".equals(reportType)) {
            columns = List.of("Mã phiếu", "Sản phẩm", "Khách hàng", "Ngày tiếp nhận", "Trạng thái", "Nhân viên", "Chi phí");
            List<Object> params = new ArrayList<>();
            String dateSql = dateWhere("pb.ngay_tiep_nhan", from, to, params);
            List<Map<String, Object>> tickets = jdbc.query("""
                    SELECT pb.ma_phieu_bao_hanh, sp.ten_san_pham, kh.ho_ten,
                           pb.ngay_tiep_nhan, pb.trang_thai, nv.ho_ten AS ten_nv, pb.chi_phi_sua_chua
                    FROM phieu_bao_hanh pb
                    JOIN san_pham sp ON sp.ma_san_pham = pb.ma_san_pham
                    LEFT JOIN khach_hang kh ON kh.ma_khach_hang = pb.ma_khach_hang
                    JOIN nhan_vien nv ON nv.ma_nhan_vien = pb.ma_nhan_vien_tiep_nhan
                    WHERE 1=1
                    """ + dateSql + (brand == null ? "" : " AND sp.ma_thuong_hieu = ?") + " ORDER BY pb.ngay_tiep_nhan DESC",
                    (rs, row) -> {
                        Map<String, Object> data = new LinkedHashMap<>();
                        data.put("id", rs.getString(1)); data.put("product", rs.getString(2));
                        data.put("customer", rs.getString(3)); data.put("date", rs.getTimestamp(4));
                        data.put("status", rs.getString(5)); data.put("staff", rs.getString(6));
                        data.put("cost", rs.getBigDecimal(7)); return data;
                    }, reportParams(params, brand));
            for (Map<String, Object> ticket : tickets) {
                rows.add(List.of(text(ticket.get("id")), text(ticket.get("product")), text(ticket.get("customer")),
                        text(ticket.get("date")), text(ticket.get("status")), text(ticket.get("staff")), text(ticket.get("cost"))));
            }
            summary.put("Số phiếu bảo hành", Integer.toString(tickets.size()));
            summary.put("Tổng chi phí sửa chữa", tickets.stream().map(t -> money(t.get("cost")))
                    .reduce(ZERO, BigDecimal::add) + " đ");
            title = "Báo cáo Tình hình Bảo hành";
        } else {
            List<Map<String, Object>> inventory = inventoryItems(brand, null, null, null, null, null);
            if ("Kiem_ke_kho".equals(reportType)) {
                columns = List.of("Mã sản phẩm", "Tên sản phẩm", "Thương hiệu", "Dòng máy", "Tồn kho", "Giữ chỗ", "Khả dụng", "Giá trị tồn");
                for (Map<String, Object> item : inventory) {
                    rows.add(List.of(text(item.get("maSanPham")), text(item.get("tenSanPham")), text(item.get("tenThuongHieu")),
                            text(item.get("dongMay")), text(item.get("soLuongTon")), text(item.get("soLuongGiuCho")),
                            text(item.get("soLuongKhaDung")), text(item.get("giaTriTonKho"))));
                }
                summary.put("Số mã sản phẩm", Integer.toString(inventory.size()));
                summary.put("Tổng tồn kho", Integer.toString(sumInt(inventory, "soLuongTon")));
                summary.put("Giá trị tồn kho", sumMoney(inventory, "giaTriTonKho") + " đ");
                title = "Báo cáo Kiểm kê & Tồn kho";
            } else {
                Map<String, Object> products = products(from, to, brand);
                Map<String, Map<String, Object>> sold = new LinkedHashMap<>();
                for (Map<String, Object> product : (List<Map<String, Object>>) products.get("bestSellers")) {
                    sold.put(String.valueOf(product.get("maSanPham")), product);
                }
                columns = List.of("Mã sản phẩm", "Tên sản phẩm", "Thương hiệu", "Đã bán", "Doanh thu", "Lợi nhuận", "Tồn kho", "Giá trị tồn");
                for (Map<String, Object> item : inventory) {
                    Map<String, Object> salesRow = sold.get(String.valueOf(item.get("maSanPham")));
                    rows.add(List.of(text(item.get("maSanPham")), text(item.get("tenSanPham")), text(item.get("tenThuongHieu")),
                            salesRow == null ? "0" : text(salesRow.get("soLuongDaBan")),
                            salesRow == null ? "0" : text(salesRow.get("doanhThu")),
                            salesRow == null ? "0" : text(salesRow.get("loiNhuan")),
                            text(item.get("soLuongTon")), text(item.get("giaTriTonKho"))));
                }
                summary.put("Tổng số lượng bán", text(products.get("tongSoLuongBan")));
                summary.put("Tổng doanh thu", text(products.get("tongDoanhThu")) + " đ");
                summary.put("Tổng tồn kho", Integer.toString(sumInt(inventory, "soLuongTon")));
                summary.put("Giá trị tồn kho", sumMoney(inventory, "giaTriTonKho") + " đ");
                title = "Báo cáo Tổng hợp";
            }
        }
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("loaiBaoCao", reportType);
        preview.put("tieuDeBaoCao", title);
        preview.put("phamViLoc", filterLabel(from, to, brand));
        preview.put("nguoiLap", employeeName);
        preview.put("ngayLap", LocalDateTime.now());
        preview.put("columns", columns);
        preview.put("rows", rows);
        preview.put("summary", summary);
        return preview;
    }

    @Transactional
    public void logExport(ReportRequest request, String format, String filename, Map<String, Object> preview) {
        String originalType = String.valueOf(preview.get("loaiBaoCao"));
        String dbType = (supportsReportType(originalType)
                || ("Tong_hop".equals(originalType) && hasColumn("tieu_chi_loc"))) ? originalType
                : List.of("Doanh_thu", "Kiem_ke_kho", "SanPham_BanChay", "Bao_hanh").contains(originalType)
                ? originalType : "Doanh_thu";
        String employee = request.maNhanVien() == null || request.maNhanVien().isBlank() ? "NV001" : request.maNhanVien();
        String criteria = String.valueOf(preview.get("phamViLoc"));
        String note = request.ghiChu() == null || request.ghiChu().isBlank() ? null : request.ghiChu().trim();
        if (hasColumn("tieu_chi_loc")) {
            if (!dbType.equals(originalType)) note = "Loại: " + originalType + (note == null ? "" : " | " + note);
            jdbc.update("""
                    INSERT INTO nhat_ky_bao_cao
                        (loai_bao_cao, ma_nhan_vien_lap, ngay_lap, tu_ngay, den_ngay,
                         dinh_dang_xuat, duong_dan_file, tieu_chi_loc, ghi_chu)
                    VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?)
                    """, dbType, employee, request.tuNgay(), request.denNgay(), format, filename, criteria, note);
        } else {
            String legacyCriteria = "Loại: " + originalType + " | " + criteria;
            if (note != null) legacyCriteria += " | Ghi chú: " + note;
            if (legacyCriteria.length() > 255) legacyCriteria = legacyCriteria.substring(0, 252) + "...";
            jdbc.update("""
                    INSERT INTO nhat_ky_bao_cao
                        (loai_bao_cao, ma_nhan_vien_lap, ngay_lap, tu_ngay, den_ngay, dinh_dang_xuat, duong_dan_file, ghi_chu)
                    VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?)
                    """, dbType, employee, request.tuNgay(), request.denNgay(), format, filename, legacyCriteria);
        }
    }

    public List<Map<String, Object>> history(int requestedLimit) {
        int limit = Math.max(1, Math.min(100, requestedLimit));
        String criteriaColumn = hasColumn("tieu_chi_loc") ? "bc.tieu_chi_loc" : "bc.ghi_chu";
        String sql = """
                SELECT bc.ma_bao_cao, bc.loai_bao_cao, bc.dinh_dang_xuat, nv.ho_ten AS ten_nhan_vien,
                       bc.ghi_chu, 
                """ + criteriaColumn + """
                       AS tieu_chi_loc,
                       bc.duong_dan_file, bc.ngay_lap
                FROM nhat_ky_bao_cao bc
                JOIN nhan_vien nv ON nv.ma_nhan_vien = bc.ma_nhan_vien_lap
                ORDER BY bc.ngay_lap DESC, bc.ma_bao_cao DESC
                LIMIT ?
                """;
        return jdbc.query(sql, (rs, row) -> {
            String details = rs.getString("ghi_chu");
            String filter = rs.getString("tieu_chi_loc");
            String type = rs.getString("loai_bao_cao");
            if (details != null && details.startsWith("Loại: ")) {
                int delimiter = details.indexOf(" | ");
                if (delimiter > 6) type = details.substring(6, delimiter);
            }
            Map<String, Object> log = new LinkedHashMap<>();
            log.put("maBaoCao", rs.getInt("ma_bao_cao"));
            log.put("loaiBaoCao", type);
            log.put("dinhDangXuat", rs.getString("dinh_dang_xuat"));
            log.put("tenNhanVienLap", rs.getString("ten_nhan_vien"));
            log.put("tieuChiLoc", filter == null || filter.isBlank() ? details : filter);
            log.put("duongDanFile", rs.getString("duong_dan_file"));
            log.put("ngayLap", rs.getTimestamp("ngay_lap"));
            return log;
        }, limit);
    }

    private boolean hasColumn(String columnName) {
        try (Connection connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, "nhat_ky_bao_cao", columnName)) {
                if (columns.next()) return true;
            }
            try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null,
                    "NHAT_KY_BAO_CAO", columnName.toUpperCase(Locale.ROOT))) {
                return columns.next();
            }
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean supportsReportType(String reportType) {
        try (Connection connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            try (ResultSet columns = connection.getMetaData().getColumns(
                    connection.getCatalog(), null, "nhat_ky_bao_cao", "loai_bao_cao")) {
                if (columns.next()) {
                    String type = columns.getString("TYPE_NAME");
                    return type == null || !type.toLowerCase(Locale.ROOT).startsWith("enum")
                            || type.contains("'" + reportType + "'");
                }
            }
        } catch (Exception ignored) {
            // Fall back to the portable report types when schema metadata is unavailable.
        }
        return List.of("Doanh_thu", "Kiem_ke_kho", "SanPham_BanChay", "Bao_hanh").contains(reportType);
    }

    private List<Map<String, Object>> inventoryItems(String brand, String line, String ram, String rom,
                                                      String color, String keyword) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        addFilter(where, args, "sp.ma_thuong_hieu", brand);
        addFilter(where, args, "sp.dong_may", line);
        addFilter(where, args, "sp.ram", ram);
        addFilter(where, args, "sp.rom", rom);
        addFilter(where, args, "sp.mau_sac", color);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (LOWER(sp.ten_san_pham) LIKE ? OR LOWER(sp.ma_san_pham) LIKE ?) ");
            args.add("%" + keyword.trim().toLowerCase(Locale.ROOT) + "%");
            args.add("%" + keyword.trim().toLowerCase(Locale.ROOT) + "%");
        }
        return jdbc.query("""
                SELECT sp.ma_san_pham, sp.ten_san_pham, sp.ma_thuong_hieu, th.ten_thuong_hieu,
                       sp.dong_may, sp.ram, sp.rom, sp.mau_sac, sp.dinh_muc_ton_toi_thieu,
                       COALESCE(stock.so_luong, 0) AS so_luong_ton,
                       COALESCE(reserved.so_luong, 0) AS so_luong_giu_cho,
                       COALESCE(stock.gia_tri, 0) AS gia_tri_ton_kho,
                       stock.ngay_nhap_cu_nhat
                FROM san_pham sp
                LEFT JOIN thuong_hieu th ON th.ma_thuong_hieu = sp.ma_thuong_hieu
                LEFT JOIN (
                    SELECT i.ma_san_pham, COUNT(*) AS so_luong,
                           SUM(COALESCE(ctpn.don_gia_nhap, sp2.gia_nhap_tham_khao)) AS gia_tri,
                           MIN(i.ngay_nhap) AS ngay_nhap_cu_nhat
                    FROM imei_san_pham i
                    JOIN san_pham sp2 ON sp2.ma_san_pham = i.ma_san_pham
                    LEFT JOIN chi_tiet_phieu_nhap ctpn
                      ON ctpn.ma_phieu_nhap = i.ma_phieu_nhap AND ctpn.ma_san_pham = i.ma_san_pham
                    WHERE i.trang_thai = 'Trong_kho'
                    GROUP BY i.ma_san_pham
                ) stock ON stock.ma_san_pham = sp.ma_san_pham
                LEFT JOIN (
                    SELECT ctdh.ma_san_pham, SUM(ctdh.so_luong) AS so_luong
                    FROM chi_tiet_don_hang ctdh
                    JOIN don_hang dh ON dh.ma_don_hang = ctdh.ma_don_hang
                    WHERE dh.trang_thai = 'Cho_thanh_toan'
                    GROUP BY ctdh.ma_san_pham
                ) reserved ON reserved.ma_san_pham = sp.ma_san_pham
                """ + where + " ORDER BY sp.ten_san_pham", (rs, row) -> {
            int onHand = rs.getInt("so_luong_ton"), held = rs.getInt("so_luong_giu_cho");
            int minimum = rs.getInt("dinh_muc_ton_toi_thieu");
            String alert = onHand == 0 ? "Hết hàng" : onHand <= minimum ? "Sắp hết hàng" : "Bình thường";
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("maSanPham", rs.getString("ma_san_pham"));
            item.put("tenSanPham", rs.getString("ten_san_pham"));
            item.put("maThuongHieu", rs.getInt("ma_thuong_hieu"));
            item.put("tenThuongHieu", rs.getString("ten_thuong_hieu"));
            item.put("dongMay", rs.getString("dong_may"));
            item.put("ram", rs.getString("ram"));
            item.put("rom", rs.getString("rom"));
            item.put("mauSac", rs.getString("mau_sac"));
            item.put("soLuongTon", onHand);
            item.put("soLuongGiuCho", held);
            item.put("soLuongKhaDung", Math.max(0, onHand - held));
            item.put("dinhMucTonToiThieu", minimum);
            item.put("giaTriTonKho", money(rs.getBigDecimal("gia_tri_ton_kho")));
            item.put("mucDoCanhBao", alert);
            item.put("canhBaoThongDiep", alert);
            item.put("ngayNhapCuNhat", timestampOrNull(rs.getTimestamp("ngay_nhap_cu_nhat")));
            return item;
        }, args.toArray());
    }

    private Map<String, Map<String, Object>> inventorySnapshots() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> item : inventoryItems(null, null, null, null, null, null)) {
            result.put(String.valueOf(item.get("maSanPham")), item);
        }
        return result;
    }

    private List<Map<String, Object>> inventoryMovements() {
        return jdbc.query("""
                SELECT * FROM (
                    SELECT pn.ngay_nhap AS thoi_gian, 'NHAP_KHO' AS loai, pn.ma_phieu_nhap AS chung_tu,
                           sp.ten_san_pham, ctpn.so_luong AS thay_doi, nv.ho_ten AS nhan_vien,
                           COALESCE(pn.ghi_chu, '') AS ghi_chu
                    FROM phieu_nhap pn
                    JOIN chi_tiet_phieu_nhap ctpn ON ctpn.ma_phieu_nhap = pn.ma_phieu_nhap
                    JOIN san_pham sp ON sp.ma_san_pham = ctpn.ma_san_pham
                    JOIN nhan_vien nv ON nv.ma_nhan_vien = pn.ma_nhan_vien
                    WHERE pn.trang_thai = 'Hoan_thanh'
                    UNION ALL
                    SELECT hd.ngay_lap AS thoi_gian, 'XUAT_BAN' AS loai, hd.ma_hoa_don AS chung_tu,
                           sp.ten_san_pham, -COUNT(*) AS thay_doi, nv.ho_ten AS nhan_vien,
                           COALESCE(hd.ghi_chu, '') AS ghi_chu
                    FROM hoa_don hd
                    JOIN chi_tiet_hoa_don cthd ON cthd.ma_hoa_don = hd.ma_hoa_don
                    JOIN san_pham sp ON sp.ma_san_pham = cthd.ma_san_pham
                    JOIN nhan_vien nv ON nv.ma_nhan_vien = hd.ma_nhan_vien
                    WHERE hd.trang_thai_thanh_toan = 'Thanh_cong'
                    GROUP BY hd.ngay_lap, hd.ma_hoa_don, sp.ten_san_pham, nv.ho_ten, hd.ghi_chu
                ) movements
                ORDER BY thoi_gian DESC
                """, (rs, row) -> {
            Map<String, Object> movement = new LinkedHashMap<>();
            movement.put("thoiGian", rs.getTimestamp("thoi_gian"));
            movement.put("loaiBienDong", rs.getString("loai"));
            movement.put("maChungTu", rs.getString("chung_tu"));
            movement.put("tenSanPham", rs.getString("ten_san_pham"));
            movement.put("thayDoiSoLuong", rs.getInt("thay_doi"));
            movement.put("nhanVienThucHien", rs.getString("nhan_vien"));
            movement.put("ghiChu", rs.getString("ghi_chu"));
            return movement;
        });
    }

    private List<Map<String, Object>> invoiceTotals(LocalDate from, LocalDate to) {
        List<Object> args = new ArrayList<>();
        String dates = dateWhere("hd.ngay_lap", from, to, args);
        return jdbc.query("""
                SELECT hd.ngay_lap, hd.tong_tien_hang, hd.tien_giam_gia, hd.tong_thanh_toan
                FROM hoa_don hd
                WHERE hd.trang_thai_thanh_toan = 'Thanh_cong'
                """ + dates + " ORDER BY hd.ngay_lap", (rs, row) -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("ngay_lap", rs.getTimestamp("ngay_lap"));
            data.put("tong_tien_hang", rs.getBigDecimal("tong_tien_hang"));
            data.put("tien_giam_gia", rs.getBigDecimal("tien_giam_gia"));
            data.put("tong_thanh_toan", rs.getBigDecimal("tong_thanh_toan"));
            return data;
        }, args.toArray());
    }

    private Map<String, BigDecimal> soldCostByPeriod(LocalDate from, LocalDate to, String groupBy) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map<String, Object> row : soldItems(from, to, null)) {
            LocalDate day = timestamp(row.get("ngay_lap")).toLocalDate();
            String label = periodLabel(day, groupBy);
            result.merge(label, money(row.get("don_gia_nhap")), BigDecimal::add);
        }
        return result;
    }

    private int soldUnitCount(LocalDate from, LocalDate to) {
        List<Object> args = new ArrayList<>();
        String dates = dateWhere("hd.ngay_lap", from, to, args);
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM hoa_don hd
                JOIN chi_tiet_hoa_don cthd ON cthd.ma_hoa_don = hd.ma_hoa_don
                WHERE hd.trang_thai_thanh_toan = 'Thanh_cong'
                """ + dates, Integer.class, args.toArray());
        return count == null ? 0 : count;
    }

    private BigDecimal invoiceNet(LocalDate from, LocalDate to) {
        List<Object> args = new ArrayList<>();
        String dates = dateWhere("hd.ngay_lap", from, to, args);
        BigDecimal total = jdbc.queryForObject("""
                SELECT COALESCE(SUM(hd.tong_thanh_toan), 0)
                FROM hoa_don hd WHERE hd.trang_thai_thanh_toan = 'Thanh_cong'
                """ + dates, BigDecimal.class, args.toArray());
        return money(total);
    }

    private List<Map<String, Object>> soldItems(LocalDate from, LocalDate to, String brand) {
        List<Object> args = new ArrayList<>();
        String dates = dateWhere("hd.ngay_lap", from, to, args);
        String brandClause = "";
        if (brand != null && !brand.isBlank()) {
            brandClause = " AND sp.ma_thuong_hieu = ?";
            args.add(brand);
        }
        return jdbc.query("""
                SELECT hd.ngay_lap, hd.tong_tien_hang AS hoa_don_tong_tien_hang,
                       hd.tien_giam_gia AS hoa_don_tien_giam_gia,
                       cthd.ma_san_pham, sp.ten_san_pham, th.ten_thuong_hieu,
                       sp.ma_thuong_hieu, sp.ram, sp.rom, cthd.don_gia,
                       COALESCE(ctpn.don_gia_nhap, sp.gia_nhap_tham_khao) AS don_gia_nhap
                FROM hoa_don hd
                JOIN chi_tiet_hoa_don cthd ON cthd.ma_hoa_don = hd.ma_hoa_don
                JOIN san_pham sp ON sp.ma_san_pham = cthd.ma_san_pham
                LEFT JOIN thuong_hieu th ON th.ma_thuong_hieu = sp.ma_thuong_hieu
                LEFT JOIN imei_san_pham i ON i.ma_imei = cthd.ma_imei
                LEFT JOIN chi_tiet_phieu_nhap ctpn
                  ON ctpn.ma_phieu_nhap = i.ma_phieu_nhap AND ctpn.ma_san_pham = cthd.ma_san_pham
                WHERE hd.trang_thai_thanh_toan = 'Thanh_cong'
                """ + dates + brandClause + " ORDER BY hd.ngay_lap", (rs, row) -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("ngay_lap", rs.getTimestamp("ngay_lap"));
            data.put("hoa_don_tong_tien_hang", rs.getBigDecimal("hoa_don_tong_tien_hang"));
            data.put("hoa_don_tien_giam_gia", rs.getBigDecimal("hoa_don_tien_giam_gia"));
            data.put("ma_san_pham", rs.getString("ma_san_pham"));
            data.put("ten_san_pham", rs.getString("ten_san_pham"));
            data.put("ten_thuong_hieu", rs.getString("ten_thuong_hieu"));
            data.put("ma_thuong_hieu", rs.getInt("ma_thuong_hieu"));
            data.put("ram", rs.getString("ram"));
            data.put("rom", rs.getString("rom"));
            data.put("don_gia", rs.getBigDecimal("don_gia"));
            data.put("don_gia_nhap", rs.getBigDecimal("don_gia_nhap"));
            return data;
        }, args.toArray());
    }

    private List<String> distinctProducts(String column) {
        if (!List.of("dong_may", "ram", "rom", "mau_sac").contains(column)) throw new IllegalArgumentException();
        return jdbc.query("SELECT DISTINCT " + column + " FROM san_pham WHERE " + column
                + " IS NOT NULL AND " + column + " <> '' ORDER BY " + column, (rs, row) -> rs.getString(1));
    }

    private static void addFilter(StringBuilder where, List<Object> args, String column, String value) {
        if (value != null && !value.isBlank()) {
            where.append(" AND ").append(column).append(" = ? ");
            args.add(value.trim());
        }
    }

    private static String dateWhere(String column, LocalDate from, LocalDate to, List<Object> args) {
        StringBuilder sql = new StringBuilder();
        if (from != null) {
            sql.append(" AND ").append(column).append(" >= ?");
            args.add(from.atStartOfDay());
        }
        if (to != null) {
            sql.append(" AND ").append(column).append(" < ?");
            args.add(to.plusDays(1).atStartOfDay());
        }
        return sql.toString();
    }

    private static Object[] reportParams(List<Object> dates, String brand) {
        if (brand != null) dates.add(brand);
        return dates.toArray();
    }

    private static String periodKey(LocalDate date, String group) {
        return periodLabel(date, group);
    }

    private static String periodLabel(LocalDate date, String group) {
        return switch (group) {
            case "DAY" -> date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            case "YEAR" -> String.valueOf(date.getYear());
            default -> YearMonth.from(date).toString();
        };
    }

    private static String periodSortValue(String label, String group) {
        return label;
    }

    private static BigDecimal percentageChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(ZERO) == 0) return ZERO;
        return current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(Object value) {
        return value == null ? ZERO : new BigDecimal(value.toString());
    }

    private static LocalDateTime timestamp(Object value) {
        if (value instanceof Timestamp ts) return ts.toLocalDateTime();
        if (value instanceof LocalDateTime dt) return dt;
        if (value instanceof java.util.Date date) return new Timestamp(date.getTime()).toLocalDateTime();
        return LocalDateTime.parse(String.valueOf(value).replace(' ', 'T'));
    }

    private static LocalDateTime timestampOrNull(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private static void validateDates(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc");
        }
    }

    private static void validateRequest(ReportRequest request) {
        if (request == null) throw new IllegalArgumentException("Thiếu dữ liệu báo cáo");
        validateDates(request.tuNgay(), request.denNgay());
        normalizeType(request.loaiBaoCao());
        if (request.maThuongHieu() != null && request.maThuongHieu() <= 0) {
            throw new IllegalArgumentException("Mã thương hiệu không hợp lệ");
        }
    }

    private static String normalizeType(String type) {
        String normalized = type == null || type.isBlank() ? "Tong_hop" : type.trim();
        if (!List.of("Tong_hop", "Doanh_thu", "SanPham_BanChay", "Kiem_ke_kho", "Bao_hanh").contains(normalized)) {
            throw new IllegalArgumentException("Loại báo cáo không hợp lệ");
        }
        return normalized;
    }

    private static String filterLabel(LocalDate from, LocalDate to, String brand) {
        String range = (from == null ? "Tất cả" : from.toString()) + " – " + (to == null ? "nay" : to.toString());
        String brandName = "Tất cả thương hiệu";
        if (brand != null) {
            brandName = brand;
        }
        return "Kỳ: " + range + " | Thương hiệu: " + brandName;
    }

    private static int sumInt(List<Map<String, Object>> rows, String key) {
        return rows.stream().mapToInt(row -> (Integer) row.get(key)).sum();
    }

    private static BigDecimal sumMoney(List<Map<String, Object>> rows, String key) {
        return rows.stream().map(row -> money(row.get(key))).reduce(ZERO, BigDecimal::add);
    }

    private static final class RevenueBucket {
        private final String label;
        private BigDecimal gross = ZERO, discount = ZERO, net = ZERO, profitRevenue = ZERO, cost = ZERO, profit = ZERO;
        private int invoiceCount;
        private RevenueBucket(String label) { this.label = label; }
    }

    private static final class ProductSales {
        private final String id, name, brand, ram, rom;
        private BigDecimal revenue = ZERO, cost = ZERO;
        private int units;
        private ProductSales(Map<String, Object> source) {
            id = String.valueOf(source.get("ma_san_pham"));
            name = text(source.get("ten_san_pham"));
            brand = text(source.get("ten_thuong_hieu"));
            ram = text(source.get("ram"));
            rom = text(source.get("rom"));
        }
    }
}
