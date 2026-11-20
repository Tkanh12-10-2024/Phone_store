package com.phonestore.dto.request;

import lombok.Data;

@Data
public class CustomerUpdateRequest {
    private String hoTen;
    private String soDienThoai;
    private String email;
    private String diaChi;
}
