package com.phonestore.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "nhan_vien")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_dang_nhap", nullable = false, unique = true)
    private String username;

    @Column(name = "mat_khau", nullable = false)
    private String password;

    @Column(name = "ho_ten", nullable = false)
    private String fullName;

    @Column(name = "chuc_vu")
    private String role; // MANAGER, STAFF

    @Column(name = "trang_thai")
    private Boolean active = true;

    public Employee() {}

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}