package com.phonestore.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EmployeeResponse {
    private Long id;
    private String employeeCode;
    private String fullName;
    private String email;
    private String phone;
    private String position;
    private BigDecimal salary;
    private String status;
}