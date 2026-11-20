package com.phonestore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "don_hang")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DonHang {

    @Id
    @Column(name = "ma_don_hang", length = 30)
    private String maDonHang;

    @Column(name = "ma_khach_hang", length = 20)
    private String maKhachHang;

    @Column(name = "ma_nhan_vien", nullable = false, length = 20)
    private String maNhanVien;

    @Column(name = "ma_khuyen_mai", length = 30)
    private String maKhuyenMai;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;

    @Column(name = "tong_tien_hang", nullable = false, precision = 14, scale = 2)
    private BigDecimal tongTienHang;

    @Column(name = "tien_giam_gia", nullable = false, precision = 14, scale = 2)
    private BigDecimal tienGiamGia;

    @Column(name = "thue_vat", nullable = false, precision = 14, scale = 2)
    private BigDecimal thueVat;

    @Column(name = "tong_tien_can_tra", nullable = false, precision = 14, scale = 2)
    private BigDecimal tongTienCanTra;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThai trangThai;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    public enum TrangThai {
        Cho_thanh_toan,
        Da_thanh_toan,
        Da_huy
    }
}