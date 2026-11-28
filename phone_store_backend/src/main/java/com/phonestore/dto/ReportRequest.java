package com.phonestore.dto;

import java.time.LocalDate;

public record ReportRequest(
        String loaiBaoCao,
        LocalDate tuNgay,
        LocalDate denNgay,
        Integer maThuongHieu,
        String maNhanVien,
        String dinhDangXuat,
        String ghiChu
) {
}
