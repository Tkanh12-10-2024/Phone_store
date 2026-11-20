
package com.phonestore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "gio_hang")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GioHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_gio_hang")
    private Integer maGioHang;

    @Column(name = "ma_nhan_vien", nullable = false, length = 20)
    private String maNhanVien;

    @Column(name = "ma_khach_hang", length = 20)
    private String maKhachHang;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;
}
