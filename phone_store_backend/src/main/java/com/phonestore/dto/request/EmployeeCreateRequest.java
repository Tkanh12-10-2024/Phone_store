package com.phonestore.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EmployeeCreateRequest {
    private String employeeCode;
    private String fullName;
    private String email;
    private String phone;
    private String position;
    private BigDecimal salary;

    // Thông tin tài khoản đi kèm để nhân viên có thể đăng nhập hệ thống
    private String username;
    private String password;
}