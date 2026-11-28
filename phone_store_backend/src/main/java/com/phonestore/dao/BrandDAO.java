package com.phonestore.dao;

import com.phonestore.model.Brand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BrandDAO {
    private final DataSource dataSource;

    public List<Brand> findAll(String keyword) {
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
            params.add(pattern); params.add(pattern); params.add(pattern);
        }
        sql.append(" ORDER BY th.ma_thuong_hieu ASC");
        List<Brand> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoJdbcHelper.bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tải danh sách thương hiệu: " + e.getMessage(), e);
        }
    }

    public Optional<Brand> findById(int id) {
        String sql = """
            SELECT th.ma_thuong_hieu, th.ten_thuong_hieu, th.quoc_gia, th.logo_url, th.mo_ta,
                   (SELECT COUNT(*) FROM san_pham sp WHERE sp.ma_thuong_hieu = th.ma_thuong_hieu) AS so_luong_san_pham
            FROM thuong_hieu th WHERE th.ma_thuong_hieu = ?
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi truy vấn thương hiệu: " + e.getMessage(), e);
        }
    }

    public boolean existsByName(String name, Integer excludeId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM thuong_hieu WHERE LOWER(ten_thuong_hieu) = ?");
        List<Object> params = new ArrayList<>();
        params.add(name.trim().toLowerCase());
        if (excludeId != null) { sql.append(" AND ma_thuong_hieu <> ?"); params.add(excludeId); }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoJdbcHelper.bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getLong(1) > 0; }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi kiểm tra trùng tên thương hiệu", e);
        }
    }

    public long countProducts(int id) {
        return count("SELECT COUNT(*) FROM san_pham WHERE ma_thuong_hieu = ?", id);
    }

    public long countPromotions(int id) {
        return count("SELECT COUNT(*) FROM khuyen_mai WHERE ma_thuong_hieu_ap_dung = ?", id);
    }

    private long count(String sql, int id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : 0L; }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi truy vấn số lượng dữ liệu liên quan thương hiệu", e);
        }
    }

    public Brand insert(Brand brand) {
        String sql = "INSERT INTO thuong_hieu (ten_thuong_hieu, quoc_gia, logo_url, mo_ta) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, brand.getTenThuongHieu());
            ps.setString(2, brand.getQuocGia());
            ps.setString(3, brand.getLogoUrl());
            ps.setString(4, brand.getMoTa());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return findById(keys.getInt(1)).orElseThrow();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi thêm thương hiệu: " + e.getMessage(), e);
        }
        throw new IllegalStateException("Không lấy được mã thương hiệu vừa tạo");
    }

    public Brand update(int id, Brand brand) {
        String sql = "UPDATE thuong_hieu SET ten_thuong_hieu=?, quoc_gia=?, logo_url=?, mo_ta=? WHERE ma_thuong_hieu=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, brand.getTenThuongHieu());
            ps.setString(2, brand.getQuocGia());
            ps.setString(3, brand.getLogoUrl());
            ps.setString(4, brand.getMoTa());
            ps.setInt(5, id);
            if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Không tìm thấy thương hiệu mã #" + id);
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi cập nhật thương hiệu: " + e.getMessage(), e);
        }
        return findById(id).orElseThrow();
    }

    public boolean deleteById(int id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM thuong_hieu WHERE ma_thuong_hieu = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi xóa thương hiệu: " + e.getMessage(), e);
        }
    }

    private Brand mapRow(ResultSet rs) throws SQLException {
        Brand brand = new Brand();
        brand.setMaThuongHieu(rs.getInt("ma_thuong_hieu"));
        brand.setTenThuongHieu(rs.getString("ten_thuong_hieu"));
        brand.setQuocGia(rs.getString("quoc_gia"));
        brand.setLogoUrl(rs.getString("logo_url"));
        brand.setMoTa(rs.getString("mo_ta"));
        brand.setSoLuongSanPham(rs.getLong("so_luong_san_pham"));
        return brand;
    }
}