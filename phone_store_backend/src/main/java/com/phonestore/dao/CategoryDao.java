package com.phonestore.dao;

import com.phonestore.dto.request.CategoryRequest;
import com.phonestore.dto.response.CategoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryDao {

    private final DataSource dataSource;

    public List<CategoryDTO> findAll(String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT dm.ma_danh_muc, dm.ten_danh_muc, dm.mo_ta,
                       (SELECT COUNT(*) FROM san_pham sp WHERE sp.ma_danh_muc = dm.ma_danh_muc) AS so_luong_san_pham
                FROM danh_muc dm
                """);
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" WHERE (LOWER(dm.ten_danh_muc) LIKE ? OR LOWER(COALESCE(dm.mo_ta, '')) LIKE ?)");
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
        }
        sql.append(" ORDER BY dm.ma_danh_muc ASC");

        List<CategoryDTO> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoJdbcHelper.bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tải danh sách danh mục: " + e.getMessage(), e);
        }
        return result;
    }

    public Optional<CategoryDTO> findById(int maDanhMuc) {
        String sql = """
                SELECT dm.ma_danh_muc, dm.ten_danh_muc, dm.mo_ta,
                       (SELECT COUNT(*) FROM san_pham sp WHERE sp.ma_danh_muc = dm.ma_danh_muc) AS so_luong_san_pham
                FROM danh_muc dm
                WHERE dm.ma_danh_muc = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maDanhMuc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi truy vấn danh mục: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public boolean existsById(int maDanhMuc) {
        String sql = "SELECT COUNT(*) FROM danh_muc WHERE ma_danh_muc = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maDanhMuc);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi kiểm tra tồn tại danh mục", e);
        }
    }

    public boolean existsByName(String tenDanhMuc, Integer excludeId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM danh_muc WHERE LOWER(ten_danh_muc) = ?");
        List<Object> params = new ArrayList<>();
        params.add(tenDanhMuc.trim().toLowerCase());
        if (excludeId != null) {
            sql.append(" AND ma_danh_muc <> ?");
            params.add(excludeId);
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoJdbcHelper.bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi kiểm tra trùng tên danh mục", e);
        }
    }

    public long countProducts(int maDanhMuc) {
        String sql = "SELECT COUNT(*) FROM san_pham WHERE ma_danh_muc = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maDanhMuc);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi đếm sản phẩm trong danh mục", e);
        }
    }

    public CategoryDTO insert(CategoryRequest req) {
        String sql = "INSERT INTO danh_muc (ten_danh_muc, mo_ta) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, req.getTenDanhMuc().trim());
            ps.setString(2, trimToNull(req.getMoTa()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getInt(1)).orElseThrow();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi thêm danh mục mới: " + e.getMessage(), e);
        }
        throw new IllegalStateException("Không thể lấy mã danh mục vừa tạo");
    }

    public CategoryDTO update(int maDanhMuc, CategoryRequest req) {
        String sql = "UPDATE danh_muc SET ten_danh_muc = ?, mo_ta = ? WHERE ma_danh_muc = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, req.getTenDanhMuc().trim());
            ps.setString(2, trimToNull(req.getMoTa()));
            ps.setInt(3, maDanhMuc);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi cập nhật danh mục: " + e.getMessage(), e);
        }
        return findById(maDanhMuc).orElseThrow();
    }

    public boolean deleteById(int maDanhMuc) {
        String sql = "DELETE FROM danh_muc WHERE ma_danh_muc = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maDanhMuc);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi xóa danh mục: " + e.getMessage(), e);
        }
    }

    private CategoryDTO mapRow(ResultSet rs) throws SQLException {
        return CategoryDTO.builder()
                .maDanhMuc(rs.getInt("ma_danh_muc"))
                .tenDanhMuc(rs.getString("ten_danh_muc"))
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
