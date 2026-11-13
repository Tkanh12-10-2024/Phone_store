package com.phonestore.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chi_tiet_kiem_ke")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCheckDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_chi_tiet_kiem_ke")
    private Long maChiTietKiemKe;

    @Column(name = "ma_phieu_kiem_ke", nullable = false, length = 30)
    private String maPhieuKiemKe;

    @Column(name = "ma_san_pham", nullable = false, length = 20)
    private String maSanPham;

    @Column(name = "so_luong_he_thong", nullable = false)
    private Integer soLuongHeThong;

    @Column(name = "so_luong_thuc_te", nullable = false)
    private Integer soLuongThucTe;

    @Column(name = "chenh_lech", nullable = false)
    private Integer chenhLech;

    @Column(name = "ly_do_chenh_lech", length = 255)
    private String lyDoChenhLech;
}
