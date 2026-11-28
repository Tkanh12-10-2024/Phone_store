
package com.phonestore.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PosEmployeeDAO {

    private final JdbcTemplate jdbcTemplate;

    public PosEmployeeDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Kiểm tra nhân viên có tồn tại và đang hoạt động hay không.
     */
    public boolean kiemTraNhanVienHoatDong(String maNhanVien) {

        if (maNhanVien == null || maNhanVien.isBlank()) {
            return false;
        }

        String sql = """
                SELECT COUNT(*)
                FROM nhan_vien
                WHERE ma_nhan_vien = ?
                  AND trang_thai = 'Dang_lam_viec'
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                maNhanVien
        );

        return count != null && count > 0;
    }

    /**
     * Lấy họ tên nhân viên.
     */
    public String layTenNhanVien(String maNhanVien) {

        String sql = """
                SELECT ho_ten
                FROM nhan_vien
                WHERE ma_nhan_vien = ?
                """;

        return jdbcTemplate.query(
                sql,
                rs -> rs.next() ? rs.getString("ho_ten") : null,
                maNhanVien
        );
    }
}
