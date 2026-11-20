package com.phonestore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "imei_san_pham")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImei {

    @Id
    @Column(name = "ma_imei", nullable = false, length = 30)
    private String maImei; // 15 chữ số IMEI chuẩn GSM

    @Column(name = "ma_san_pham", nullable = false, length = 20)
    private String maSanPham;

    @Column(name = "ma_phieu_nhap", nullable = false, length = 30)
    private String maPhieuNhap;

    @Column(name = "ma_hoa_don", length = 30)
    private String maHoaDon;

    @Column(name = "trang_thai", nullable = false, length = 30)
    @Builder.Default
    private String trangThai = "Trong_kho"; // Trong_kho, Da_ban, Dang_bao_hanh, Loi_doi_tra, Da_tra_NCC

    @Column(name = "ngay_nhap")
    @Builder.Default
    private LocalDateTime ngayNhap = LocalDateTime.now();

    @Column(name = "ngay_ban")
    private LocalDateTime ngayBan;

    @Column(name = "han_bao_hanh")
    private LocalDate hanBaoHanh;
}
