
package com.phonestore.dao;

import com.phonestore.dto.pos.PosEmployeeResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PosEmployeeDAO {

    private final JdbcTemplate jdbcTemplate;

    public PosEmployeeDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PosEmployeeResponse> layDanhSachNhanVienHoatDong() {
        return jdbcTemplate.query(
                """
                SELECT nv.ma_nhan_vien, nv.ho_ten
                FROM nhan_vien nv
                JOIN tai_khoan tk ON tk.ma_nhan_vien = nv.ma_nhan_vien
                WHERE nv.trang_thai = 'Dang_lam_viec'
                  AND tk.trang_thai = 'Hoat_dong'
                ORDER BY nv.ho_ten, nv.ma_nhan_vien
                """,
                (rs, rowNum) -> new PosEmployeeResponse(
                        rs.getString("ma_nhan_vien"),
                        rs.getString("ho_ten")
                )
        );
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
                FROM nhan_vien nv
                JOIN tai_khoan tk ON tk.ma_nhan_vien = nv.ma_nhan_vien
                WHERE nv.ma_nhan_vien = ?
                  AND nv.trang_thai = 'Dang_lam_viec'
                  AND tk.trang_thai = 'Hoat_dong'
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
