package com.phonestore.service;

import com.phonestore.entity.Employee;
import com.phonestore.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Optional<Employee> getEmployeeById(String id) {
        return employeeRepository.findById(Long.valueOf(id));
    }

    public void deleteEmployee(String id) {
        employeeRepository.deleteById(Long.valueOf(id));
    }

    public Employee saveEmployee(Employee employee) {
        // Nếu là thêm mới (id chưa có hoặc trống), tự động sinh mã NV00x tiếp theo
        if (employee.getId() == null || employee.getId().trim().isEmpty()) {
            long count = employeeRepository.count();
            String generatedId = String.format("NV%03d", count + 1);

            // Đảm bảo không bị trùng lặp nếu có mã bị xóa trước đó (Truyền String trực tiếp)
            while (employeeRepository.existsById(Long.valueOf(generatedId))) {
                count++;
                generatedId = String.format("NV%03d", count + 1);
            }
            employee.setId(generatedId);
        }
        return employeeRepository.save(employee);
    }
}