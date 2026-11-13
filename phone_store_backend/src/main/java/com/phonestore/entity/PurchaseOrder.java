package com.phonestore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "phieu_nhap")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    @Id
    @Column(name = "ma_phieu_nhap", nullable = false, length = 30)
    private String maPhieuNhap;

    @Column(name = "ma_nha_cung_cap", nullable = false, length = 20)
    private String maNhaCungCap;

    @Column(name = "ma_nhan_vien", nullable = false, length = 20)
    private String maNhanVien;

    @Column(name = "ngay_nhap", nullable = false)
    @Builder.Default
    private LocalDateTime ngayNhap = LocalDateTime.now();

    @Column(name = "tong_tien")
    @Builder.Default
    private Double tongTien = 0.0;

    @Column(name = "da_thanh_toan")
    @Builder.Default
    private Double daThanhToan = 0.0;

    @Column(name = "cong_no_con_lai")
    @Builder.Default
    private Double congNoConLai = 0.0;

    @Column(name = "trang_thai", nullable = false, length = 20)
    @Builder.Default
    private String trangThai = "Hoan_thanh"; // Cho_duyet, Hoan_thanh, Da_huy

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;
}
