package com.phonestore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "phieu_kiem_ke")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCheck {

    @Id
    @Column(name = "ma_phieu_kiem_ke", nullable = false, length = 30)
    private String maPhieuKiemKe;

    @Column(name = "ma_nhan_vien", nullable = false, length = 20)
    private String maNhanVien;

    @Column(name = "ngay_kiem_ke", nullable = false)
    @Builder.Default
    private LocalDateTime ngayKiemKe = LocalDateTime.now();

    @Column(name = "trang_thai", nullable = false, length = 20)
    @Builder.Default
    private String trangThai = "Da_can_bang"; // Da_can_bang, Cho_xu_ly

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;
}
