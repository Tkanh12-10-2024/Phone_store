package com.phonestore.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "san_pham")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @Column(name = "ma_san_pham", nullable = false, length = 20)
    private String maSanPham;

    @Column(name = "ten_san_pham", nullable = false, length = 150)
    private String tenSanPham;

    @Column(name = "dong_may", nullable = false, length = 100)
    private String dongMay;

    @Column(name = "mau_sac", nullable = false, length = 50)
    private String mauSac;

    @Column(name = "ram", nullable = false, length = 20)
    private String ram;

    @Column(name = "rom", nullable = false, length = 20)
    private String rom;

    @Column(name = "gia_nhap_tham_khao")
    @Builder.Default
    private Double giaNhapThamKhao = 0.0;

    @Column(name = "gia_ban")
    @Builder.Default
    private Double giaBan = 0.0;

    @Column(name = "so_luong_ton")
    @Builder.Default
    private Integer soLuongTon = 0;

    @Column(name = "dinh_muc_ton_toi_thieu")
    @Builder.Default
    private Integer dinhMucTonToiThieu = 2;

    @Column(name = "trang_thai", nullable = false, length = 30)
    @Builder.Default
    private String trangThai = "Dang_kinh_doanh";

    @Column(name = "hinh_anh")
    private String hinhAnh;
}
