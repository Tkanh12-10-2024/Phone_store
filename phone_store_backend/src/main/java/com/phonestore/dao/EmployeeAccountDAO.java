package com.phonestore.dao;

import com.phonestore.dto.employee.EmployeeAccountRequest;
import com.phonestore.dto.employee.EmployeeAccountResponse;
import com.phonestore.dto.employee.EmployeeValidationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Repository
public class EmployeeAccountDAO {

    private static final String SELECT_EMPLOYEES = """
            SELECT nv.ma_nhan_vien,
                   nv.ho_ten,
                   nv.so_dien_thoai,
                   tk.ten_dang_nhap,
                   vt.ten_vai_tro,
                   CASE
                       WHEN nv.trang_thai = 'Dang_lam_viec'
                        AND tk.trang_thai = 'Hoat_dong' THEN 1
                       ELSE 0
                   END AS active
            FROM nhan_vien nv
            JOIN tai_khoan tk ON tk.ma_nhan_vien = nv.ma_nhan_vien
            JOIN vai_tro vt ON vt.ma_vai_tro = tk.ma_vai_tro
            """;

    private final JdbcTemplate jdbcTemplate;

    public EmployeeAccountDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<EmployeeAccountResponse> layDanhSach() {
        return jdbcTemplate.query(
                SELECT_EMPLOYEES + " ORDER BY nv.ho_ten, nv.ma_nhan_vien",
                (rs, rowNum) -> taoResponse(
                        rs.getString("ma_nhan_vien"),
                        rs.getString("ho_ten"),
                        rs.getString("so_dien_thoai"),
                        rs.getString("ten_dang_nhap"),
                        rs.getString("ten_vai_tro"),
                        rs.getInt("active")
                )
        );
    }

    public EmployeeAccountResponse timTheoMa(String id) {
        List<EmployeeAccountResponse> result = jdbcTemplate.query(
                SELECT_EMPLOYEES + " WHERE nv.ma_nhan_vien = ?",
                (rs, rowNum) -> taoResponse(
                        rs.getString("ma_nhan_vien"),
                        rs.getString("ho_ten"),
                        rs.getString("so_dien_thoai"),
                        rs.getString("ten_dang_nhap"),
                        rs.getString("ten_vai_tro"),
                        rs.getInt("active")
                ),
                id
        );

        if (result.isEmpty()) {
            throw new NoSuchElementException("Không tìm thấy nhân viên.");
        }

        return result.get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public EmployeeAccountResponse taoMoi(EmployeeAccountRequest request) {
        String id = batBuoc(request.getId(), "Mã nhân viên");
        String fullName = batBuoc(request.getFullName(), "Họ tên");
        String phone = batBuoc(request.getPhone(), "Số điện thoại");
        String username = batBuoc(request.getUsername(), "Tên đăng nhập");
        String password = batBuoc(request.getPassword(), "Mật khẩu");
        String role = chuanHoaRole(request.getRole());
        int roleId = timMaVaiTro(role);

        if (id.length() > 20 || phone.length() > 15 || username.length() > 50
                || fullName.length() > 100) {
            throw new EmployeeValidationException("Thông tin nhân viên vượt quá độ dài cho phép.");
        }

        kiemTraMaNhanVienChuaTonTai(id);
        kiemTraTenDangNhapChuaTonTai(username, null);

        jdbcTemplate.update(
                """
                INSERT INTO nhan_vien (
                    ma_nhan_vien, ho_ten, so_dien_thoai,
                    chuc_vu, trang_thai
                )
                VALUES (?, ?, ?, ?, 'Dang_lam_viec')
                """,
                id,
                fullName,
                phone,
                tenChucVu(role)
        );

        jdbcTemplate.update(
                """
                INSERT INTO tai_khoan (
                    ma_nhan_vien, ten_dang_nhap, mat_khau,
                    ma_vai_tro, trang_thai
                )
                VALUES (?, ?, ?, ?, 'Hoat_dong')
                """,
                id,
                username,
                password,
                roleId
        );

        return timTheoMa(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public EmployeeAccountResponse capNhat(String id, EmployeeAccountRequest request) {
        String fullName = batBuoc(request.getFullName(), "Họ tên");
        String phone = batBuoc(request.getPhone(), "Số điện thoại");
        String username = batBuoc(request.getUsername(), "Tên đăng nhập");
        String role = chuanHoaRole(request.getRole());
        int roleId = timMaVaiTro(role);
        int active = request.getActive() == null ? 1 : request.getActive();

        if (fullName.length() > 100 || phone.length() > 15 || username.length() > 50) {
            throw new EmployeeValidationException("Thông tin nhân viên vượt quá độ dài cho phép.");
        }
        if (active != 0 && active != 1) {
            throw new EmployeeValidationException("Trạng thái tài khoản không hợp lệ.");
        }

        timTheoMa(id);
        kiemTraTenDangNhapChuaTonTai(username, id);

        jdbcTemplate.update(
                """
                UPDATE nhan_vien
                SET ho_ten = ?, so_dien_thoai = ?, chuc_vu = ?,
                    trang_thai = ?
                WHERE ma_nhan_vien = ?
                """,
                fullName,
                phone,
                tenChucVu(role),
                active == 1 ? "Dang_lam_viec" : "Da_nghi_viec",
                id
        );

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            jdbcTemplate.update(
                    """
                    UPDATE tai_khoan
                    SET ten_dang_nhap = ?, ma_vai_tro = ?, trang_thai = ?
                    WHERE ma_nhan_vien = ?
                    """,
                    username,
                    roleId,
                    active == 1 ? "Hoat_dong" : "Bi_khoa",
                    id
            );
        } else {
            jdbcTemplate.update(
                    """
                    UPDATE tai_khoan
                    SET ten_dang_nhap = ?, mat_khau = ?, ma_vai_tro = ?,
                        trang_thai = ?
                    WHERE ma_nhan_vien = ?
                    """,
                    username,
                    request.getPassword(),
                    roleId,
                    active == 1 ? "Hoat_dong" : "Bi_khoa",
                    id
            );
        }

        return timTheoMa(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void xoa(String id) {
        timTheoMa(id);
        jdbcTemplate.update(
                "UPDATE tai_khoan SET trang_thai = 'Bi_khoa' WHERE ma_nhan_vien = ?",
                id
        );
        jdbcTemplate.update(
                "UPDATE nhan_vien SET trang_thai = 'Da_nghi_viec' WHERE ma_nhan_vien = ?",
                id
        );
    }

    private EmployeeAccountResponse taoResponse(
            String id,
            String fullName,
            String phone,
            String username,
            String databaseRole,
            int active
    ) {
        return new EmployeeAccountResponse(
                id,
                fullName,
                phone,
                username,
                hienThiRole(databaseRole),
                active
        );
    }

    private String hienThiRole(String databaseRole) {
        String normalized = databaseRole.toLowerCase(Locale.ROOT);
        if (normalized.contains("quan") || normalized.contains("quản")
                || normalized.contains("admin")) {
            return "QUAN_LY";
        }
        if (normalized.contains("banhang") || normalized.contains("bán hàng")) {
            return "NHAN_VIEN";
        }
        return databaseRole;
    }

    private int timMaVaiTro(String role) {
        List<Integer> ids;
        if ("QUAN_LY".equals(role)) {
            ids = jdbcTemplate.query(
                    """
                    SELECT ma_vai_tro
                    FROM vai_tro
                    WHERE LOWER(REPLACE(ten_vai_tro, ' ', '')) IN (?, ?)
                    ORDER BY ma_vai_tro
                    """,
                    (rs, rowNum) -> rs.getInt("ma_vai_tro"),
                    "quanly",
                    "quảnlý"
            );
        } else {
            ids = jdbcTemplate.query(
                    """
                    SELECT ma_vai_tro
                    FROM vai_tro
                    WHERE LOWER(REPLACE(ten_vai_tro, ' ', '')) IN (?, ?)
                    ORDER BY ma_vai_tro
                    """,
                    (rs, rowNum) -> rs.getInt("ma_vai_tro"),
                    "nhanvienbanhang",
                    "nhânviênbánhàng"
            );
        }

        if (ids.isEmpty()) {
            throw new IllegalStateException("Cơ sở dữ liệu chưa khai báo vai trò nhân viên phù hợp.");
        }
        return ids.get(0);
    }

    private String tenChucVu(String role) {
        return "QUAN_LY".equals(role) ? "Quản lý cửa hàng" : "Nhân viên bán hàng";
    }

    private String chuanHoaRole(String role) {
        if (role == null) {
            throw new EmployeeValidationException("Chức vụ không hợp lệ.");
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!"QUAN_LY".equals(normalized) && !"NHAN_VIEN".equals(normalized)) {
            throw new EmployeeValidationException("Chức vụ không hợp lệ.");
        }
        return normalized;
    }

    private String batBuoc(String value, String tenTruong) {
        if (value == null || value.isBlank()) {
            throw new EmployeeValidationException(tenTruong + " không được để trống.");
        }
        return value.trim();
    }

    private void kiemTraMaNhanVienChuaTonTai(String id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM nhan_vien WHERE ma_nhan_vien = ?",
                Integer.class,
                id
        );
        if (count != null && count > 0) {
            throw new EmployeeValidationException("Mã nhân viên đã tồn tại.");
        }
    }

    private void kiemTraTenDangNhapChuaTonTai(String username, String excludeId) {
        String sql = "SELECT COUNT(*) FROM tai_khoan WHERE LOWER(ten_dang_nhap) = LOWER(?)"
                + (excludeId == null ? "" : " AND ma_nhan_vien <> ?");
        Integer count = excludeId == null
                ? jdbcTemplate.queryForObject(sql, Integer.class, username)
                : jdbcTemplate.queryForObject(sql, Integer.class, username, excludeId);
        if (count != null && count > 0) {
            throw new EmployeeValidationException("Tên đăng nhập đã được sử dụng.");
        }
    }
}
