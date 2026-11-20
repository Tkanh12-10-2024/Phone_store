
package com.phonestore.service;

import com.phonestore.entity.GioHang;
import com.phonestore.entity.ChiTietGioHang;
import com.phonestore.entity.DonHang;
import com.phonestore.entity.ChiTietDonHang;

import com.phonestore.repository.GioHangRepository;
import com.phonestore.repository.ChiTietGioHangRepository;
import com.phonestore.repository.DonHangRepository;
import com.phonestore.repository.ChiTietDonHangRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DonHangService {

    @Autowired
    private GioHangRepository gioHangRepository;

    @Autowired
    private ChiTietGioHangRepository chiTietGioHangRepository;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;

    // Tạo đơn hàng từ giỏ hàng
    @Transactional
    public DonHang taoDonHang(
            Integer maGioHang,
            String maKhachHang,
            String ghiChu) {

        // Kiểm tra giỏ hàng tồn tại
        GioHang gioHang = gioHangRepository.findById(maGioHang)
                .orElseThrow(() ->
                        new IllegalArgumentException("Giỏ hàng không tồn tại"));

        // Lấy danh sách sản phẩm trong giỏ
        List<ChiTietGioHang> danhSachChiTiet =
                chiTietGioHangRepository.findByMaGioHang(maGioHang);

        if (danhSachChiTiet.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng chưa có sản phẩm");
        }

        // Tính tổng tiền hàng
        BigDecimal tongTienHang = BigDecimal.ZERO;

        for (ChiTietGioHang chiTiet : danhSachChiTiet) {
            BigDecimal thanhTien = chiTiet.getDonGia()
                    .multiply(BigDecimal.valueOf(chiTiet.getSoLuong()));

            tongTienHang = tongTienHang.add(thanhTien);
        }

        // Tính tiền giảm giá và thuế VAT
        BigDecimal tienGiamGia = BigDecimal.ZERO;

        BigDecimal thueVat = tongTienHang
                .multiply(new BigDecimal("0.10"));

        // Tính tổng tiền cần trả
        BigDecimal tongTienCanTra = tongTienHang
                .subtract(tienGiamGia)
                .add(thueVat);

        // Tạo đơn hàng
        DonHang donHang = new DonHang();

        donHang.setMaDonHang("DH" +
                UUID.randomUUID().toString().replace("-", "").substring(0, 28));

        donHang.setMaKhachHang(maKhachHang);
        donHang.setMaNhanVien(gioHang.getMaNhanVien());
        donHang.setMaKhuyenMai(null);
        donHang.setNgayTao(LocalDateTime.now());

        donHang.setTongTienHang(tongTienHang);
        donHang.setTienGiamGia(tienGiamGia);
        donHang.setThueVat(thueVat);
        donHang.setTongTienCanTra(tongTienCanTra);

        donHang.setTrangThai(DonHang.TrangThai.Cho_thanh_toan);
        donHang.setGhiChu(ghiChu);

        donHangRepository.save(donHang);

        // Sao chép sản phẩm từ giỏ hàng sang chi tiết đơn hàng
        for (ChiTietGioHang chiTiet : danhSachChiTiet) {

            ChiTietDonHang chiTietDonHang = new ChiTietDonHang();

            chiTietDonHang.setMaDonHang(donHang.getMaDonHang());
            chiTietDonHang.setMaSanPham(chiTiet.getMaSanPham());
            chiTietDonHang.setSoLuong(chiTiet.getSoLuong());
            chiTietDonHang.setDonGia(chiTiet.getDonGia());

            // Tính thành tiền
            chiTietDonHang.setThanhTien(
                    chiTiet.getDonGia()
                            .multiply(BigDecimal.valueOf(chiTiet.getSoLuong()))
            );

            chiTietDonHangRepository.save(chiTietDonHang);
        }

        return donHang;
    }
}
