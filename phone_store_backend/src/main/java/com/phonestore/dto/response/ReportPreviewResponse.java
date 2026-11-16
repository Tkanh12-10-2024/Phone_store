package com.phonestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportPreviewResponse {
    private String loaiBaoCao;
    private String tieuDeBaoCao;
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private String phamViLoc;
    private LocalDateTime ngayLap;
    private String nguoiLap;
    private List<String> columns;
    private List<List<String>> rows;
    private Map<String, String> summary;
    private BigDecimal tongDoanhThu;
    private BigDecimal tongChiPhi;
    private BigDecimal tongLoiNhuan;
    private Integer tongSoLuong;
}

