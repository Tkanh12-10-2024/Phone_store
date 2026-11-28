package com.phonestore.Controller;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.phonestore.dao.StatisticsReportDAO;
import com.phonestore.dto.ReportRequest;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatisticsReportController {
    private static final String EXCEL_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private final StatisticsReportDAO dao;

    public StatisticsReportController(StatisticsReportDAO dao) {
        this.dao = dao;
    }

    @GetMapping("/statistics/revenue")
    public Map<String, Object> revenue(@RequestParam(required = false) String tuNgay,
                                       @RequestParam(required = false) String denNgay,
                                       @RequestParam(required = false) String groupBy) {
        return dao.revenue(parseDate(tuNgay), parseDate(denNgay), groupBy);
    }

    @GetMapping("/statistics/products")
    public Map<String, Object> products(@RequestParam(required = false) String tuNgay,
                                        @RequestParam(required = false) String denNgay,
                                        @RequestParam(required = false) String maThuongHieu) {
        return dao.products(parseDate(tuNgay), parseDate(denNgay), maThuongHieu);
    }

    @GetMapping("/statistics/inventory")
    public Map<String, Object> inventory(@RequestParam(required = false) String maThuongHieu,
                                         @RequestParam(required = false) String dongMay,
                                         @RequestParam(required = false) String ram,
                                         @RequestParam(required = false) String rom,
                                         @RequestParam(required = false) String mauSac,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(defaultValue = "false") boolean onlyAlert) {
        return dao.inventory(maThuongHieu, dongMay, ram, rom, mauSac, keyword, onlyAlert);
    }

    @GetMapping("/statistics/inventory/filters")
    public Map<String, Object> inventoryFilters() {
        return dao.inventoryFilters();
    }

    @PostMapping("/reports/preview")
    public Map<String, Object> preview(@RequestBody ReportRequest request) {
        return dao.preview(request);
    }

    @PostMapping("/reports/export/excel")
    public ResponseEntity<byte[]> exportExcel(@RequestBody ReportRequest request) throws IOException {
        Map<String, Object> preview = dao.preview(request);
        String filename = filename(preview, "xlsx");
        byte[] bytes = excel(preview);
        dao.logExport(request, "EXCEL", filename, preview);
        return binary(bytes, EXCEL_MIME, filename);
    }

    @PostMapping("/reports/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestBody ReportRequest request) throws IOException {
        Map<String, Object> preview = dao.preview(request);
        String filename = filename(preview, "pdf");
        byte[] bytes = pdf(preview);
        dao.logExport(request, "PDF", filename, preview);
        return binary(bytes, MediaType.APPLICATION_PDF_VALUE, filename);
    }

    @GetMapping("/reports/history")
    public List<Map<String, Object>> history(@RequestParam(defaultValue = "15") int limit) {
        return dao.history(limit);
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Ngày phải có định dạng YYYY-MM-DD");
        }
    }

    private static byte[] excel(Map<String, Object> preview) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Báo cáo");
            int rowIndex = 0;
            Row title = sheet.createRow(rowIndex++);
            title.createCell(0).setCellValue(String.valueOf(preview.get("tieuDeBaoCao")));
            Row filters = sheet.createRow(rowIndex++);
            filters.createCell(0).setCellValue("Phạm vi: " + preview.get("phamViLoc"));
            filters.createCell(1).setCellValue("Người lập: " + preview.get("nguoiLap"));

            List<?> columns = (List<?>) preview.get("columns");
            Row header = sheet.createRow(rowIndex++);
            for (int i = 0; i < columns.size(); i++) header.createCell(i).setCellValue(String.valueOf(columns.get(i)));
            for (Object value : (List<?>) preview.get("rows")) {
                Row row = sheet.createRow(rowIndex++);
                List<?> cells = (List<?>) value;
                for (int i = 0; i < cells.size(); i++) row.createCell(i).setCellValue(String.valueOf(cells.get(i)));
            }
            rowIndex++;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) preview.get("summary")).entrySet()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(String.valueOf(entry.getKey()));
                row.createCell(1).setCellValue(String.valueOf(entry.getValue()));
            }
            for (int i = 0; i < columns.size(); i++) sheet.autoSizeColumn(i);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static byte[] pdf(Map<String, Object> preview) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 28, 28, 30, 30);
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph(String.valueOf(preview.get("tieuDeBaoCao")), new Font(Font.HELVETICA, 16, Font.BOLD)));
            document.add(new Paragraph("Phạm vi: " + preview.get("phamViLoc")));
            document.add(new Paragraph("Người lập: " + preview.get("nguoiLap") + " • Ngày lập: " + preview.get("ngayLap")));
            document.add(new Paragraph(" "));
            List<?> columns = (List<?>) preview.get("columns");
            PdfPTable table = new PdfPTable(columns.size());
            table.setWidthPercentage(100);
            for (Object column : columns) table.addCell(new Phrase(String.valueOf(column), new Font(Font.HELVETICA, 9, Font.BOLD)));
            for (Object value : (List<?>) preview.get("rows")) {
                for (Object cell : (List<?>) value) table.addCell(new Phrase(String.valueOf(cell), new Font(Font.HELVETICA, 8)));
            }
            if (((List<?>) preview.get("rows")).isEmpty()) {
                table.addCell(new Phrase("Không có dữ liệu trong kỳ báo cáo"));
                for (int i = 1; i < columns.size(); i++) table.addCell("");
            }
            document.add(table);
            document.add(new Paragraph(" "));
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) preview.get("summary")).entrySet()) {
                document.add(new Paragraph(entry.getKey() + ": " + entry.getValue()));
            }
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            if (exception instanceof IOException io) throw io;
            throw new IOException("Không thể tạo tệp PDF", exception);
        }
    }

    private static ResponseEntity<byte[]> binary(byte[] data, String mediaType, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mediaType));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8).build());
        headers.setContentLength(data.length);
        return ResponseEntity.ok().headers(headers).body(data);
    }

    private static String filename(Map<String, Object> preview, String extension) {
        String type = String.valueOf(preview.get("loaiBaoCao")).toLowerCase();
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "bao_cao_" + type + "_" + stamp + "." + extension;
    }
}

@RestControllerAdvice(assignableTypes = StatisticsReportController.class)
class StatisticsReportExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
