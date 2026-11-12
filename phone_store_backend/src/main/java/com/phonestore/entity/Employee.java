package com.phonestore.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "nhan_vien")
public class Employee {

    @Id
    @Column(name = "ma_nhan_vien", nullable = false, length = 20)
    private String id; // Sử dụng ma_nhan_vien làm khóa chính kiểu String (VD: NV001)

    @Column(name = "ho_ten", nullable = false)
    private String fullName;

    @Column(name = "gioi_tinh")
    private String gender;

    @Column(name = "ngay_sinh")
    private LocalDate birthDate;

    @Column(name = "so_dien_thoai")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "dia_chi")
    private String address;

    @Column(name = "chuc_vu")
    private String role;

    @Column(name = "ngay_vao_lam")
    private LocalDate hireDate;

    @Column(name = "luong_co_ban")
    private Double basicSalary;

    @Column(name = "trang_thai")
    private Integer active; // Trong DB đang lưu số 1 hoặc 0

    @Column(name = "mat_khau")
    private String password;

    @Column(name = "ten_dang_nhap")
    private String username;

    public Employee() {}

    // Getters and Setters đầy đủ
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getActive() { return active; }
    public void setActive(Integer active) { this.active = active; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}