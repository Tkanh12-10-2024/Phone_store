
package com.phonestore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "san_pham")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SanPham {

    @Id
    @Column(name = "ma_san_pham", length = 20)
    private String maSanPham;

    @Column(name = "ten_san_pham", nullable = false, length = 150)
    private String tenSanPham;

    @Column(name = "ma_thuong_hieu", nullable = false)
    private Integer maThuongHieu;

    @Column(name = "ma_danh_muc", nullable = false)
    private Integer maDanhMuc;

    @Column(name = "dong_may", length = 100)
    private String dongMay;

    @Column(name = "mau_sac", length = 50)
    private String mauSac;

    @Column(name = "ram", length = 20)
    private String ram;

    @Column(name = "rom", length = 20)
    private String rom;

    @Column(name = "man_hinh", length = 100)
    private String manHinh;

    @Column(name = "chip_xu_ly", length = 100)
    private String chipXuLy;

    @Column(name = "pin", length = 50)
    private String pin;

    @Column(name = "camera", length = 100)
    private String camera;

    @Column(name = "he_dieu_hanh", length = 50)
    private String heDieuHanh;

    @Column(name = "gia_nhap_tham_khao", precision = 14, scale = 2)
    private BigDecimal giaNhapThamKhao;

    @Column(name = "gia_ban", precision = 14, scale = 2)
    private BigDecimal giaBan;

    @Column(name = "thoi_gian_bao_hanh_thang")
    private Integer thoiGianBaoHanhThang;

    @Column(name = "so_luong_ton")
    private Integer soLuongTon;

    @Column(name = "dinh_muc_ton_toi_thieu")
    private Integer dinhMucTonToiThieu;

    @Column(name = "hinh_anh", length = 255)
    private String hinhAnh;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThai trangThai;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    public enum TrangThai {
        Dang_kinh_doanh,
        Ngung_kinh_doanh
    }
}
