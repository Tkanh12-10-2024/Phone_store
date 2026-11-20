
package com.phonestore.service;

import com.phonestore.entity.ChiTietGioHang;
import com.phonestore.entity.SanPham;
import com.phonestore.repository.ChiTietGioHangRepository;
import com.phonestore.repository.SanPhamRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ChiTietGioHangService {

    @Autowired
    private ChiTietGioHangRepository chiTietGioHangRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    // Thêm sản phẩm vào giỏ hàng
    public ChiTietGioHang themSanPham(ChiTietGioHang chiTiet) {

        // Kiểm tra số lượng hợp lệ
        if (chiTiet.getSoLuong() == null || chiTiet.getSoLuong() <= 0) {
            throw new IllegalArgumentException(
                    "Số lượng sản phẩm phải lớn hơn 0"
            );
        }

        // Kiểm tra sản phẩm tồn tại
        SanPham sanPham = sanPhamRepository.findById(
                chiTiet.getMaSanPham()
        ).orElseThrow(() ->
                new IllegalArgumentException("Sản phẩm không tồn tại")
        );

        // Kiểm tra trạng thái kinh doanh
        if (sanPham.getTrangThai() != SanPham.TrangThai.Dang_kinh_doanh) {
            throw new IllegalArgumentException(
                    "Sản phẩm hiện không kinh doanh"
            );
        }

        // Tìm sản phẩm đã có trong giỏ hàng
        Optional<ChiTietGioHang> chiTietTonTai =
                chiTietGioHangRepository.findByMaGioHangAndMaSanPham(
                        chiTiet.getMaGioHang(),
                        chiTiet.getMaSanPham()
                );

        int soLuongMoi = chiTiet.getSoLuong();

        if (chiTietTonTai.isPresent()) {
            ChiTietGioHang chiTietCu = chiTietTonTai.get();

            soLuongMoi += chiTietCu.getSoLuong();

            // Kiểm tra tổng số lượng sau khi cộng
            if (soLuongMoi > sanPham.getSoLuongTon()) {
                throw new IllegalArgumentException(
                        "Số lượng tồn kho không đủ. Số lượng còn lại: "
                                + sanPham.getSoLuongTon()
                );
            }

            chiTietCu.setSoLuong(soLuongMoi);

            return chiTietGioHangRepository.save(chiTietCu);
        }

        // Kiểm tra tồn kho khi thêm mới
        if (soLuongMoi > sanPham.getSoLuongTon()) {
            throw new IllegalArgumentException(
                    "Số lượng tồn kho không đủ. Số lượng còn lại: "
                            + sanPham.getSoLuongTon()
            );
        }

        // Lấy giá bán từ database
        chiTiet.setDonGia(sanPham.getGiaBan());

        return chiTietGioHangRepository.save(chiTiet);
    }

    // Tìm chi tiết giỏ hàng theo mã
    public Optional<ChiTietGioHang> timChiTietTheoMa(Integer maChiTiet) {
        return chiTietGioHangRepository.findById(maChiTiet);
    }

    // Lấy danh sách sản phẩm trong giỏ hàng
    public List<ChiTietGioHang> layDanhSachChiTiet() {
        return chiTietGioHangRepository.findAll();
    }

    // Cập nhật số lượng sản phẩm trong giỏ hàng
    public ChiTietGioHang capNhatChiTiet(ChiTietGioHang chiTiet) {

        // Kiểm tra số lượng hợp lệ
        if (chiTiet.getSoLuong() == null || chiTiet.getSoLuong() <= 0) {
            throw new IllegalArgumentException(
                    "Số lượng sản phẩm phải lớn hơn 0"
            );
        }

        // Kiểm tra chi tiết giỏ hàng tồn tại
        ChiTietGioHang chiTietCu = chiTietGioHangRepository
                .findById(chiTiet.getMaChiTietGioHang())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Chi tiết giỏ hàng không tồn tại"
                        )
                );

        // Lấy thông tin sản phẩm
        SanPham sanPham = sanPhamRepository
                .findById(chiTietCu.getMaSanPham())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Sản phẩm không tồn tại"
                        )
                );

        // Kiểm tra tồn kho
        if (chiTiet.getSoLuong() > sanPham.getSoLuongTon()) {
            throw new IllegalArgumentException(
                    "Số lượng tồn kho không đủ. Số lượng còn lại: "
                            + sanPham.getSoLuongTon()
            );
        }

        // Chỉ cập nhật số lượng, giữ nguyên thông tin còn lại
        chiTietCu.setSoLuong(chiTiet.getSoLuong());

        return chiTietGioHangRepository.save(chiTietCu);
    }

    // Tính tổng tiền giỏ hàng
    public BigDecimal tinhTongTien(Integer maGioHang) {

        List<ChiTietGioHang> danhSach =
                chiTietGioHangRepository.findByMaGioHang(maGioHang);

        BigDecimal tongTien = BigDecimal.ZERO;

        for (ChiTietGioHang chiTiet : danhSach) {
            BigDecimal thanhTien = chiTiet.getDonGia()
                    .multiply(BigDecimal.valueOf(chiTiet.getSoLuong()));

            tongTien = tongTien.add(thanhTien);
        }

        return tongTien;
    }

    // Xóa sản phẩm khỏi giỏ hàng
    public void xoaSanPham(Integer maChiTiet) {
        chiTietGioHangRepository.deleteById(maChiTiet);
    }
}
