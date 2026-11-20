
package com.phonestore.service;

import com.phonestore.entity.DonHang;
import com.phonestore.entity.HoaDon;

import com.phonestore.repository.DonHangRepository;
import com.phonestore.repository.HoaDonRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ThanhToanService {

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    // Thanh toán đơn hàng
    @Transactional
    public HoaDon thanhToan(
            String maDonHang,
            HoaDon.PhuongThucThanhToan phuongThuc,
            BigDecimal tienKhachDua,
            boolean xacNhanChuyenKhoan) {

        // Kiểm tra đơn hàng tồn tại
        DonHang donHang = donHangRepository.findById(maDonHang)
                .orElseThrow(() ->
                        new IllegalArgumentException("Đơn hàng không tồn tại"));

        // Kiểm tra trạng thái đơn hàng
        if (donHang.getTrangThai() != DonHang.TrangThai.Cho_thanh_toan) {
            throw new IllegalArgumentException(
                    "Đơn hàng không ở trạng thái chờ thanh toán");
        }

        BigDecimal tongThanhToan = donHang.getTongTienCanTra();
        BigDecimal tienThua = BigDecimal.ZERO;

        // Kiểm tra số tiền khách đưa
        if (tienKhachDua == null || tienKhachDua.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số tiền khách đưa không hợp lệ");
        }

        // Xử lý thanh toán tiền mặt
        if (phuongThuc == HoaDon.PhuongThucThanhToan.Tien_mat) {

            if (tienKhachDua.compareTo(tongThanhToan) < 0) {
                throw new IllegalArgumentException(
                        "Số tiền khách đưa không đủ thanh toán");
            }

            tienThua = tienKhachDua.subtract(tongThanhToan);
        }

        // Xử lý chuyển khoản QR
        else if (phuongThuc == HoaDon.PhuongThucThanhToan.Chuyen_khoan_QR) {

            if (!xacNhanChuyenKhoan) {
                throw new IllegalArgumentException(
                        "Chưa xác nhận nhận được tiền chuyển khoản");
            }

            if (tienKhachDua.compareTo(tongThanhToan) < 0) {
                throw new IllegalArgumentException(
                        "Số tiền chuyển khoản không đủ");
            }

            tienThua = tienKhachDua.subtract(tongThanhToan);
        }

        // Xử lý thanh toán bằng thẻ
        else if (phuongThuc == HoaDon.PhuongThucThanhToan.The) {

            if (tienKhachDua.compareTo(tongThanhToan) < 0) {
                throw new IllegalArgumentException(
                        "Số tiền thanh toán không đủ");
            }

            tienThua = tienKhachDua.subtract(tongThanhToan);
        }

        // Tạo hóa đơn thanh toán
        HoaDon hoaDon = new HoaDon();

        hoaDon.setMaHoaDon("HD" +
                UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 28));

        hoaDon.setMaDonHang(donHang.getMaDonHang());
        hoaDon.setMaKhachHang(donHang.getMaKhachHang());
        hoaDon.setMaNhanVien(donHang.getMaNhanVien());
        hoaDon.setNgayLap(LocalDateTime.now());

        hoaDon.setTongTienHang(donHang.getTongTienHang());
        hoaDon.setThueVatPhanTram(new BigDecimal("10.00"));
        hoaDon.setTienThueVat(donHang.getThueVat());
        hoaDon.setTienGiamGia(donHang.getTienGiamGia());
        hoaDon.setTongThanhToan(tongThanhToan);

        hoaDon.setPhuongThucThanhToan(phuongThuc);
        hoaDon.setTienKhachDua(tienKhachDua);
        hoaDon.setTienThua(tienThua);

        hoaDon.setTrangThaiThanhToan(
                HoaDon.TrangThaiThanhToan.Thanh_cong);

        hoaDon.setGhiChu(null);

        hoaDonRepository.save(hoaDon);

        // Cập nhật trạng thái đơn hàng
        donHang.setTrangThai(DonHang.TrangThai.Da_thanh_toan);
        donHangRepository.save(donHang);

        return hoaDon;
    }
}
