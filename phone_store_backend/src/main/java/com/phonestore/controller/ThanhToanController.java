
package com.phonestore.controller;

import com.phonestore.entity.HoaDon;
import com.phonestore.service.ThanhToanService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/thanh-toan")
@CrossOrigin(origins = "*")
public class ThanhToanController {

    @Autowired
    private ThanhToanService thanhToanService;

    // Thanh toán đơn hàng
    @PostMapping("/{maDonHang}")
    public ResponseEntity<?> thanhToan(
            @PathVariable String maDonHang,
            @RequestParam HoaDon.PhuongThucThanhToan phuongThuc,
            @RequestParam BigDecimal tienKhachDua,
            @RequestParam(defaultValue = "false") boolean xacNhanChuyenKhoan) {

        try {
            HoaDon hoaDon = thanhToanService.thanhToan(
                    maDonHang,
                    phuongThuc,
                    tienKhachDua,
                    xacNhanChuyenKhoan
            );

            return ResponseEntity.ok(hoaDon);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", ex.getMessage())
            );
        }
    }
}
