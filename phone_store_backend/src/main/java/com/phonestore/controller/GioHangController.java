
package com.phonestore.controller;

import com.phonestore.entity.GioHang;
import com.phonestore.service.GioHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gio-hang")
@CrossOrigin(origins = "*")
public class GioHangController {

    @Autowired
    private GioHangService gioHangService;

    // Lấy danh sách giỏ hàng
    @GetMapping
    public ResponseEntity<List<GioHang>> layDanhSachGioHang() {
        return ResponseEntity.ok(gioHangService.layDanhSachGioHang());
    }

    // Tìm giỏ hàng theo mã
    @GetMapping("/{maGioHang}")
    public ResponseEntity<GioHang> timGioHangTheoMa(
            @PathVariable Integer maGioHang) {

        return gioHangService.timGioHangTheoMa(maGioHang)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Tạo giỏ hàng
    @PostMapping
    public ResponseEntity<GioHang> taoGioHang(
            @RequestBody GioHang gioHang) {

        return ResponseEntity.ok(gioHangService.taoGioHang(gioHang));
    }

    // Xóa giỏ hàng
    @DeleteMapping("/{maGioHang}")
    public ResponseEntity<Void> xoaGioHang(
            @PathVariable Integer maGioHang) {

        if (gioHangService.timGioHangTheoMa(maGioHang).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        gioHangService.xoaGioHang(maGioHang);
        return ResponseEntity.noContent().build();
    }
}
