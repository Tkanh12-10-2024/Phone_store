
package com.phonestore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "chi_tiet_gio_hang")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChiTietGioHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_chi_tiet_gio_hang")
    private Integer maChiTietGioHang;

    @Column(name = "ma_gio_hang", nullable = false)
    private Integer maGioHang;

    @Column(name = "ma_san_pham", nullable = false, length = 20)
    private String maSanPham;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "don_gia", nullable = false, precision = 14, scale = 2)
    private BigDecimal donGia;
}
