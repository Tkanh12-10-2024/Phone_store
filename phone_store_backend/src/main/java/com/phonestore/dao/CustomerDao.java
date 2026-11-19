package com.phonestore.dao;

import com.phonestore.dto.request.CustomerCreateRequest;
import com.phonestore.dto.request.CustomerUpdateRequest;
import com.phonestore.dto.response.CustomerDTO;
import com.phonestore.dto.response.PurchaseHistoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CustomerDao {

    private final DataSource dataSource;

    // ========= UC 2.12.1 - Tìm kiếm khách hàng =========
    public List<CustomerDTO> findAll(String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT kh.ma_khach_hang, kh.ho_ten, kh.so_dien_thoai, kh.email,
                       kh.dia_chi, kh.diem_tich_luy, kh.ngay_tao, kh.trang_thai,
                       COUNT(DISTINCT hd.ma_hoa_don) AS so_hoa_don,
                       COUNT(DISTINCT pbh.ma_phieu_bao_hanh) AS so_phieu_bao_hanh
                FROM khach_hang kh
                LEFT JOIN hoa_don hd ON hd.ma_khach_hang = kh.ma_khach_hang
                LEFT JOIN phieu_bao_hanh pbh ON pbh.ma_khach_hang = kh.ma_khach_hang
                """);
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                    WHERE (LOWER(kh.ho_ten) LIKE ? OR kh.so_dien_thoai LIKE ? OR LOWER(kh.email) LIKE ?)
                    """);
            String p = "%" + keyword.trim().toLowerCase() + "%";
            params.add(p);
            params.add(p);
            params.add(p);
        }
        sql.append(" GROUP BY kh.ma_khach_hang ORDER BY kh.ngay_tao DESC");

        List<CustomerDTO> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoJdbcHelper.bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapCustomer(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm kiếm khách hàng: " + e.getMessage(), e);
        }
        return result;
    }

    // ========= UC 2.12.1 - Tìm theo ID =========
    public Optional<CustomerDTO> findById(String maKhachHang) {
        String sql = """
                SELECT kh.ma_khach_hang, kh.ho_ten, kh.so_dien_thoai, kh.email,
                       kh.dia_chi, kh.diem_tich_luy, kh.ngay_tao, kh.trang_thai,
                       COUNT(DISTINCT hd.ma_hoa_don) AS so_hoa_don,
                       COUNT(DISTINCT pbh.ma_phieu_bao_hanh) AS so_phieu_bao_hanh
                FROM khach_hang kh
                LEFT JOIN hoa_don hd ON hd.ma_khach_hang = kh.ma_khach_hang
                LEFT JOIN phieu_bao_hanh pbh ON pbh.ma_khach_hang = kh.ma_khach_hang
                WHERE kh.ma_khach_hang = ?
                GROUP BY kh.ma_khach_hang
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maKhachHang);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapCustomer(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm khách hàng theo ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ========= UC 2.12.2 - Thêm mới khách hàng =========
    public CustomerDTO create(CustomerCreateRequest req) {
        String newId = generateNewId();
        String sql = """
                INSERT INTO khach_hang (ma_khach_hang, ho_ten, so_dien_thoai, email, dia_chi,
                                        diem_tich_luy, ngay_tao, trang_thai)
                VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP, 'Hoat_dong')
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newId);
            ps.setString(2, req.getHoTen());
            ps.setString(3, req.getSoDienThoai());
            ps.setString(4, req.getEmail());
            ps.setString(5, req.getDiaChi());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage().contains("Unique") || e.getMessage().contains("unique") || e.getMessage().contains("UNIQUE")) {
                throw new RuntimeException("Số điện thoại hoặc email đã tồn tại trong hệ thống.");
            }
            throw new RuntimeException("Lỗi thêm khách hàng: " + e.getMessage(), e);
        }
        return findById(newId).orElseThrow();
    }

    // ========= UC 2.12.3 - Cập nhật thông tin khách hàng =========
    public CustomerDTO update(String id, CustomerUpdateRequest req) {
        String sql = """
                UPDATE khach_hang SET ho_ten=?, so_dien_thoai=?, email=?, dia_chi=?
                WHERE ma_khach_hang=?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, req.getHoTen());
            ps.setString(2, req.getSoDienThoai());
            ps.setString(3, req.getEmail());
            ps.setString(4, req.getDiaChi());
            ps.setString(5, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new RuntimeException("Không tìm thấy khách hàng: " + id);
        } catch (SQLException e) {
            if (e.getMessage().contains("Unique") || e.getMessage().contains("unique") || e.getMessage().contains("UNIQUE")) {
                throw new RuntimeException("Số điện thoại hoặc email đã tồn tại trong hệ thống.");
            }
            throw new RuntimeException("Lỗi cập nhật khách hàng: " + e.getMessage(), e);
        }
        return findById(id).orElseThrow();
    }

    // ========= UC 2.12.4 - Xóa / Vô hiệu hóa khách hàng =========
    public String deactivateOrDelete(String id) {
        boolean hasHistory = hasTransactionHistory(id);
        if (hasHistory) {
            // Soft delete - vô hiệu hóa
            String sql = "UPDATE khach_hang SET trang_thai='Ngung_hoat_dong' WHERE ma_khach_hang=?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Lỗi vô hiệu hóa khách hàng: " + e.getMessage(), e);
            }
            return "DEACTIVATED";
        } else {
            // Hard delete - không có lịch sử
            String sql = "DELETE FROM khach_hang WHERE ma_khach_hang=?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Lỗi xóa khách hàng: " + e.getMessage(), e);
            }
            return "DELETED";
        }
    }

    // ========= UC 2.12.5 - Xem lịch sử mua hàng =========
    public List<PurchaseHistoryDTO> findPurchaseHistory(String maKhachHang) {
        String sql = """
                SELECT hd.ma_hoa_don, hd.ngay_lap,
                       sp.ma_san_pham, sp.ten_san_pham,
                       cthd.ma_imei, cthd.don_gia, cthd.thoi_gian_bao_hanh_thang,
                       im.han_bao_hanh, im.trang_thai AS trang_thai_imei
                FROM hoa_don hd
                JOIN chi_tiet_hoa_don cthd ON cthd.ma_hoa_don = hd.ma_hoa_don
                JOIN san_pham sp ON sp.ma_san_pham = cthd.ma_san_pham
                LEFT JOIN imei_san_pham im ON im.ma_imei = cthd.ma_imei
                WHERE hd.ma_khach_hang = ?
                ORDER BY hd.ngay_lap DESC
                """;
        List<PurchaseHistoryDTO> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maKhachHang);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(PurchaseHistoryDTO.builder()
                            .maHoaDon(rs.getString("ma_hoa_don"))
                            .ngayMua(DaoJdbcHelper.toLocalDateTime(rs.getTimestamp("ngay_lap")))
                            .maSanPham(rs.getString("ma_san_pham"))
                            .tenSanPham(rs.getString("ten_san_pham"))
                            .maImei(rs.getString("ma_imei"))
                            .giaBan(DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("don_gia")))
                            .thoiGianBaoHanhThang(rs.getInt("thoi_gian_bao_hanh_thang"))
                            .hanBaoHanh(DaoJdbcHelper.toLocalDate(rs.getDate("han_bao_hanh")))
                            .trangThaiBaoHanh(rs.getString("trang_thai_imei"))
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lịch sử mua hàng: " + e.getMessage(), e);
        }
        return result;
    }

    // ========= Helpers =========

    private boolean hasTransactionHistory(String maKhachHang) {
        String sql = """
                SELECT (SELECT COUNT(*) FROM hoa_don WHERE ma_khach_hang=?) +
                       (SELECT COUNT(*) FROM phieu_bao_hanh WHERE ma_khach_hang=?) AS total
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maKhachHang);
            ps.setString(2, maKhachHang);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("total") > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private String generateNewId() {
        String sql = "SELECT ma_khach_hang FROM khach_hang WHERE ma_khach_hang LIKE 'KH%' ORDER BY ma_khach_hang DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String last = rs.getString(1); // e.g. KH004
                int num = Integer.parseInt(last.substring(2)) + 1;
                return String.format("KH%03d", num);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return "KH001";
    }

    private String computeHang(int diem) {
        if (diem >= 1000) return "VIP";
        if (diem >= 500) return "Vàng";
        if (diem >= 200) return "Bạc";
        return "Thường";
    }

    private CustomerDTO mapCustomer(ResultSet rs) throws SQLException {
        BigDecimal diem = DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("diem_tich_luy"));
        return CustomerDTO.builder()
                .maKhachHang(rs.getString("ma_khach_hang"))
                .hoTen(rs.getString("ho_ten"))
                .soDienThoai(rs.getString("so_dien_thoai"))
                .email(rs.getString("email"))
                .diaChi(rs.getString("dia_chi"))
                .diemTichLuy(diem)
                .hangKhachHang(computeHang(diem.intValue()))
                .ngayTao(DaoJdbcHelper.toLocalDateTime(rs.getTimestamp("ngay_tao")))
                .trangThai(rs.getString("trang_thai"))
                .soHoaDon(rs.getLong("so_hoa_don"))
                .soPhieuBaoHanh(rs.getLong("so_phieu_bao_hanh"))
                .build();
    }
}
