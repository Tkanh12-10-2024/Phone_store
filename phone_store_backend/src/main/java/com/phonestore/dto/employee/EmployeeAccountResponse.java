package com.phonestore.dto.employee;

public class EmployeeAccountResponse {

    private final String id;
    private final String fullName;
    private final String phone;
    private final String username;
    private final String role;
    private final int active;

    public EmployeeAccountResponse(
            String id,
            String fullName,
            String phone,
            String username,
            String role,
            int active
    ) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.username = username;
        this.role = role;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public int getActive() {
        return active;
    }
}
