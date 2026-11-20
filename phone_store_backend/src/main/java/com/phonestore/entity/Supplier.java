package com.phonestore.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nha_cung_cap")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @Column(name = "ma_nha_cung_cap", nullable = false, length = 20)
    private String maNhaCungCap;

    @Column(name = "ten_nha_cung_cap", nullable = false, length = 150)
    private String tenNhaCungCap;

    @Column(name = "so_dien_thoai", nullable = false, length = 15)
    private String soDienThoai;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "dia_chi", nullable = false, length = 255)
    private String diaChi;

    @Column(name = "ma_so_thue", length = 30)
    private String maSoThue;

    @Column(name = "cong_no")
    @Builder.Default
    private Double congNo = 0.0;

    @Column(name = "trang_thai", nullable = false, length = 20)
    @Builder.Default
    private String trangThai = "Dang_hop_tac"; // Dang_hop_tac, Ngung_hop_tac
}
