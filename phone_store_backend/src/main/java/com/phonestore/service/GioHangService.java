
package com.phonestore.service;

import com.phonestore.entity.GioHang;
import com.phonestore.repository.GioHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GioHangService {

    @Autowired
    private GioHangRepository gioHangRepository;

    // Tạo giỏ hàng
    public GioHang taoGioHang(GioHang gioHang) {
        return gioHangRepository.save(gioHang);
    }

    // Tìm giỏ hàng theo mã
    public Optional<GioHang> timGioHangTheoMa(Integer maGioHang) {
        return gioHangRepository.findById(maGioHang);
    }

    // Lấy danh sách giỏ hàng
    public java.util.List<GioHang> layDanhSachGioHang() {
        return gioHangRepository.findAll();
    }

    // Xóa giỏ hàng
    public void xoaGioHang(Integer maGioHang) {
        gioHangRepository.deleteById(maGioHang);
    }
}
