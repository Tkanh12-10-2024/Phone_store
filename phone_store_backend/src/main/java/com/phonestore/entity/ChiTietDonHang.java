package com.phonestore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "chi_tiet_don_hang")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChiTietDonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_chi_tiet_don")
    private Integer maChiTietDon;

    @Column(name = "ma_don_hang", nullable = false, length = 30)
    private String maDonHang;

    @Column(name = "ma_san_pham", nullable = false, length = 20)
    private String maSanPham;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "don_gia", nullable = false, precision = 14, scale = 2)
    private BigDecimal donGia;

    @Column(name = "thanh_tien", precision = 38, scale = 2)
    private BigDecimal thanhTien;
}