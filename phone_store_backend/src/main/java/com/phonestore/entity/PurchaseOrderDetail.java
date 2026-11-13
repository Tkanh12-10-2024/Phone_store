package com.phonestore.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chi_tiet_phieu_nhap")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_chi_tiet_nhap")
    private Long maChiTietNhap;

    @Column(name = "ma_phieu_nhap", nullable = false, length = 30)
    private String maPhieuNhap;

    @Column(name = "ma_san_pham", nullable = false, length = 20)
    private String maSanPham;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "don_gia_nhap", nullable = false)
    private Double donGiaNhap;

    @Column(name = "thanh_tien", nullable = false)
    private Double thanhTien;
}
