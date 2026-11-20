
package com.phonestore.controller;

import com.phonestore.entity.ChiTietGioHang;
import com.phonestore.service.ChiTietGioHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/chi-tiet-gio-hang")
@CrossOrigin(origins = "*")
public class ChiTietGioHangController {

    @Autowired
    private ChiTietGioHangService chiTietGioHangService;

    // Lấy danh sách chi tiết giỏ hàng
    @GetMapping
    public ResponseEntity<List<ChiTietGioHang>> layDanhSachChiTiet() {
        return ResponseEntity.ok(
                chiTietGioHangService.layDanhSachChiTiet()
        );
    }

    // Tìm chi tiết theo mã
    @GetMapping("/{maChiTiet}")
    public ResponseEntity<ChiTietGioHang> timChiTietTheoMa(
            @PathVariable Integer maChiTiet) {

        return chiTietGioHangService.timChiTietTheoMa(maChiTiet)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Thêm sản phẩm vào giỏ hàng
    @PostMapping
    public ResponseEntity<ChiTietGioHang> themSanPham(
            @RequestBody ChiTietGioHang chiTiet) {

        return ResponseEntity.ok(
                chiTietGioHangService.themSanPham(chiTiet)
        );
    }

    // Cập nhật chi tiết giỏ hàng
    @PutMapping("/{maChiTiet}")
    public ResponseEntity<ChiTietGioHang> capNhatChiTiet(
            @PathVariable Integer maChiTiet,
            @RequestBody ChiTietGioHang chiTiet) {

        if (chiTietGioHangService.timChiTietTheoMa(maChiTiet).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        chiTiet.setMaChiTietGioHang(maChiTiet);

        return ResponseEntity.ok(
                chiTietGioHangService.capNhatChiTiet(chiTiet)
        );
    }

    // Xóa sản phẩm khỏi giỏ hàng
    @DeleteMapping("/{maChiTiet}")
    public ResponseEntity<Void> xoaSanPham(
            @PathVariable Integer maChiTiet) {

        if (chiTietGioHangService.timChiTietTheoMa(maChiTiet).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        chiTietGioHangService.xoaSanPham(maChiTiet);
        return ResponseEntity.noContent().build();
    }

    // Tính tổng tiền giỏ hàng
    @GetMapping("/tong-tien/{maGioHang}")
    public ResponseEntity<BigDecimal> tinhTongTien(
            @PathVariable Integer maGioHang) {

        return ResponseEntity.ok(
                chiTietGioHangService.tinhTongTien(maGioHang)
        );
    }

    // Xử lý lỗi nghiệp vụ
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> xuLyLoiNghiepVu(
            IllegalArgumentException ex) {

        return ResponseEntity.badRequest().body(
                Map.of("message", ex.getMessage())
        );
    }
}
