package com.phonestore.service;

import com.phonestore.dto.request.EmployeeCreateRequest;
import com.phonestore.dto.request.EmployeeUpdateRequest;
import com.phonestore.dto.response.EmployeeResponse;

import java.util.List;

public interface EmployeeService {
    List<EmployeeResponse> getAllEmployees();
    EmployeeResponse getEmployeeById(Long id);
    EmployeeResponse createEmployee(EmployeeCreateRequest request);
    EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request);
    void deleteEmployee(Long id);
}