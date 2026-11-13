package com.phonestore.service;

import com.phonestore.dto.request.LoginRequest;
import com.phonestore.dto.response.LoginResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    public LoginResponse login(LoginRequest request) {
        // In ra console của IntelliJ xem dữ liệu từ trình duyệt gửi lên là gì
        System.out.println("--> Request nhan duoc: username = [" + request.getUsername() + "], password = [" + request.getPassword() + "]");

        // Test cứng tài khoản admin / 123456
        if ("admin".equals(request.getUsername()) && "123456".equals(request.getPassword())) {
            String token = "SESSION_" + UUID.randomUUID().toString();
            return new LoginResponse(
                    token,
                    request.getUsername(),
                    "Quản Trị Viên (Test)",
                    "QUAN_LY"
            );
        }

        throw new RuntimeException("Tài khoản hoặc mật khẩu không chính xác!");
    }
}