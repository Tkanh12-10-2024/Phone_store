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
        return employeeRepository.findById(id);
    }

    public Employee saveEmployee(Employee employee) {
        if (employee.getId() == null || employee.getId().trim().isEmpty()) {
            long count = employeeRepository.count();
            String generatedId = String.format("NV%03d", count + 1);

            while (employeeRepository.existsById(generatedId)) {
                count++;
                generatedId = String.format("NV%03d", count + 1);
            }
            employee.setId(generatedId);
        }
        return employeeRepository.save(employee);
    }

    public void deleteEmployee(String id) {
        employeeRepository.deleteById(id);
    }
    }