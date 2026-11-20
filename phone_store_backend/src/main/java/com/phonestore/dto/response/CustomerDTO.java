package com.phonestore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerDTO {
    private String maKhachHang;
    private String hoTen;
    private String soDienThoai;
    private String email;
    private String diaChi;
    private BigDecimal diemTichLuy;
    private String hangKhachHang;
    private LocalDateTime ngayTao;
    private String trangThai;
    private Long soPhieuBaoHanh;
    private Long soHoaDon;
}
