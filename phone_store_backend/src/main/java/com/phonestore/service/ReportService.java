package com.phonestore.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.phonestore.dao.PromotionDao;
import com.phonestore.dao.ReportDao;
import com.phonestore.dao.StatisticsDao;
import com.phonestore.dto.request.ReportGenerateRequest;
import com.phonestore.dto.response.InventoryStatsResponse;
import com.phonestore.dto.response.OptionItemDTO;
import com.phonestore.dto.response.ProductStatsResponse;
import com.phonestore.dto.response.ReportLogDTO;
import com.phonestore.dto.response.ReportPreviewResponse;
import com.phonestore.dto.response.RevenueStatsResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Set<String> VALID_TEMPLATES = Set.of(
            "Doanh_thu", "SanPham_BanChay", "Kiem_ke_kho", "Ton_kho", "Tong_hop", "Bao_hanh"
    );

    private final ReportDao reportDao;
    private final StatisticsDao statisticsDao;
    private final PromotionDao promotionDao;

    public ReportPreviewResponse generatePreview(ReportGenerateRequest req) {
        validateRequest(req);
        LocalDate tuNgay = req.getTuNgay() != null ? req.getTuNgay() : LocalDate.now().minusDays(30);
        LocalDate denNgay = req.getDenNgay() != null ? req.getDenNgay() : LocalDate.now();
        String staffId = (req.getMaNhanVien() == null || req.getMaNhanVien().isBlank()) ? "NV001" : req.getMaNhanVien().trim();
        String staffName = reportDao.findEmployeeName(staffId);
        String scopeLabel = resolveScopeLabel(req.getMaThuongHieu(), tuNgay, denNgay);

        return switch (req.getLoaiBaoCao()) {
            case "Doanh_thu" -> buildRevenuePreview(tuNgay, denNgay, scopeLabel, staffName);
            case "SanPham_BanChay" -> buildBestSellerPreview(tuNgay, denNgay, req.getMaThuongHieu(), scopeLabel, staffName);
            case "Kiem_ke_kho", "Ton_kho" -> buildInventoryPreview(req.getLoaiBaoCao(), tuNgay, denNgay, req.getMaThuongHieu(), scopeLabel, staffName);
            case "Bao_hanh" -> buildWarrantyPreview(tuNgay, denNgay, scopeLabel, staffName);
            default -> buildSummaryPreview(tuNgay, denNgay, req.getMaThuongHieu(), scopeLabel, staffName);
        };
    }

    public ExportedReport exportExcel(ReportGenerateRequest req) {
        ReportPreviewResponse preview = generatePreview(req);
        String filename = buildFileName(preview.getLoaiBaoCao(), "xlsx");

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("BaoCao");
            int colCount = Math.max(1, preview.getColumns().size());

            XSSFFont titleFont = (XSSFFont) wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            XSSFFont headerFont = (XSSFFont) wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyThinBorders(headerStyle);

            CellStyle bodyStyle = wb.createCellStyle();
            bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyThinBorders(bodyStyle);

            XSSFFont boldFont = (XSSFFont) wb.createFont();
            boldFont.setBold(true);
            CellStyle boldStyle = wb.createCellStyle();
            boldStyle.setFont(boldFont);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(preview.getTieuDeBaoCao().toUpperCase(Locale.ROOT));
            titleCell.setCellStyle(titleStyle);
            if (colCount > 1) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, colCount - 1));
            }

            Row metaRow = sheet.createRow(1);
            metaRow.createCell(0).setCellValue("Phạm vi: " + preview.getPhamViLoc() + " | Người lập: " + preview.getNguoiLap());

            int rowIdx = 3;
            Row headerRow = sheet.createRow(rowIdx++);
            headerRow.setHeightInPoints(24);
            for (int c = 0; c < preview.getColumns().size(); c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(preview.getColumns().get(c));
                cell.setCellStyle(headerStyle);
            }

            for (List<String> dataLine : preview.getRows()) {
                Row r = sheet.createRow(rowIdx++);
                for (int c = 0; c < dataLine.size(); c++) {
                    Cell cell = r.createCell(c);
                    cell.setCellValue(dataLine.get(c));
                    cell.setCellStyle(bodyStyle);
                }
            }

            rowIdx++;
            for (Map.Entry<String, String> entry : preview.getSummary().entrySet()) {
                Row sr = sheet.createRow(rowIdx++);
                Cell kCell = sr.createCell(0);
                kCell.setCellValue(entry.getKey());
                kCell.setCellStyle(boldStyle);
                Cell vCell = sr.createCell(1);
                vCell.setCellValue(entry.getValue());
                vCell.setCellStyle(boldStyle);
            }

            for (int c = 0; c < colCount; c++) {
                sheet.autoSizeColumn(c);
                int currentWidth = sheet.getColumnWidth(c);
                sheet.setColumnWidth(c, Math.min(12000, Math.max(3600, currentWidth + 600)));
            }

            wb.write(out);
            saveExportLog(req, preview, "EXCEL", filename);
            return new ExportedReport(filename, out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Không thể khởi tạo file Excel báo cáo: " + e.getMessage(), e);
        }
    }

    public ExportedReport exportPdf(ReportGenerateRequest req) {
        ReportPreviewResponse preview = generatePreview(req);
        String filename = buildFileName(preview.getLoaiBaoCao(), "pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 32, 32, 32, 32);
            PdfWriter.getInstance(doc, out);
            doc.open();

            PdfFontBundle fonts = loadPdfFonts();

            Paragraph storeBrand = new Paragraph(formatPdfText("CỬA HÀNG ĐIỆN THOẠI PHONESTORE PRO", fonts.unicode()), fonts.titleSmall());
            storeBrand.setAlignment(Element.ALIGN_LEFT);
            doc.add(storeBrand);

            Paragraph title = new Paragraph(formatPdfText(preview.getTieuDeBaoCao().toUpperCase(Locale.ROOT), fonts.unicode()), fonts.titleLarge());
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(8);
            title.setSpacingAfter(6);
            doc.add(title);

            Paragraph meta = new Paragraph(
                    formatPdfText(
                            "Kỳ báo cáo: " + DATE_FMT.format(preview.getTuNgay()) + " - " + DATE_FMT.format(preview.getDenNgay())
                                    + "  |  " + preview.getPhamViLoc() + "\nNgười lập: " + preview.getNguoiLap()
                                    + "  |  Ngày xuất: " + DT_FMT.format(preview.getNgayLap()),
                            fonts.unicode()
                    ),
                    fonts.muted()
            );
            meta.setAlignment(Element.ALIGN_CENTER);
            meta.setSpacingAfter(12);
            doc.add(meta);

            int cols = Math.max(1, preview.getColumns().size());
            PdfPTable table = new PdfPTable(cols);
            table.setWidthPercentage(100);
            table.setSpacingAfter(12);

            for (String colTitle : preview.getColumns()) {
                PdfPCell hCell = new PdfPCell(new Phrase(formatPdfText(colTitle, fonts.unicode()), fonts.header()));
                hCell.setBackgroundColor(new Color(30, 58, 138));
                hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                hCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                hCell.setPadding(6);
                table.addCell(hCell);
            }

            if (preview.getRows().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase(formatPdfText("Không có dữ liệu phát sinh trong kỳ báo cáo", fonts.unicode()), fonts.body()));
                emptyCell.setColspan(cols);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setPadding(10);
                table.addCell(emptyCell);
            } else {
                for (int r = 0; r < preview.getRows().size(); r++) {
                    List<String> line = preview.getRows().get(r);
                    Color rowBg = (r % 2 == 1) ? new Color(248, 250, 252) : Color.WHITE;
                    for (String val : line) {
                        PdfPCell cell = new PdfPCell(new Phrase(formatPdfText(val, fonts.unicode()), fonts.body()));
                        cell.setBackgroundColor(rowBg);
                        cell.setPadding(5);
                        table.addCell(cell);
                    }
                }
            }
            doc.add(table);

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(65);
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.setSpacingAfter(20);
            for (Map.Entry<String, String> entry : preview.getSummary().entrySet()) {
                PdfPCell k = new PdfPCell(new Phrase(formatPdfText(entry.getKey(), fonts.unicode()), fonts.bold()));
                k.setBackgroundColor(new Color(241, 245, 249));
                k.setPadding(6);
                PdfPCell v = new PdfPCell(new Phrase(formatPdfText(entry.getValue(), fonts.unicode()), fonts.bold()));
                v.setHorizontalAlignment(Element.ALIGN_RIGHT);
                v.setPadding(6);
                summaryTable.addCell(k);
                summaryTable.addCell(v);
            }
            doc.add(summaryTable);

            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.addCell(signatureCell(formatPdfText("Người lập báo cáo\n(Ký, ghi rõ họ tên)\n\n\n" + preview.getNguoiLap(), fonts.unicode()), fonts.body()));
            signTable.addCell(signatureCell(formatPdfText("Quản lý phê duyệt\n(Ký, đóng dấu)\n\n\nNgày ..... tháng ..... năm .....", fonts.unicode()), fonts.body()));
            doc.add(signTable);

            doc.close();
            saveExportLog(req, preview, "PDF", filename);
            return new ExportedReport(filename, out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Không thể khởi tạo file PDF báo cáo: " + e.getMessage(), e);
        }
    }

    public List<ReportLogDTO> getReportLogs(int limit) {
        return reportDao.findReportLogs(limit);
    }

    private ReportPreviewResponse buildRevenuePreview(LocalDate tuNgay, LocalDate denNgay, String scope, String staffName) {
        RevenueStatsResponse rev = statisticsDao.getRevenueStatistics(tuNgay, denNgay, "DAY");
        List<String> cols = List.of("Thời gian", "Số hóa đơn", "Doanh thu gộp", "Chiết khấu KM", "Doanh thu thực thu", "Tiền vốn", "Lợi nhuận gộp");
        List<List<String>> rows = new ArrayList<>();
        for (RevenueStatsResponse.RevenuePointDTO pt : rev.getSeries()) {
            rows.add(List.of(
                    pt.getNhanThoiGian(),
                    String.valueOf(pt.getSoHoaDon()),
                    formatVnd(pt.getDoanhThuGop()),
                    formatVnd(pt.getChietKhau()),
                    formatVnd(pt.getDoanhThuThucThu()),
                    formatVnd(pt.getTienVon()),
                    formatVnd(pt.getLoiNhuanGop())
            ));
        }

        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("Tổng số hóa đơn", String.valueOf(rev.getTongHoaDon()));
        summary.put("Doanh thu gộp", formatVnd(rev.getDoanhThuGop()));
        summary.put("Tổng chiết khấu khuyến mãi", formatVnd(rev.getTongChietKhau()));
        summary.put("Doanh thu thực thu", formatVnd(rev.getDoanhThuThucThu()));
        summary.put("Tổng tiền vốn (COGS)", formatVnd(rev.getTongTienVon()));
        summary.put("Lợi nhuận gộp", formatVnd(rev.getLoiNhuanGop()));

        return ReportPreviewResponse.builder()
                .loaiBaoCao("Doanh_thu")
                .tieuDeBaoCao("Báo cáo doanh thu & lợi nhuận kinh doanh")
                .tuNgay(tuNgay)
                .denNgay(denNgay)
                .phamViLoc(scope)
                .ngayLap(LocalDateTime.now())
                .nguoiLap(staffName)
                .columns(cols)
                .rows(rows)
                .summary(summary)
                .tongDoanhThu(rev.getDoanhThuThucThu())
                .tongChiPhi(rev.getTongTienVon())
                .tongLoiNhuan(rev.getLoiNhuanGop())
                .tongSoLuong(rev.getTongSanPhamBan())
                .build();
    }

    private ReportPreviewResponse buildBestSellerPreview(LocalDate tuNgay, LocalDate denNgay, Integer maThuongHieu, String scope, String staffName) {
        ProductStatsResponse stats = statisticsDao.getProductStatistics(tuNgay, denNgay, maThuongHieu);
        List<String> cols = List.of("Mã SP", "Tên sản phẩm", "Thương hiệu", "Đã bán", "Doanh thu", "Lợi nhuận", "Tồn hiện tại");
        List<List<String>> rows = new ArrayList<>();
        BigDecimal totalProfit = BigDecimal.ZERO;
        for (ProductStatsResponse.ProductSalesStatDTO p : stats.getBestSellers()) {
            totalProfit = totalProfit.add(p.getLoiNhuan());
            rows.add(List.of(
                    p.getMaSanPham(),
                    p.getTenSanPham(),
                    p.getTenThuongHieu(),
                    String.valueOf(p.getSoLuongDaBan()),
                    formatVnd(p.getDoanhThu()),
                    formatVnd(p.getLoiNhuan()),
                    String.valueOf(p.getTonKhoHienTai())
            ));
        }

        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("Tổng số thiết bị đã bán", String.valueOf(stats.getTongSoLuongBan()));
        summary.put("Tổng doanh thu nhóm hàng", formatVnd(stats.getTongDoanhThu()));
        summary.put("Tổng lợi nhuận nhóm hàng", formatVnd(totalProfit));
        summary.put("Số model tồn đọng (>60 ngày / 0 bán)", String.valueOf(stats.getStagnantProducts().size()));

        return ReportPreviewResponse.builder()
                .loaiBaoCao("SanPham_BanChay")
                .tieuDeBaoCao("Báo cáo xếp hạng sản phẩm bán chạy")
                .tuNgay(tuNgay)
                .denNgay(denNgay)
                .phamViLoc(scope)
                .ngayLap(LocalDateTime.now())
                .nguoiLap(staffName)
                .columns(cols)
                .rows(rows)
                .summary(summary)
                .tongDoanhThu(stats.getTongDoanhThu())
                .tongChiPhi(BigDecimal.ZERO)
                .tongLoiNhuan(totalProfit)
                .tongSoLuong(stats.getTongSoLuongBan())
                .build();
    }

    private ReportPreviewResponse buildInventoryPreview(String template, LocalDate tuNgay, LocalDate denNgay, Integer maThuongHieu, String scope, String staffName) {
        InventoryStatsResponse inv = statisticsDao.getInventoryStatistics(maThuongHieu, null, null, null, null, null, false);
        List<String> cols = List.of("Mã SP", "Tên sản phẩm", "Hãng", "Cấu hình", "Tồn thực", "Giữ chỗ", "Khả dụng", "Giá trị tồn", "Trạng thái");
        List<List<String>> rows = new ArrayList<>();
        for (InventoryStatsResponse.InventoryStatItemDTO item : inv.getItems()) {
            rows.add(List.of(
                    item.getMaSanPham(),
                    item.getTenSanPham(),
                    item.getTenThuongHieu(),
                    item.getRam() + "/" + item.getRom() + " - " + item.getMauSac(),
                    String.valueOf(item.getSoLuongTon()),
                    String.valueOf(item.getSoLuongGiuCho()),
                    String.valueOf(item.getSoLuongKhaDung()),
                    formatVnd(item.getGiaTriTonKho()),
                    item.getMucDoCanhBao()
            ));
        }

        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("Tổng số mã thiết bị", String.valueOf(inv.getTongMaSanPham()));
        summary.put("Tổng tồn kho thực tế", String.valueOf(inv.getTongSoLuongTon()));
        summary.put("Đang giữ chỗ / Khả dụng", inv.getTongDangGiuCho() + " / " + inv.getTongKhaDung());
        summary.put("Thiết bị cảnh báo định mức", String.valueOf(inv.getSoSanPhamCanhBao()));
        summary.put("Tổng giá trị hàng tồn kho", formatVnd(inv.getTongGiaTriTonKho()));

        return ReportPreviewResponse.builder()
                .loaiBaoCao(template)
                .tieuDeBaoCao("Báo cáo kiểm kê & định mức tồn kho")
                .tuNgay(tuNgay)
                .denNgay(denNgay)
                .phamViLoc(scope)
                .ngayLap(LocalDateTime.now())
                .nguoiLap(staffName)
                .columns(cols)
                .rows(rows)
                .summary(summary)
                .tongDoanhThu(BigDecimal.ZERO)
                .tongChiPhi(inv.getTongGiaTriTonKho())
                .tongLoiNhuan(BigDecimal.ZERO)
                .tongSoLuong(inv.getTongSoLuongTon())
                .build();
    }

    private ReportPreviewResponse buildSummaryPreview(LocalDate tuNgay, LocalDate denNgay, Integer maThuongHieu, String scope, String staffName) {
        RevenueStatsResponse rev = statisticsDao.getRevenueStatistics(tuNgay, denNgay, "DAY");
        List<ReportDao.ImportExportRow> ioRows = reportDao.findImportExportSummary(tuNgay, denNgay, maThuongHieu);
        List<String> cols = List.of("Mã SP", "Tên sản phẩm", "Thương hiệu", "Nhập kỳ", "Xuất kỳ", "Tồn cuối", "Định mức", "Doanh thu kỳ");
        List<List<String>> rows = new ArrayList<>();
        int totalOut = 0;
        int totalStock = 0;
        BigDecimal totalProductRev = BigDecimal.ZERO;

        for (ReportDao.ImportExportRow r : ioRows) {
            totalOut += r.xuatTrongKy();
            totalStock += r.tonCuoiKy();
            totalProductRev = totalProductRev.add(r.doanhThuTrongKy());
            rows.add(List.of(
                    r.maSanPham(),
                    r.tenSanPham(),
                    r.tenThuongHieu(),
                    String.valueOf(r.nhapTrongKy()),
                    String.valueOf(r.xuatTrongKy()),
                    String.valueOf(r.tonCuoiKy()),
                    String.valueOf(r.dinhMucToiThieu()),
                    formatVnd(r.doanhThuTrongKy())
            ));
        }

        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("Số hóa đơn bán hàng", String.valueOf(rev.getTongHoaDon()));
        summary.put("Doanh thu thực thu toàn kỳ", formatVnd(rev.getDoanhThuThucThu()));
        summary.put("Tổng chiết khấu khuyến mãi", formatVnd(rev.getTongChietKhau()));
        summary.put("Lợi nhuận gộp toàn kỳ", formatVnd(rev.getLoiNhuanGop()));
        summary.put("Tổng xuất bán / Tồn kho", totalOut + " máy bán / " + totalStock + " máy tồn");

        return ReportPreviewResponse.builder()
                .loaiBaoCao("Tong_hop")
                .tieuDeBaoCao("Báo cáo tổng hợp bán hàng - khuyến mãi - xuất nhập tồn")
                .tuNgay(tuNgay)
                .denNgay(denNgay)
                .phamViLoc(scope)
                .ngayLap(LocalDateTime.now())
                .nguoiLap(staffName)
                .columns(cols)
                .rows(rows)
                .summary(summary)
                .tongDoanhThu(rev.getDoanhThuThucThu())
                .tongChiPhi(rev.getTongTienVon())
                .tongLoiNhuan(rev.getLoiNhuanGop())
                .tongSoLuong(totalStock)
                .build();
    }

    private ReportPreviewResponse buildWarrantyPreview(LocalDate tuNgay, LocalDate denNgay, String scope, String staffName) {
        List<ReportDao.WarrantyRow> list = reportDao.findWarrantySummary(tuNgay, denNgay);
        List<String> cols = List.of("Mã phiếu", "Thiết bị", "IMEI", "Khách hàng", "Ngày tiếp nhận", "Mô tả lỗi", "Chi phí", "Trạng thái");
        List<List<String>> rows = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        for (ReportDao.WarrantyRow w : list) {
            totalCost = totalCost.add(w.chiPhiSuaChua());
            rows.add(List.of(
                    w.maPhieuBaoHanh(),
                    w.tenSanPham(),
                    w.maImei(),
                    w.tenKhachHang(),
                    w.ngayTiepNhan() != null ? DT_FMT.format(w.ngayTiepNhan()) : "-",
                    w.loiKhachPhanAnh(),
                    formatVnd(w.chiPhiSuaChua()),
                    w.trangThai()
            ));
        }

        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("Tổng số phiếu tiếp nhận", String.valueOf(list.size()));
        summary.put("Tổng chi phí sửa chữa thu thêm", formatVnd(totalCost));

        return ReportPreviewResponse.builder()
                .loaiBaoCao("Bao_hanh")
                .tieuDeBaoCao("Báo cáo tiếp nhận & xử lý bảo hành")
                .tuNgay(tuNgay)
                .denNgay(denNgay)
                .phamViLoc(scope)
                .ngayLap(LocalDateTime.now())
                .nguoiLap(staffName)
                .columns(cols)
                .rows(rows)
                .summary(summary)
                .tongDoanhThu(totalCost)
                .tongChiPhi(BigDecimal.ZERO)
                .tongLoiNhuan(totalCost)
                .tongSoLuong(list.size())
                .build();
    }

    private void saveExportLog(ReportGenerateRequest req, ReportPreviewResponse preview, String format, String filename) {
        String staffId = (req.getMaNhanVien() == null || req.getMaNhanVien().isBlank()) ? "NV001" : req.getMaNhanVien().trim();
        reportDao.insertReportLog(ReportLogDTO.builder()
                .loaiBaoCao(preview.getLoaiBaoCao())
                .maNhanVienLap(staffId)
                .tuNgay(preview.getTuNgay())
                .denNgay(preview.getDenNgay())
                .dinhDangXuat(format)
                .duongDanFile(filename)
                .tieuChiLoc(preview.getPhamViLoc())
                .ghiChu(req.getGhiChu() == null || req.getGhiChu().isBlank() ? preview.getTieuDeBaoCao() : req.getGhiChu().trim())
                .build());
    }

    private void validateRequest(ReportGenerateRequest req) {
        if (req == null || req.getLoaiBaoCao() == null || !VALID_TEMPLATES.contains(req.getLoaiBaoCao())) {
            throw new IllegalArgumentException("Mẫu báo cáo không hợp lệ (chọn: Doanh_thu, SanPham_BanChay, Kiem_ke_kho, Tong_hop, Bao_hanh)");
        }
        if (req.getTuNgay() != null && req.getDenNgay() != null && req.getDenNgay().isBefore(req.getTuNgay())) {
            throw new IllegalArgumentException("Khoảng thời gian báo cáo không hợp lệ: Ngày kết thúc phải >= ngày bắt đầu");
        }
    }

    private String resolveScopeLabel(Integer maThuongHieu, LocalDate tuNgay, LocalDate denNgay) {
        String brandName = "Tất cả thương hiệu";
        if (maThuongHieu != null) {
            for (OptionItemDTO b : promotionDao.findBrandOptions()) {
                if (String.valueOf(maThuongHieu).equals(b.getValue())) {
                    brandName = b.getLabel();
                    break;
                }
            }
        }
        return "Thương hiệu: " + brandName + " | Từ " + DATE_FMT.format(tuNgay) + " đến " + DATE_FMT.format(denNgay);
    }

    private String buildFileName(String template, String ext) {
        String ts = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        return "baocao_" + template.toLowerCase(Locale.ROOT) + "_" + ts + "." + ext;
    }

    private String formatVnd(BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount;
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return nf.format(safe) + " đ";
    }

    private void applyThinBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private PdfPCell signatureCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        return cell;
    }

    private PdfFontBundle loadPdfFonts() {
        try {
            File regular = new File("C:/Windows/Fonts/arial.ttf");
            File bold = new File("C:/Windows/Fonts/arialbd.ttf");
            if (regular.exists()) {
                BaseFont bfReg = BaseFont.createFont(regular.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                BaseFont bfBold = bold.exists()
                        ? BaseFont.createFont(bold.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
                        : bfReg;
                return new PdfFontBundle(
                        true,
                        new Font(bfBold, 15, Font.BOLD, new Color(15, 23, 42)),
                        new Font(bfBold, 10, Font.BOLD, new Color(37, 99, 235)),
                        new Font(bfBold, 9, Font.BOLD, Color.WHITE),
                        new Font(bfBold, 9, Font.BOLD, new Color(15, 23, 42)),
                        new Font(bfReg, 9, Font.NORMAL, new Color(30, 41, 59)),
                        new Font(bfReg, 9, Font.NORMAL, new Color(100, 116, 139))
                );
            }
        } catch (Exception ignored) {
        }
        return new PdfFontBundle(
                false,
                new Font(Font.HELVETICA, 14, Font.BOLD, new Color(15, 23, 42)),
                new Font(Font.HELVETICA, 10, Font.BOLD, new Color(37, 99, 235)),
                new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE),
                new Font(Font.HELVETICA, 9, Font.BOLD, new Color(15, 23, 42)),
                new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(30, 41, 59)),
                new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(100, 116, 139))
        );
    }

    private String formatPdfText(String text, boolean supportsUnicode) {
        if (text == null) return "";
        if (supportsUnicode) return text;
        String replaced = text.replace('đ', 'd').replace('Đ', 'D');
        return Normalizer.normalize(replaced, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private record PdfFontBundle(
            boolean unicode,
            Font titleLarge,
            Font titleSmall,
            Font header,
            Font bold,
            Font body,
            Font muted
    ) {
    }

    public record ExportedReport(String filename, byte[] bytes) {
    }
}

