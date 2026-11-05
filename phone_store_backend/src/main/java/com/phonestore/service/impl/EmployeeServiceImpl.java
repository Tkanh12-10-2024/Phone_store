package com.phonestore.service.impl;

import com.phonestore.dto.request.EmployeeCreateRequest;
import com.phonestore.dto.request.EmployeeUpdateRequest;
import com.phonestore.dto.response.EmployeeResponse;
import com.phonestore.entity.Employee;
import com.phonestore.repository.EmployeeRepository;
import com.phonestore.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream().map(emp -> {
            EmployeeResponse res = new EmployeeResponse();
            BeanUtils.copyProperties(emp, res);
            return res;
        }).collect(Collectors.toList());
    }

    @Override
    public EmployeeResponse getEmployeeById(Long id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        EmployeeResponse res = new EmployeeResponse();
        BeanUtils.copyProperties(emp, res);
        return res;
    }

    @Override
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new RuntimeException("Mã nhân viên đã tồn tại");
        }
        Employee emp = new Employee();
        BeanUtils.copyProperties(request, emp);
        emp.setStatus("ACTIVE");

        Employee saved = employeeRepository.save(emp);

        EmployeeResponse res = new EmployeeResponse();
        BeanUtils.copyProperties(saved, res);
        return res;
    }

    @Override
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        emp.setFullName(request.getFullName());
        emp.setEmail(request.getEmail());
        emp.setPhone(request.getPhone());
        emp.setPosition(request.getPosition());
        emp.setSalary(request.getSalary());
        emp.setStatus(request.getStatus());

        Employee updated = employeeRepository.save(emp);

        EmployeeResponse res = new EmployeeResponse();
        BeanUtils.copyProperties(updated, res);
        return res;
    }

    @Override
    public void deleteEmployee(Long id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        emp.setStatus("INACTIVE"); // Xóa mềm
        employeeRepository.save(emp);
    }
}