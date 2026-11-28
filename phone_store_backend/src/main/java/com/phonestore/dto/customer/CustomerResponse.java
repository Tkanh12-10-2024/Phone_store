package com.phonestore.dto.customer;

import java.time.LocalDateTime;

public record CustomerResponse(
        String maKhachHang,
        String hoTen,
        String soDienThoai,
        String email,
        String diaChi,
        int diemTichLuy,
        String hangKhachHang,
        LocalDateTime ngayTao,
        String trangThai,
        int soHoaDon,
        int soPhieuBaoHanh,
        int soDonHang
) {
}
