package com.phonestore.dao;

import com.phonestore.dto.request.BrandRequest;
import com.phonestore.dto.response.BrandDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BrandDao {

    private final DataSource dataSource;

    public List<BrandDTO> findAll(String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT th.ma_thuong_hieu, th.ten_thuong_hieu, th.quoc_gia, th.logo_url, th.mo_ta,
                       (SELECT COUNT(*) FROM san_pham sp WHERE sp.ma_thuong_hieu = th.ma_thuong_hieu) AS so_luong_san_pham
                FROM thuong_hieu th
                """);
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                     WHERE (LOWER(th.ten_thuong_hieu) LIKE ?
                        OR LOWER(COALESCE(th.quoc_gia, '')) LIKE ?
                        OR LOWER(COALESCE(th.mo_ta, '')) LIKE ?)
                    """);
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        sql.append(" ORDER BY th.ma_thuong_hieu ASC");

        List<BrandDTO> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoJdbcHelper.bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tải danh sách thương hiệu: " + e.getMessage(), e);
        }
        return result;
    }

    public Optional<BrandDTO> findById(int maThuongHieu) {
        String sql = """
                SELECT th.ma_thuong_hieu, th.ten_thuong_hieu, th.quoc_gia, th.logo_url, th.mo_ta,
                       (SELECT COUNT(*) FROM san_pham sp WHERE sp.ma_thuong_hieu = th.ma_thuong_hieu) AS so_luong_san_pham
                FROM thuong_hieu th
                WHERE th.ma_thuong_hieu = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maThuongHieu);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi truy vấn thương hiệu: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public boolean existsById(int maThuongHieu) {
        String sql = "SELECT COUNT(*) FROM thuong_hieu WHERE ma_thuong_hieu = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maThuongHieu);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi kiểm tra tồn tại thương hiệu", e);
        }
    }

    public boolean existsByName(String tenThuongHieu, Integer excludeId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM thuong_hieu WHERE LOWER(ten_thuong_hieu) = ?");
        List<Object> params = new ArrayList<>();
        params.add(tenThuongHieu.trim().toLowerCase());
        if (excludeId != null) {
            sql.append(" AND ma_thuong_hieu <> ?");
            params.add(excludeId);
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoJdbcHelper.bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi kiểm tra trùng tên thương hiệu", e);
        }
    }

    public long countProducts(int maThuongHieu) {
        String sql = "SELECT COUNT(*) FROM san_pham WHERE ma_thuong_hieu = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maThuongHieu);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi đếm sản phẩm của thương hiệu", e);
        }
    }

    public long countPromotions(int maThuongHieu) {
        String sql = "SELECT COUNT(*) FROM khuyen_mai WHERE ma_thuong_hieu_ap_dung = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maThuongHieu);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi kiểm tra khuyến mãi của thương hiệu", e);
        }
    }

    public BrandDTO insert(BrandRequest req) {
        String sql = "INSERT INTO thuong_hieu (ten_thuong_hieu, quoc_gia, logo_url, mo_ta) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, req.getTenThuongHieu().trim());
            String quocGia = trimToNull(req.getQuocGia());
            ps.setString(2, quocGia != null ? quocGia : "Quốc tế");
            ps.setString(3, trimToNull(req.getLogoUrl()));
            ps.setString(4, trimToNull(req.getMoTa()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getInt(1)).orElseThrow();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi thêm thương hiệu mới: " + e.getMessage(), e);
        }
        throw new IllegalStateException("Không thể lấy mã thương hiệu vừa tạo");
    }

    public BrandDTO update(int maThuongHieu, BrandRequest req) {
        String sql = "UPDATE thuong_hieu SET ten_thuong_hieu = ?, quoc_gia = ?, logo_url = ?, mo_ta = ? WHERE ma_thuong_hieu = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, req.getTenThuongHieu().trim());
            String quocGia = trimToNull(req.getQuocGia());
            ps.setString(2, quocGia != null ? quocGia : "Quốc tế");
            ps.setString(3, trimToNull(req.getLogoUrl()));
            ps.setString(4, trimToNull(req.getMoTa()));
            ps.setInt(5, maThuongHieu);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi cập nhật thương hiệu: " + e.getMessage(), e);
        }
        return findById(maThuongHieu).orElseThrow();
    }

    public boolean deleteById(int maThuongHieu) {
        String sql = "DELETE FROM thuong_hieu WHERE ma_thuong_hieu = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maThuongHieu);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi xóa thương hiệu: " + e.getMessage(), e);
        }
    }

    private BrandDTO mapRow(ResultSet rs) throws SQLException {
        return BrandDTO.builder()
                .maThuongHieu(rs.getInt("ma_thuong_hieu"))
                .tenThuongHieu(rs.getString("ten_thuong_hieu"))
                .quocGia(rs.getString("quoc_gia"))
                .logoUrl(rs.getString("logo_url"))
                .moTa(rs.getString("mo_ta"))
                .soLuongSanPham(rs.getLong("so_luong_san_pham"))
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
