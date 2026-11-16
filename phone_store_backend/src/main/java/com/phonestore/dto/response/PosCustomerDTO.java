package com.phonestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosCustomerDTO {
    private String maKhachHang;
    private String hoTen;
    private String soDienThoai;
    private String email;
    private Integer diemTichLuy;
    private String hangKhachHang; // Khach_VIP, Thanh_vien, Khach_moi
}

