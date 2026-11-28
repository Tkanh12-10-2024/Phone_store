package com.phonestore.dao;

import com.phonestore.model.KhuyenMai;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class KhuyenMaiDAO {

    private final JdbcTemplate jdbcTemplate;

    public KhuyenMaiDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<KhuyenMai> rowMapper = (rs, rowNum) -> {
        ResultSetMetaData metadata = rs.getMetaData();
        Set<String> columns = new LinkedHashSet<>();
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            columns.add(metadata.getColumnLabel(i).toLowerCase(Locale.ROOT));
        }

        KhuyenMai km = new KhuyenMai();
        km.setMaKhuyenMai(rs.getString("ma_khuyen_mai"));
        km.setCodeVoucher(rs.getString("code_voucher"));
        km.setTenKhuyenMai(rs.getString("ten_khuyen_mai"));
        km.setLoaiGiamGia(rs.getString("loai_giam_gia"));
        km.setGiaTriGiam(rs.getBigDecimal("gia_tri_giam"));
        km.setGiaTriDonHangToiThieu(rs.getBigDecimal("gia_tri_don_hang_toi_thieu"));
        km.setGiamToiDa(rs.getBigDecimal("giam_toi_da"));
        km.setSoLuongPhatHanh(rs.getInt("so_luong_phat_hanh"));
        km.setSoLuongDaDung(rs.getInt("so_luong_da_dung"));
        km.setNgayBatDau(toLocalDateTime(rs.getTimestamp("ngay_bat_dau")));
        km.setNgayKetThuc(toLocalDateTime(rs.getTimestamp("ngay_ket_thuc")));
        km.setTrangThai(rs.getString("trang_thai"));

        if (columns.contains("ma_thuong_hieu_ap_dung")) {
            km.setMaThuongHieuApDung(nullableInt(rs.getObject("ma_thuong_hieu_ap_dung")));
        }
        if (columns.contains("dong_may_ap_dung")) {
            km.setDongMayApDung(rs.getString("dong_may_ap_dung"));
        }
        if (columns.contains("ma_danh_muc_ap_dung")) {
            km.setMaDanhMucApDung(nullableInt(rs.getObject("ma_danh_muc_ap_dung")));
        }
        if (columns.contains("doi_tuong_khach_hang")) {
            km.setDoiTuongKhachHang(rs.getString("doi_tuong_khach_hang"));
        }
        if (columns.contains("qua_tang")) {
            km.setQuaTang(rs.getString("qua_tang"));
        }
        if (columns.contains("mo_ta")) {
            km.setMoTa(rs.getString("mo_ta"));
        }
        return km;
    };

    public Optional<KhuyenMai> timTheoCodeVoucher(String codeVoucher) {
        if (codeVoucher == null || codeVoucher.isBlank()) {
            return Optional.empty();
        }
        List<KhuyenMai> result = jdbcTemplate.query("""
                SELECT *
                FROM khuyen_mai
                WHERE LOWER(code_voucher) = LOWER(?)
                LIMIT 1
                """, rowMapper, codeVoucher.trim());
        return result.stream().findFirst();
    }

    public Optional<KhuyenMai> timTheoMa(String maKhuyenMai) {
        if (maKhuyenMai == null || maKhuyenMai.isBlank()) {
            return Optional.empty();
        }
        List<KhuyenMai> result = jdbcTemplate.query("""
                SELECT *
                FROM khuyen_mai
                WHERE ma_khuyen_mai = ?
                LIMIT 1
                """, rowMapper, maKhuyenMai);
        return result.stream().findFirst();
    }

    public List<KhuyenMai> layDanhSach() {
        return jdbcTemplate.query("""
                SELECT *
                FROM khuyen_mai
                ORDER BY ngay_bat_dau DESC, ma_khuyen_mai
                """, rowMapper);
    }

    public List<KhuyenMai> layDanhSachTheoTrangThai(String trangThai) {
        return jdbcTemplate.query("""
                SELECT *
                FROM khuyen_mai
                WHERE trang_thai = ?
                ORDER BY ngay_bat_dau DESC, ma_khuyen_mai
                """, rowMapper, trangThai);
    }

    public boolean daTonTaiCodeVoucher(String codeVoucher, String excludeId) {
        if (codeVoucher == null || codeVoucher.isBlank()) {
            return false;
        }
        String sql = """
                SELECT COUNT(*)
                FROM khuyen_mai
                WHERE LOWER(code_voucher) = LOWER(?)
                """ + (excludeId == null ? "" : " AND ma_khuyen_mai <> ?");
        Integer count = excludeId == null
                ? jdbcTemplate.queryForObject(sql, Integer.class, codeVoucher.trim())
                : jdbcTemplate.queryForObject(sql, Integer.class, codeVoucher.trim(), excludeId);
        return count != null && count > 0;
    }

    public boolean taoKhuyenMai(KhuyenMai km) {
        Map<String, Object> values = commonValues(km);
        values.put("ma_khuyen_mai", "KM" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        values.put("so_luong_da_dung", 0);

        Set<String> columns = getColumns();
        values.keySet().removeIf(column -> !columns.contains(column));
        String names = String.join(", ", values.keySet());
        String placeholders = String.join(", ", values.keySet().stream().map(c -> "?").toList());
        String sql = "INSERT INTO khuyen_mai (" + names + ") VALUES (" + placeholders + ")";
        return jdbcTemplate.update(sql, values.values().toArray()) == 1;
    }

    public boolean capNhatKhuyenMai(String maKhuyenMai, KhuyenMai km) {
        Map<String, Object> values = commonValues(km);
        values.remove("so_luong_da_dung");
        Set<String> columns = getColumns();
        values.keySet().removeIf(column -> !columns.contains(column));
        if (values.isEmpty()) {
            return false;
        }

        List<Object> parameters = new ArrayList<>();
        List<String> assignments = new ArrayList<>();
        values.forEach((column, value) -> {
            assignments.add(column + " = ?");
            parameters.add(value);
        });
        parameters.add(maKhuyenMai);
        String sql = "UPDATE khuyen_mai SET " + String.join(", ", assignments)
                + " WHERE ma_khuyen_mai = ?";
        return jdbcTemplate.update(sql, parameters.toArray()) > 0;
    }

    public boolean capNhatTrangThai(String maKhuyenMai, String trangThai) {
        return jdbcTemplate.update("""
                UPDATE khuyen_mai
                SET trang_thai = ?
                WHERE ma_khuyen_mai = ?
                """, trangThai, maKhuyenMai) > 0;
    }

    public int demDonHangDaSuDung(String maKhuyenMai) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM don_hang
                WHERE ma_khuyen_mai = ?
                """, Integer.class, maKhuyenMai);
        return count == null ? 0 : count;
    }

    public boolean xoaKhuyenMai(String maKhuyenMai) {
        return jdbcTemplate.update("""
                DELETE FROM khuyen_mai
                WHERE ma_khuyen_mai = ?
                """, maKhuyenMai) > 0;
    }

    /*
     * The original MySQL schema predates some promotion fields that exist in
     * the H2 schema and the additive promotion migration. Keep POS reads
     * compatible with either schema, and only write columns that are present.
     */
    private Set<String> getColumns() {
        return jdbcTemplate.query("SELECT * FROM khuyen_mai WHERE 1 = 0", rs -> {
            Set<String> columns = new LinkedHashSet<>();
            ResultSetMetaData metadata = rs.getMetaData();
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                columns.add(metadata.getColumnLabel(i).toLowerCase(Locale.ROOT));
            }
            return columns;
        });
    }

    private Map<String, Object> commonValues(KhuyenMai km) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code_voucher", km.getCodeVoucher());
        values.put("ten_khuyen_mai", km.getTenKhuyenMai());
        values.put("loai_giam_gia", km.getLoaiGiamGia());
        values.put("gia_tri_giam", km.getGiaTriGiam());
        values.put("gia_tri_don_hang_toi_thieu", km.getGiaTriDonHangToiThieu());
        values.put("giam_toi_da", km.getGiamToiDa());
        values.put("qua_tang", km.getQuaTang());
        values.put("so_luong_phat_hanh", km.getSoLuongPhatHanh());
        values.put("so_luong_da_dung", km.getSoLuongDaDung());
        values.put("ngay_bat_dau", km.getNgayBatDau());
        values.put("ngay_ket_thuc", km.getNgayKetThuc());
        values.put("ma_thuong_hieu_ap_dung", km.getMaThuongHieuApDung());
        values.put("dong_may_ap_dung", km.getDongMayApDung());
        values.put("ma_danh_muc_ap_dung", km.getMaDanhMucApDung());
        values.put("doi_tuong_khach_hang", km.getDoiTuongKhachHang());
        values.put("mo_ta", km.getMoTa());
        values.put("trang_thai", km.getTrangThai());
        return values;
    }

    private static java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static Integer nullableInt(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    public boolean capNhatSoLuongDaDung(KhuyenMai km) {
        if (km == null || km.getMaKhuyenMai() == null) {
            return false;
        }
        return jdbcTemplate.update("""
                UPDATE khuyen_mai
                SET so_luong_da_dung = ?
                WHERE ma_khuyen_mai = ?
                """, km.getSoLuongDaDung(), km.getMaKhuyenMai()) > 0;
    }

    public boolean tangSoLuongDaDung(String maKhuyenMai) {
        if (maKhuyenMai == null || maKhuyenMai.isBlank()) {
            return false;
        }
        return jdbcTemplate.update("""
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
                """, maKhuyenMai) > 0;
    }
}
