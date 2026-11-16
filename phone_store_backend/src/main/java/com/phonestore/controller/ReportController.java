package com.phonestore.controller;

import com.phonestore.dto.request.ReportGenerateRequest;
import com.phonestore.dto.response.ReportLogDTO;
import com.phonestore.dto.response.ReportPreviewResponse;
import com.phonestore.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportService reportService;

    @PostMapping("/preview")
    public ResponseEntity<ReportPreviewResponse> previewReport(@RequestBody ReportGenerateRequest request) {
        return ResponseEntity.ok(reportService.generatePreview(request));
    }

    @PostMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(@RequestBody ReportGenerateRequest request) {
        ReportService.ExportedReport exported = reportService.exportExcel(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(EXCEL_MEDIA_TYPE);
        headers.setContentDisposition(ContentDisposition.attachment().filename(exported.filename(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(exported.bytes());
    }

    @PostMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestBody ReportGenerateRequest request) {
        ReportService.ExportedReport exported = reportService.exportPdf(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(exported.filename(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(exported.bytes());
    }

    @GetMapping("/history")
    public ResponseEntity<List<ReportLogDTO>> getReportHistory(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(reportService.getReportLogs(limit));
    }
}

