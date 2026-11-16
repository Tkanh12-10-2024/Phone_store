package com.phonestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportLogDTO {
    private Integer maBaoCao;
    private String loaiBaoCao;
    private String maNhanVienLap;
    private String tenNhanVienLap;
    private LocalDateTime ngayLap;
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private String dinhDangXuat;
    private String duongDanFile;
    private String tieuChiLoc;
    private String ghiChu;
}

