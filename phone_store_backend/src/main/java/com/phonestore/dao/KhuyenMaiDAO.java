
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

    /*
     * Chuyển dữ liệu từ database thành đối tượng KhuyenMai.
     */
    private final RowMapper<KhuyenMai> rowMapper = (rs, rowNum) -> {
        KhuyenMai km = new KhuyenMai();

        km.setMaKhuyenMai(rs.getString("ma_khuyen_mai"));
        km.setCodeVoucher(rs.getString("code_voucher"));
        km.setTenKhuyenMai(rs.getString("ten_khuyen_mai"));
        km.setLoaiGiamGia(rs.getString("loai_giam_gia"));

        km.setGiaTriGiam(rs.getBigDecimal("gia_tri_giam"));
        km.setGiaTriDonHangToiThieu(
                rs.getBigDecimal("gia_tri_don_hang_toi_thieu")
        );
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

        // Điều kiện áp dụng
        km.setMaThuongHieuApDung(
                (Integer) rs.getObject("ma_thuong_hieu_ap_dung")
        );

        km.setDongMayApDung(
                rs.getString("dong_may_ap_dung")
        );

        km.setMaDanhMucApDung(
                (Integer) rs.getObject("ma_danh_muc_ap_dung")
        );

        km.setDoiTuongKhachHang(
                rs.getString("doi_tuong_khach_hang")
        );

        // Thông tin bổ sung
        km.setQuaTang(rs.getString("qua_tang"));
        km.setMoTa(rs.getString("mo_ta"));

        return km;
    };

    /*
     * Tìm voucher bằng mã code.
     */
    public Optional<KhuyenMai> timTheoCodeVoucher(String codeVoucher) {

        if (codeVoucher == null || codeVoucher.isBlank()) {
            return Optional.empty();
        }

        String sql = """
                SELECT *
                FROM khuyen_mai
                WHERE LOWER(code_voucher) = LOWER(?)
                LIMIT 1
                """;

        List<KhuyenMai> result = jdbcTemplate.query(
                sql,
                rowMapper,
                codeVoucher.trim()
        );

        return result.stream().findFirst();
    }

    /*
     * Tìm khuyến mãi bằng mã khuyến mãi.
     */
    public Optional<KhuyenMai> timTheoMa(String maKhuyenMai) {

        if (maKhuyenMai == null || maKhuyenMai.isBlank()) {
            return Optional.empty();
        }

        String sql = """
                SELECT *
                FROM khuyen_mai
                WHERE ma_khuyen_mai = ?
                LIMIT 1
                """;

        List<KhuyenMai> result = jdbcTemplate.query(
                sql,
                rowMapper,
                maKhuyenMai
        );

        return result.stream().findFirst();
    }

    /*
     * Lấy toàn bộ danh sách khuyến mãi.
     */
    public List<KhuyenMai> layDanhSach() {

        String sql = """
                SELECT *
                FROM khuyen_mai
                ORDER BY ngay_tao DESC
                """;

        return jdbcTemplate.query(sql, rowMapper);
    }

    /*
     * Lấy danh sách khuyến mãi theo trạng thái.
     */
    public List<KhuyenMai> layDanhSachTheoTrangThai(String trangThai) {

        String sql = """
                SELECT *
                FROM khuyen_mai
                WHERE trang_thai = ?
                ORDER BY ngay_tao DESC
                """;

        return jdbcTemplate.query(
                sql,
                rowMapper,
                trangThai
        );
    }

    /*
     * Cập nhật số lượt đã sử dụng.
     */
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

    /*
     * Tăng lượt sử dụng voucher một cách nguyên tử.
     *
     * Điều kiện được kiểm tra ngay trong câu UPDATE:
     * - Voucher đang diễn ra.
     * - Chưa hết lượt sử dụng.
     * - Đã đến ngày bắt đầu.
     * - Chưa qua ngày kết thúc.
     *
     * Tránh trường hợp nhiều giao dịch cùng sử dụng
     * voucher vượt quá số lượng phát hành.
     */
    public boolean tangSoLuongDaDung(String maKhuyenMai) {

        if (maKhuyenMai == null || maKhuyenMai.isBlank()) {
            return false;
        }

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
