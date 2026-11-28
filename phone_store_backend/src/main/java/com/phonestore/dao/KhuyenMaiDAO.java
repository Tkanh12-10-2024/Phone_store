
package com.phonestore.dao;

import com.phonestore.model.KhuyenMai;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class KhuyenMaiDAO {

    private final JdbcTemplate jdbcTemplate;

    public KhuyenMaiDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<KhuyenMai> rowMapper = (rs, rowNum) -> {
        KhuyenMai km = new KhuyenMai();

        km.setMaKhuyenMai(rs.getString("ma_khuyen_mai"));
        km.setCodeVoucher(rs.getString("code_voucher"));
        km.setTenKhuyenMai(rs.getString("ten_khuyen_mai"));
        km.setLoaiGiamGia(rs.getString("loai_giam_gia"));
        km.setGiaTriGiam(rs.getBigDecimal("gia_tri_giam"));
        km.setGiaTriDonHangToiThieu(
                rs.getBigDecimal("gia_tri_don_hang_toi_thieu"));
        km.setGiamToiDa(rs.getBigDecimal("giam_toi_da"));
        km.setSoLuongPhatHanh(rs.getInt("so_luong_phat_hanh"));
        km.setSoLuongDaDung(rs.getInt("so_luong_da_dung"));

        Timestamp batDau = rs.getTimestamp("ngay_bat_dau");
        Timestamp ketThuc = rs.getTimestamp("ngay_ket_thuc");

        if (batDau != null) {
            km.setNgayBatDau(batDau.toLocalDateTime());
        }

        if (ketThuc != null) {
            km.setNgayKetThuc(ketThuc.toLocalDateTime());
        }

        km.setTrangThai(rs.getString("trang_thai"));

        return km;
    };

    public Optional<KhuyenMai> timTheoCodeVoucher(String codeVoucher) {

        if (codeVoucher == null || codeVoucher.isBlank()) {
            return Optional.empty();
        }

        String sql = """
            SELECT *
            FROM khuyen_mai
            WHERE LOWER(code_voucher) = LOWER(?)
        """;

        List<KhuyenMai> result = jdbcTemplate.query(
                sql,
                rowMapper,
                codeVoucher.trim()
        );

        return result.stream().findFirst();
    }

    public Optional<KhuyenMai> timTheoMa(String maKhuyenMai) {

        String sql = """
            SELECT *
            FROM khuyen_mai
            WHERE ma_khuyen_mai = ?
        """;

        List<KhuyenMai> result = jdbcTemplate.query(
                sql,
                rowMapper,
                maKhuyenMai
        );

        return result.stream().findFirst();
    }

    public List<KhuyenMai> layDanhSach() {

        String sql = """
            SELECT *
            FROM khuyen_mai
            ORDER BY ngay_tao DESC
        """;

        return jdbcTemplate.query(sql, rowMapper);
    }

    public List<KhuyenMai> layDanhSachTheoTrangThai(String trangThai) {

        String sql = """
            SELECT *
            FROM khuyen_mai
            WHERE trang_thai = ?
            ORDER BY ngay_tao DESC
        """;

        return jdbcTemplate.query(sql, rowMapper, trangThai);
    }

    public boolean capNhatSoLuongDaDung(KhuyenMai km) {

        if (km == null || km.getMaKhuyenMai() == null) {
            return false;
        }

        String sql = """
            UPDATE khuyen_mai
            SET so_luong_da_dung = ?
            WHERE ma_khuyen_mai = ?
        """;

        return jdbcTemplate.update(
                sql,
                km.getSoLuongDaDung(),
                km.getMaKhuyenMai()
        ) > 0;
    }

    public boolean tangSoLuongDaDung(String maKhuyenMai) {

        String sql = """
            UPDATE khuyen_mai
            SET so_luong_da_dung = so_luong_da_dung + 1
            WHERE ma_khuyen_mai = ?
              AND trang_thai = 'Dang_dien_ra'
              AND (
                  so_luong_phat_hanh <= 0
                  OR so_luong_da_dung < so_luong_phat_hanh
              )
              AND ngay_bat_dau <= CURRENT_TIMESTAMP
              AND ngay_ket_thuc >= CURRENT_TIMESTAMP
        """;

        return jdbcTemplate.update(sql, maKhuyenMai) > 0;
    }
}
