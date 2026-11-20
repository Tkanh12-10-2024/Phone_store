
package com.phonestore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hoa_don")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HoaDon {

    @Id
    @Column(name = "ma_hoa_don", length = 30)
    private String maHoaDon;

    @Column(name = "ma_don_hang", length = 30, nullable = false)
    private String maDonHang;

    @Column(name = "ma_khach_hang", length = 20)
    private String maKhachHang;

    @Column(name = "ma_nhan_vien", length = 20, nullable = false)
    private String maNhanVien;

    @Column(name = "ngay_lap", nullable = false)
    private LocalDateTime ngayLap;

    @Column(name = "tong_tien_hang", precision = 14, scale = 2, nullable = false)
    private BigDecimal tongTienHang;

    @Column(name = "thue_vat_phan_tram", precision = 5, scale = 2)
    private BigDecimal thueVatPhanTram;

    @Column(name = "tien_thue_vat", precision = 14, scale = 2)
    private BigDecimal tienThueVat;

    @Column(name = "tien_giam_gia", precision = 14, scale = 2)
    private BigDecimal tienGiamGia;

    @Column(name = "tong_thanh_toan", precision = 14, scale = 2, nullable = false)
    private BigDecimal tongThanhToan;

    @Enumerated(EnumType.STRING)
    @Column(name = "phuong_thuc_thanh_toan", nullable = false)
    private PhuongThucThanhToan phuongThucThanhToan;

    @Column(name = "tien_khach_dua", precision = 14, scale = 2, nullable = false)
    private BigDecimal tienKhachDua;

    @Column(name = "tien_thua", precision = 14, scale = 2)
    private BigDecimal tienThua;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_thanh_toan", nullable = false)
    private TrangThaiThanhToan trangThaiThanhToan;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    public enum PhuongThucThanhToan {
        Tien_mat,
        Chuyen_khoan_QR,
        The
    }

    public enum TrangThaiThanhToan {
        Thanh_cong,
        That_bai,
        Da_hoan_tien
    }
}
