package com.phonestore.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EmployeeUpdateRequest {
    private String fullName;
    private String email;
    private String phone;
    private String position;
    private BigDecimal salary;
    private String status; // ACTIVE, INACTIVE
}