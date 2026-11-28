package com.phonestore.dao;

import com.phonestore.model.KhuyenMai;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class KhuyenMaiDAO {

    public Optional<KhuyenMai> timTheoCodeVoucher(String codeVoucher) {
        if (codeVoucher == null || codeVoucher.isBlank()) {
            return Optional.empty();
        }
        // TODO: Truy vấn bảng khuyen_mai theo codeVoucher (không phân biệt hoa thường)
        return Optional.empty();
    }

    public List<KhuyenMai> layDanhSachTheoTrangThai(String trangThai) {
        // TODO: Truy vấn danh sách khuyến mãi theo trangThai (vd: Dang_dien_ra)
        return List.of();
    }

    public KhuyenMai capNhatSoLuongDaDung(KhuyenMai khuyenMai) {
        // TODO: Lưu cập nhật soLuongDaDung của khuyến mãi xuống CSDL
        return khuyenMai;
    }
}