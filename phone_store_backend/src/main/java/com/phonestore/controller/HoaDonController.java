
package com.phonestore.controller;

import com.phonestore.entity.HoaDon;
import com.phonestore.repository.HoaDonRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hoa-don")
@CrossOrigin(origins = "*")
public class HoaDonController {

    @Autowired
    private HoaDonRepository hoaDonRepository;

    // Lấy danh sách hóa đơn
    @GetMapping
    public ResponseEntity<List<HoaDon>> layDanhSachHoaDon() {
        return ResponseEntity.ok(hoaDonRepository.findAll());
    }

    // Lấy hóa đơn theo mã
    @GetMapping("/{maHoaDon}")
    public ResponseEntity<?> layHoaDonTheoMa(
            @PathVariable String maHoaDon) {

        return hoaDonRepository.findById(maHoaDon)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Tìm hóa đơn theo mã đơn hàng
    @GetMapping("/don-hang/{maDonHang}")
    public ResponseEntity<List<HoaDon>> layHoaDonTheoDonHang(
            @PathVariable String maDonHang) {

        List<HoaDon> danhSach =
                hoaDonRepository.findAll().stream()
                        .filter(hoaDon ->
                                hoaDon.getMaDonHang().equals(maDonHang))
                        .toList();

        return ResponseEntity.ok(danhSach);
    }
}
