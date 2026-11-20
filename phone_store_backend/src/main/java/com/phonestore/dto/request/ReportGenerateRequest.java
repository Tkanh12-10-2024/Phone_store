package com.phonestore.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReportGenerateRequest {
    private String loaiBaoCao; // Doanh_thu, Kiem_ke_kho, SanPham_BanChay, Ton_kho, Tong_hop, Bao_hanh
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private Integer maThuongHieu;
    private String maNhanVien;
    private String dinhDangXuat; // EXCEL, PDF
    private String ghiChu;
}

