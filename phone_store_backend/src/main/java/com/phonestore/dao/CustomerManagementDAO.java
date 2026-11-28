package com.phonestore.dao;

import com.phonestore.dto.customer.CustomerHistoryResponse;
import com.phonestore.dto.customer.CustomerRequest;
import com.phonestore.dto.customer.CustomerResponse;
import com.phonestore.dto.customer.CustomerValidationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Repository
public class CustomerManagementDAO {

    private static final String CUSTOMER_SELECT = """
            SELECT kh.ma_khach_hang, kh.ho_ten, kh.so_dien_thoai,
                   kh.email, kh.dia_chi, kh.diem_tich_luy, kh.ngay_tao,
                   kh.trang_thai,
                   (SELECT COUNT(*) FROM hoa_don hd
                    WHERE hd.ma_khach_hang = kh.ma_khach_hang) AS so_hoa_don,
                   (SELECT COUNT(*) FROM phieu_bao_hanh pb
                    WHERE pb.ma_khach_hang = kh.ma_khach_hang) AS so_phieu_bao_hanh,
                   (SELECT COUNT(*) FROM don_hang dh
                    WHERE dh.ma_khach_hang = kh.ma_khach_hang) AS so_don_hang
            FROM khach_hang kh
            """;

    private final JdbcTemplate jdbcTemplate;

    public CustomerManagementDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CustomerResponse> layDanhSach(String keyword, String trangThai) {
        StringBuilder sql = new StringBuilder(CUSTOMER_SELECT);
        if (hasText(keyword)) {
            sql.append("""
                     WHERE (
                         LOWER(kh.ma_khach_hang) LIKE ?
                         OR LOWER(kh.ho_ten) LIKE ?
                         OR LOWER(kh.so_dien_thoai) LIKE ?
                         OR LOWER(COALESCE(kh.email, '')) LIKE ?
                     )
                    """);
        }
        if (hasText(trangThai)) {
            sql.append(hasText(keyword) ? " AND" : " WHERE")
                    .append(" kh.trang_thai = ?");
        }
        sql.append(" ORDER BY kh.ngay_tao DESC, kh.ma_khach_hang");

        if (hasText(keyword) && hasText(trangThai)) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            return jdbcTemplate.query(sql.toString(), this::mapCustomer,
                    pattern, pattern, pattern, pattern, trangThai.trim());
        }
        if (hasText(keyword)) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            return jdbcTemplate.query(sql.toString(), this::mapCustomer,
                    pattern, pattern, pattern, pattern);
        }
        if (hasText(trangThai)) {
            return jdbcTemplate.query(sql.toString(), this::mapCustomer, trangThai.trim());
        }
        return jdbcTemplate.query(sql.toString(), this::mapCustomer);
    }

    public CustomerResponse timTheoMa(String id) {
        List<CustomerResponse> customers = jdbcTemplate.query(
                CUSTOMER_SELECT + " WHERE kh.ma_khach_hang = ?",
                this::mapCustomer,
                id
        );
        if (customers.isEmpty()) {
            throw new NoSuchElementException("Không tìm thấy khách hàng.");
        }
        return customers.get(0);
    }

    public List<CustomerHistoryResponse> layLichSuMuaHang(String id) {
        timTheoMa(id);
        return jdbcTemplate.query(
                """
                SELECT hd.ma_hoa_don, sp.ten_san_pham, cthd.don_gia,
                       hd.ngay_lap, cthd.ma_imei,
                       cthd.thoi_gian_bao_hanh_thang, im.han_bao_hanh
                FROM hoa_don hd
                JOIN chi_tiet_hoa_don cthd ON cthd.ma_hoa_don = hd.ma_hoa_don
                JOIN san_pham sp ON sp.ma_san_pham = cthd.ma_san_pham
                LEFT JOIN imei_san_pham im ON im.ma_imei = cthd.ma_imei
                WHERE hd.ma_khach_hang = ?
                ORDER BY hd.ngay_lap DESC, hd.ma_hoa_don DESC, cthd.ma_chi_tiet_hd
                """,
                (rs, rowNum) -> mapHistory(rs),
                id
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public CustomerResponse taoMoi(CustomerRequest request) {
        ValidatedCustomer customer = validate(request);
        kiemTraSoDienThoaiChuaTonTai(customer.phone(), null);
        String id = taoMaKhachHang();

        jdbcTemplate.update(
                """
                INSERT INTO khach_hang (
                    ma_khach_hang, ho_ten, so_dien_thoai,
                    email, dia_chi, diem_tich_luy, trang_thai
                )
                VALUES (?, ?, ?, ?, ?, 0, 'Hoat_dong')
                """,
                id,
                customer.name(),
                customer.phone(),
                customer.email(),
                customer.address()
        );
        return timTheoMa(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public CustomerResponse capNhat(String id, CustomerRequest request) {
        timTheoMa(id);
        ValidatedCustomer customer = validate(request);
        kiemTraSoDienThoaiChuaTonTai(customer.phone(), id);

        jdbcTemplate.update(
                """
                UPDATE khach_hang
                SET ho_ten = ?, so_dien_thoai = ?, email = ?, dia_chi = ?
                WHERE ma_khach_hang = ?
                """,
                customer.name(),
                customer.phone(),
                customer.email(),
                customer.address(),
                id
        );
        return timTheoMa(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public DeleteCustomerResult xoa(String id) {
        CustomerResponse customer = timTheoMa(id);
        boolean hasHistory = customer.soDonHang() > 0
                || customer.soHoaDon() > 0
                || customer.soPhieuBaoHanh() > 0;
        if (hasHistory) {
            jdbcTemplate.update(
                    "UPDATE khach_hang SET trang_thai = 'Ngung_hoat_dong' WHERE ma_khach_hang = ?",
                    id
            );
            return new DeleteCustomerResult(
                    "DEACTIVATED",
                    "Khách hàng có lịch sử giao dịch nên đã được vô hiệu hóa."
            );
        }
        jdbcTemplate.update("DELETE FROM khach_hang WHERE ma_khach_hang = ?", id);
        return new DeleteCustomerResult("DELETED", "Đã xóa khách hàng.");
    }

    private CustomerResponse mapCustomer(ResultSet rs, int rowNum) throws SQLException {
        int points = rs.getInt("diem_tich_luy");
        Timestamp createdAt = rs.getTimestamp("ngay_tao");
        return new CustomerResponse(
                rs.getString("ma_khach_hang"),
                rs.getString("ho_ten"),
                rs.getString("so_dien_thoai"),
                rs.getString("email"),
                rs.getString("dia_chi"),
                points,
                xacDinhHangKhachHang(points),
                createdAt == null ? null : createdAt.toLocalDateTime(),
                rs.getString("trang_thai"),
                rs.getInt("so_hoa_don"),
                rs.getInt("so_phieu_bao_hanh"),
                rs.getInt("so_don_hang")
        );
    }

    private CustomerHistoryResponse mapHistory(ResultSet rs) throws SQLException {
        Timestamp saleTimestamp = rs.getTimestamp("ngay_lap");
        java.sql.Date warrantyDate = rs.getDate("han_bao_hanh");
        return new CustomerHistoryResponse(
                rs.getString("ma_hoa_don"),
                rs.getString("ten_san_pham"),
                rs.getBigDecimal("don_gia"),
                saleTimestamp == null ? null : saleTimestamp.toLocalDateTime(),
                rs.getString("ma_imei"),
                rs.getInt("thoi_gian_bao_hanh_thang"),
                warrantyDate == null ? null : warrantyDate.toLocalDate()
        );
    }

    private ValidatedCustomer validate(CustomerRequest request) {
        if (request == null) {
            throw new CustomerValidationException("Thiếu thông tin khách hàng.");
        }
        String name = required(request.hoTen(), "Họ tên");
        if (name.length() > 100) {
            throw new CustomerValidationException("Họ tên không được vượt quá 100 ký tự.");
        }

        String phone = normalizePhone(request.soDienThoai());
        String email = emptyToNull(request.email());
        String address = emptyToNull(request.diaChi());
        if (email != null && (email.length() > 100
                || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            throw new CustomerValidationException("Email không hợp lệ hoặc vượt quá 100 ký tự.");
        }
        if (address != null && address.length() > 255) {
            throw new CustomerValidationException("Địa chỉ không được vượt quá 255 ký tự.");
        }
        return new ValidatedCustomer(name, phone, email, address);
    }

    private String normalizePhone(String value) {
        String phone = required(value, "Số điện thoại");
        if (phone.length() > 25 || !phone.matches("[+0-9().\\-\\s]+")) {
            throw new CustomerValidationException("Số điện thoại không hợp lệ.");
        }
        String normalized = phone.replaceAll("[().\\-\\s]", "");
        String digits = normalized.startsWith("+") ? normalized.substring(1) : normalized;
        if (!digits.matches("[0-9]{7,15}") || normalized.length() > 15) {
            throw new CustomerValidationException("Số điện thoại phải có từ 7 đến 15 chữ số.");
        }
        return normalized;
    }

    private void kiemTraSoDienThoaiChuaTonTai(String phone, String excludedId) {
        String sql = "SELECT COUNT(*) FROM khach_hang WHERE so_dien_thoai = ?";
        if (excludedId != null) {
            sql += " AND ma_khach_hang <> ?";
        }
        Integer count = excludedId == null
                ? jdbcTemplate.queryForObject(sql, Integer.class, phone)
                : jdbcTemplate.queryForObject(sql, Integer.class, phone, excludedId);
        if (count != null && count > 0) {
            throw new CustomerValidationException("Số điện thoại đã được đăng ký cho khách hàng khác.");
        }
    }

    private String taoMaKhachHang() {
        List<String> existingIds = jdbcTemplate.queryForList(
                "SELECT ma_khach_hang FROM khach_hang",
                String.class
        );
        BigInteger maxSequence = BigInteger.ZERO;
        for (String id : existingIds) {
            if (id != null && id.matches("KH[0-9]+")) {
                BigInteger sequence = new BigInteger(id.substring(2));
                maxSequence = maxSequence.max(sequence);
            }
        }
        String number = maxSequence.add(BigInteger.ONE).toString();
        String nextId = "KH" + "0".repeat(Math.max(0, 3 - number.length())) + number;
        if (nextId.length() > 20) {
            throw new CustomerValidationException("Không thể sinh mã khách hàng mới.");
        }
        return nextId;
    }

    private String xacDinhHangKhachHang(int points) {
        if (points >= 1000) {
            return "Khach_VIP";
        }
        if (points >= 200) {
            return "Thanh_vien";
        }
        return "Khach_moi";
    }

    private String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new CustomerValidationException(label + " không được để trống.");
        }
        return value.trim();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ValidatedCustomer(
            String name,
            String phone,
            String email,
            String address
    ) {
    }

    public record DeleteCustomerResult(String action, String message) {
    }
}
