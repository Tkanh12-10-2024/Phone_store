package com.phonestore.dto.customer;

public record CustomerRequest(
        String hoTen,
        String soDienThoai,
        String email,
        String diaChi
) {
}
