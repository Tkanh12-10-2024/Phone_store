
package com.phonestore.bus;

import com.phonestore.dao.KhuyenMaiDAO;
import com.phonestore.model.KhuyenMai;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class KhuyenMaiBUS {

    private final KhuyenMaiDAO khuyenMaiDAO;

    public KhuyenMaiBUS(KhuyenMaiDAO khuyenMaiDAO) {
        this.khuyenMaiDAO = khuyenMaiDAO;
    }

    public List<KhuyenMai> layDanhSach() {
        return khuyenMaiDAO.layDanhSach();
    }

    public List<KhuyenMai> layDanhSachTheoTrangThai(String trangThai) {

        if (trangThai == null || trangThai.isBlank()) {
            throw new IllegalArgumentException(
                    "Trạng thái khuyến mãi không được để trống"
            );
        }

        return khuyenMaiDAO.layDanhSachTheoTrangThai(trangThai);
    }

    public Optional<KhuyenMai> timTheoCodeVoucher(String code) {

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Mã voucher không được để trống"
            );
        }

        return khuyenMaiDAO.timTheoCodeVoucher(code);
    }

    public boolean kiemTraVoucher(
            String code,
            BigDecimal tongTien
    ) {

        if (tongTien == null ||
                tongTien.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }

        Optional<KhuyenMai> optional =
                khuyenMaiDAO.timTheoCodeVoucher(code);

        if (optional.isEmpty()) {
            return false;
        }

        return optional.get()
                .kiemTraHieuLucChoDonHang(tongTien, true);
    }

    @Transactional
    public boolean ghiNhanSuDungVoucher(String maKhuyenMai) {

        if (maKhuyenMai == null || maKhuyenMai.isBlank()) {
            throw new IllegalArgumentException(
                    "Mã khuyến mãi không hợp lệ"
            );
        }

        return khuyenMaiDAO.tangSoLuongDaDung(maKhuyenMai);
    }
}
