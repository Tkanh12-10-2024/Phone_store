package com.phonestore.controller;

import com.phonestore.entity.DonHang;
import com.phonestore.service.DonHangService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/don-hang")
@CrossOrigin(origins = "*")
public class DonHangController {

    @Autowired
    private DonHangService donHangService;

    // Tạo đơn hàng từ giỏ hàng
    @PostMapping
    public ResponseEntity<DonHang> taoDonHang(
            @RequestParam Integer maGioHang,
            @RequestParam(required = false) String maKhachHang,
            @RequestParam(required = false) String ghiChu) {

        return ResponseEntity.ok(
                donHangService.taoDonHang(
                        maGioHang,
                        maKhachHang,
                        ghiChu
                )
        );
    }
}