package com.phonestore.dao;

import com.phonestore.dto.request.ProductRequest;
import com.phonestore.dto.response.ProductDTO;
import com.phonestore.dto.response.ProductGalleryDTO;
import com.phonestore.dto.response.ProductVariantSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class ProductDao {

    private final DataSource dataSource;
    private final BrandDao brandDao;
    private final CategoryDao categoryDao;

    private static final String BASE_SELECT = """
            SELECT sp.ma_san_pham, sp.ten_san_pham, sp.ma_thuong_hieu, th.ten_thuong_hieu,
                   sp.ma_danh_muc, dm.ten_danh_muc, sp.dong_may, sp.mau_sac, sp.ram, sp.rom,
                   sp.man_hinh, sp.chip_xu_ly, sp.pin, sp.camera, sp.he_dieu_hanh,
                   sp.gia_nhap_tham_khao, sp.gia_ban, sp.thoi_gian_bao_hanh_thang,
                   sp.so_luong_ton, sp.dinh_muc_ton_toi_thieu, sp.hinh_anh,
                   sp.trang_thai, sp.ngay_tao
            FROM san_pham sp
            JOIN thuong_hieu th ON sp.ma_thuong_hieu = th.ma_thuong_hieu
            JOIN danh_muc dm ON sp.ma_danh_muc = dm.ma_danh_muc
            """;

    public List<ProductDTO> findAll(
            String keyword,
            Integer maThuongHieu,
            Integer maDanhMuc,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String ram,
            String rom,
            String mauSac,
            String heDieuHanh,
            String trangThai,
            String sort
    ) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                     AND (LOWER(sp.ten_san_pham) LIKE ?
                          OR LOWER(sp.ma_san_pham) LIKE ?
                          OR LOWER(sp.dong_may) LIKE ?
                          OR LOWER(COALESCE(sp.chip_xu_ly, '')) LIKE ?
                          OR LOWER(th.ten_thuong_hieu) LIKE ?)
                    """);
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (maThuongHieu != null) {
            sql.append(" AND sp.ma_thuong_hieu = ?");
            params.add(maThuongHieu);
        }
        if (maDanhMuc != null) {
            sql.append(" AND sp.ma_danh_muc = ?");
            params.add(maDanhMuc);
        }
        if (minPrice != null) {
            sql.append(" AND sp.gia_ban >= ?");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND sp.gia_ban <= ?");
            params.add(maxPrice);
        }
        if (ram != null && !ram.isBlank()) {
            sql.append(" AND LOWER(TRIM(sp.ram)) = ?");
            params.add(ram.trim().toLowerCase());
        }
        if (rom != null && !rom.isBlank()) {
            sql.append(" AND LOWER(TRIM(sp.rom)) = ?");
            params.add(rom.trim().toLowerCase());
        }
        if (mauSac != null && !mauSac.isBlank()) {
            sql.append(" AND LOWER(sp.mau_sac) LIKE ?");
            params.add("%" + mauSac.trim().toLowerCase() + "%");
        }
        if (heDieuHanh != null && !heDieuHanh.isBlank()) {
            sql.append(" AND LOWER(COALESCE(sp.he_dieu_hanh, '')) LIKE ?");
            params.add("%" + heDieuHanh.trim().toLowerCase() + "%");
        }
        if (trangThai != null && !trangThai.isBlank()) {
            sql.append(" AND sp.trang_thai = ?");
            params.add(trangThai.trim());
        }

        sql.append(switch (sort == null ? "newest" : sort.trim()) {
            case "price_asc" -> " ORDER BY sp.gia_ban ASC, sp.ma_san_pham ASC";
            case "price_desc" -> " ORDER BY sp.gia_ban DESC, sp.ma_san_pham ASC";
            case "name_asc" -> " ORDER BY sp.ten_san_pham ASC, sp.ma_san_pham ASC";
            case "stock_desc" -> " ORDER BY sp.so_luong_ton DESC, sp.ten_san_pham ASC";
            default -> " ORDER BY sp.ngay_tao DESC, sp.ma_san_pham ASC";
        });

        Map<String, List<ProductGalleryDTO>> galleriesByProduct = loadAllGalleriesGrouped();
        List<ProductDTO> result = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoJdbcHelper.bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductDTO dto = mapRow(rs);
                    dto.setGalleries(galleriesByProduct.getOrDefault(dto.getMaSanPham(), List.of()));
                    result.add(dto);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tải danh sách sản phẩm: " + e.getMessage(), e);
        }
        return result;
    }

    public Optional<ProductDTO> findById(String maSanPham) {
        String sql = BASE_SELECT + " WHERE sp.ma_san_pham = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSanPham);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ProductDTO dto = mapRow(rs);
                    dto.setGalleries(findGalleriesByProduct(maSanPham));
                    dto.setSiblingVariants(findSiblingVariants(dto.getMaThuongHieu(), dto.getDongMay()));
                    dto.setCoLichSuGiaoDich(hasRelatedHistory(maSanPham));
                    return Optional.of(dto);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi truy vấn chi tiết sản phẩm: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Map<String, Object> getFilterOptions() {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("brands", brandDao.findAll(null));
        options.put("categories", categoryDao.findAll(null));
        options.put("rams", queryDistinctValues("SELECT DISTINCT ram FROM san_pham WHERE ram IS NOT NULL AND TRIM(ram) <> '' ORDER BY ram"));
        options.put("roms", queryDistinctValues("SELECT DISTINCT rom FROM san_pham WHERE rom IS NOT NULL AND TRIM(rom) <> '' ORDER BY rom"));
        options.put("colors", queryDistinctValues("SELECT DISTINCT mau_sac FROM san_pham WHERE mau_sac IS NOT NULL AND TRIM(mau_sac) <> '' ORDER BY mau_sac"));
        options.put("operatingSystems", queryDistinctValues("SELECT DISTINCT he_dieu_hanh FROM san_pham WHERE he_dieu_hanh IS NOT NULL AND TRIM(he_dieu_hanh) <> '' ORDER BY he_dieu_hanh"));
        options.put("productLines", queryDistinctValues("SELECT DISTINCT dong_may FROM san_pham WHERE dong_may IS NOT NULL AND TRIM(dong_may) <> '' ORDER BY dong_may"));

        BigDecimal minPrice = BigDecimal.ZERO;
        BigDecimal maxPrice = BigDecimal.ZERO;
        String priceSql = "SELECT COALESCE(MIN(gia_ban), 0) AS min_price, COALESCE(MAX(gia_ban), 0) AS max_price FROM san_pham";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(priceSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                minPrice = DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("min_price"));
                maxPrice = DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("max_price"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tải khoảng giá sản phẩm", e);
        }
        options.put("minPrice", minPrice);
        options.put("maxPrice", maxPrice);
        return options;
    }

    public boolean existsById(String maSanPham) {
        String sql = "SELECT COUNT(*) FROM san_pham WHERE ma_san_pham = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSanPham);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi kiểm tra mã sản phẩm", e);
        }
    }

    public void insert(String maSanPham, ProductRequest req, String primaryImageUrl) {
        String sql = """
                INSERT INTO san_pham (
                    ma_san_pham, ten_san_pham, ma_thuong_hieu, ma_danh_muc, dong_may,
                    mau_sac, ram, rom, man_hinh, chip_xu_ly, pin, camera, he_dieu_hanh,
                    gia_nhap_tham_khao, gia_ban, thoi_gian_bao_hanh_thang,
                    so_luong_ton, dinh_muc_ton_toi_thieu, hinh_anh, trang_thai, ngay_tao
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        List<Object> params = Arrays.asList(
                maSanPham,
                req.getTenSanPham().trim(),
                req.getMaThuongHieu(),
                req.getMaDanhMuc(),
                req.getDongMay().trim(),
                req.getMauSac().trim(),
                req.getRam().trim(),
                req.getRom().trim(),
                trimToNull(req.getManHinh()),
                trimToNull(req.getChipXuLy()),
                trimToNull(req.getPin()),
                trimToNull(req.getCamera()),
                trimToNull(req.getHeDieuHanh()),
                req.getGiaNhapThamKhao() != null ? req.getGiaNhapThamKhao() : BigDecimal.ZERO,
                req.getGiaBan() != null ? req.getGiaBan() : BigDecimal.ZERO,
                req.getThoiGianBaoHanhThang() != null ? req.getThoiGianBaoHanhThang() : 12,
                req.getDinhMucTonToiThieu() != null ? req.getDinhMucTonToiThieu() : 2,
                trimToNull(primaryImageUrl),
                req.getTrangThai() != null && !req.getTrangThai().isBlank() ? req.getTrangThai().trim() : "Dang_kinh_doanh"
        );
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DaoJdbcHelper.bindParams(ps, params);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi thêm sản phẩm mới: " + e.getMessage(), e);
        }
    }

    public void update(String maSanPham, ProductRequest req, String primaryImageUrl) {
        String sql = """
                UPDATE san_pham
                SET ten_san_pham = ?,
                    ma_thuong_hieu = ?,
                    ma_danh_muc = ?,
                    dong_may = ?,
                    mau_sac = ?,
                    ram = ?,
                    rom = ?,
                    man_hinh = ?,
                    chip_xu_ly = ?,
                    pin = ?,
                    camera = ?,
                    he_dieu_hanh = ?,
                    gia_nhap_tham_khao = ?,
                    gia_ban = ?,
                    thoi_gian_bao_hanh_thang = ?,
                    dinh_muc_ton_toi_thieu = ?,
                    hinh_anh = ?,
                    trang_thai = ?
                WHERE ma_san_pham = ?
                """;
        List<Object> params = Arrays.asList(
                req.getTenSanPham().trim(),
                req.getMaThuongHieu(),
                req.getMaDanhMuc(),
                req.getDongMay().trim(),
                req.getMauSac().trim(),
                req.getRam().trim(),
                req.getRom().trim(),
                trimToNull(req.getManHinh()),
                trimToNull(req.getChipXuLy()),
                trimToNull(req.getPin()),
                trimToNull(req.getCamera()),
                trimToNull(req.getHeDieuHanh()),
                req.getGiaNhapThamKhao() != null ? req.getGiaNhapThamKhao() : BigDecimal.ZERO,
                req.getGiaBan() != null ? req.getGiaBan() : BigDecimal.ZERO,
                req.getThoiGianBaoHanhThang() != null ? req.getThoiGianBaoHanhThang() : 12,
                req.getDinhMucTonToiThieu() != null ? req.getDinhMucTonToiThieu() : 2,
                trimToNull(primaryImageUrl),
                req.getTrangThai() != null && !req.getTrangThai().isBlank() ? req.getTrangThai().trim() : "Dang_kinh_doanh",
                maSanPham
        );
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            DaoJdbcHelper.bindParams(ps, params);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi cập nhật sản phẩm: " + e.getMessage(), e);
        }
    }

    public boolean updateStatus(String maSanPham, String trangThai) {
        String sql = "UPDATE san_pham SET trang_thai = ? WHERE ma_san_pham = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trangThai);
            ps.setString(2, maSanPham);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi cập nhật trạng thái sản phẩm: " + e.getMessage(), e);
        }
    }

    public boolean hasRelatedHistory(String maSanPham) {
        List<String> historyQueries = List.of(
                "SELECT COUNT(*) FROM chi_tiet_phieu_nhap WHERE ma_san_pham = ?",
                "SELECT COUNT(*) FROM imei_san_pham WHERE ma_san_pham = ?",
                "SELECT COUNT(*) FROM chi_tiet_don_hang WHERE ma_san_pham = ?",
                "SELECT COUNT(*) FROM chi_tiet_hoa_don WHERE ma_san_pham = ?",
                "SELECT COUNT(*) FROM phieu_bao_hanh WHERE ma_san_pham = ?",
                "SELECT COUNT(*) FROM chi_tiet_kiem_ke WHERE ma_san_pham = ?"
        );
        try (Connection conn = dataSource.getConnection()) {
            for (String sql : historyQueries) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, maSanPham);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getLong(1) > 0) {
                            return true;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi kiểm tra lịch sử giao dịch sản phẩm", e);
        }
        return false;
    }

    public boolean deleteById(String maSanPham) {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement delGallery = conn.prepareStatement("DELETE FROM hinh_anh_san_pham WHERE ma_san_pham = ?")) {
                delGallery.setString(1, maSanPham);
                delGallery.executeUpdate();
            }
            try (PreparedStatement delProduct = conn.prepareStatement("DELETE FROM san_pham WHERE ma_san_pham = ?")) {
                delProduct.setString(1, maSanPham);
                return delProduct.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi xóa sản phẩm: " + e.getMessage(), e);
        }
    }

    public void replaceGalleries(String maSanPham, List<String> rawUrls, String preferredPrimaryUrl) {
        List<String> cleanUrls = new ArrayList<>();
        if (rawUrls != null) {
            for (String u : rawUrls) {
                String trimmed = trimToNull(u);
                if (trimmed != null && !cleanUrls.contains(trimmed)) {
                    cleanUrls.add(trimmed);
                }
            }
        }
        String cleanPreferred = trimToNull(preferredPrimaryUrl);
        if (cleanPreferred != null && !cleanUrls.contains(cleanPreferred)) {
            cleanUrls.add(0, cleanPreferred);
        }

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM hinh_anh_san_pham WHERE ma_san_pham = ?")) {
                del.setString(1, maSanPham);
                del.executeUpdate();
            }

            if (cleanUrls.isEmpty()) {
                try (PreparedStatement upd = conn.prepareStatement("UPDATE san_pham SET hinh_anh = NULL WHERE ma_san_pham = ?")) {
                    upd.setString(1, maSanPham);
                    upd.executeUpdate();
                }
                return;
            }

            String primaryUrl = (cleanPreferred != null && cleanUrls.contains(cleanPreferred))
                    ? cleanPreferred
                    : cleanUrls.get(0);

            String insertSql = "INSERT INTO hinh_anh_san_pham (ma_san_pham, duong_dan_anh, la_anh_chinh, thu_tu, ngay_tao) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
            try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                for (int i = 0; i < cleanUrls.size(); i++) {
                    String url = cleanUrls.get(i);
                    ins.setString(1, maSanPham);
                    ins.setString(2, url);
                    ins.setBoolean(3, url.equals(primaryUrl));
                    ins.setInt(4, i + 1);
                    ins.addBatch();
                }
                ins.executeBatch();
            }

            try (PreparedStatement upd = conn.prepareStatement("UPDATE san_pham SET hinh_anh = ? WHERE ma_san_pham = ?")) {
                upd.setString(1, primaryUrl);
                upd.setString(2, maSanPham);
                upd.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi cập nhật thư viện ảnh sản phẩm: " + e.getMessage(), e);
        }
    }

    public List<ProductGalleryDTO> findGalleriesByProduct(String maSanPham) {
        String sql = """
                SELECT ma_hinh_anh, ma_san_pham, duong_dan_anh, la_anh_chinh, thu_tu, ngay_tao
                FROM hinh_anh_san_pham
                WHERE ma_san_pham = ?
                ORDER BY la_anh_chinh DESC, thu_tu ASC, ma_hinh_anh ASC
                """;
        List<ProductGalleryDTO> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSanPham);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapGalleryRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tải ảnh sản phẩm", e);
        }
        return list;
    }

    public ProductGalleryDTO addGalleryImage(String maSanPham, String duongDanAnh, boolean laAnhChinh, Integer thuTu) {
        try (Connection conn = dataSource.getConnection()) {
            int nextOrder = thuTu != null ? thuTu : 1;
            if (thuTu == null) {
                try (PreparedStatement psOrder = conn.prepareStatement(
                        "SELECT COALESCE(MAX(thu_tu), 0) + 1 FROM hinh_anh_san_pham WHERE ma_san_pham = ?")) {
                    psOrder.setString(1, maSanPham);
                    try (ResultSet rs = psOrder.executeQuery()) {
                        if (rs.next()) nextOrder = rs.getInt(1);
                    }
                }
            }

            boolean shouldBePrimary = laAnhChinh || nextOrder == 1;
            if (shouldBePrimary) {
                try (PreparedStatement reset = conn.prepareStatement(
                        "UPDATE hinh_anh_san_pham SET la_anh_chinh = FALSE WHERE ma_san_pham = ?")) {
                    reset.setString(1, maSanPham);
                    reset.executeUpdate();
                }
            }

            String insertSql = "INSERT INTO hinh_anh_san_pham (ma_san_pham, duong_dan_anh, la_anh_chinh, thu_tu, ngay_tao) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
            int newId = 0;
            try (PreparedStatement ins = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ins.setString(1, maSanPham);
                ins.setString(2, duongDanAnh.trim());
                ins.setBoolean(3, shouldBePrimary);
                ins.setInt(4, nextOrder);
                ins.executeUpdate();
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    if (keys.next()) newId = keys.getInt(1);
                }
            }

            if (shouldBePrimary) {
                try (PreparedStatement updSp = conn.prepareStatement("UPDATE san_pham SET hinh_anh = ? WHERE ma_san_pham = ?")) {
                    updSp.setString(1, duongDanAnh.trim());
                    updSp.setString(2, maSanPham);
                    updSp.executeUpdate();
                }
            }

            final int createdId = newId;
            return findGalleriesByProduct(maSanPham).stream()
                    .filter(g -> g.getMaHinhAnh() == createdId)
                    .findFirst()
                    .orElseThrow();
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi thêm ảnh vào thư viện sản phẩm: " + e.getMessage(), e);
        }
    }

    public boolean setPrimaryGalleryImage(String maSanPham, int maHinhAnh) {
        try (Connection conn = dataSource.getConnection()) {
            String targetUrl = null;
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT duong_dan_anh FROM hinh_anh_san_pham WHERE ma_hinh_anh = ? AND ma_san_pham = ?")) {
                check.setInt(1, maHinhAnh);
                check.setString(2, maSanPham);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        targetUrl = rs.getString("duong_dan_anh");
                    }
                }
            }
            if (targetUrl == null) {
                return false;
            }
            try (PreparedStatement reset = conn.prepareStatement(
                    "UPDATE hinh_anh_san_pham SET la_anh_chinh = FALSE WHERE ma_san_pham = ?")) {
                reset.setString(1, maSanPham);
                reset.executeUpdate();
            }
            try (PreparedStatement setPrimary = conn.prepareStatement(
                    "UPDATE hinh_anh_san_pham SET la_anh_chinh = TRUE WHERE ma_hinh_anh = ?")) {
                setPrimary.setInt(1, maHinhAnh);
                setPrimary.executeUpdate();
            }
            try (PreparedStatement updSp = conn.prepareStatement(
                    "UPDATE san_pham SET hinh_anh = ? WHERE ma_san_pham = ?")) {
                updSp.setString(1, targetUrl);
                updSp.setString(2, maSanPham);
                updSp.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi đặt ảnh đại diện: " + e.getMessage(), e);
        }
    }

    public boolean deleteGalleryImage(int maHinhAnh) {
        try (Connection conn = dataSource.getConnection()) {
            String maSanPham = null;
            boolean wasPrimary = false;
            try (PreparedStatement find = conn.prepareStatement(
                    "SELECT ma_san_pham, la_anh_chinh FROM hinh_anh_san_pham WHERE ma_hinh_anh = ?")) {
                find.setInt(1, maHinhAnh);
                try (ResultSet rs = find.executeQuery()) {
                    if (rs.next()) {
                        maSanPham = rs.getString("ma_san_pham");
                        wasPrimary = rs.getBoolean("la_anh_chinh");
                    }
                }
            }
            if (maSanPham == null) {
                return false;
            }
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM hinh_anh_san_pham WHERE ma_hinh_anh = ?")) {
                del.setInt(1, maHinhAnh);
                del.executeUpdate();
            }
            if (wasPrimary) {
                List<ProductGalleryDTO> remaining = findGalleriesByProduct(maSanPham);
                if (!remaining.isEmpty()) {
                    setPrimaryGalleryImage(maSanPham, remaining.get(0).getMaHinhAnh());
                } else {
                    try (PreparedStatement upd = conn.prepareStatement("UPDATE san_pham SET hinh_anh = NULL WHERE ma_san_pham = ?")) {
                        upd.setString(1, maSanPham);
                        upd.executeUpdate();
                    }
                }
            }
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi xóa ảnh sản phẩm: " + e.getMessage(), e);
        }
    }

    private List<ProductVariantSummaryDTO> findSiblingVariants(Integer maThuongHieu, String dongMay) {
        if (maThuongHieu == null || dongMay == null || dongMay.isBlank()) {
            return List.of();
        }
        String sql = """
                SELECT ma_san_pham, ten_san_pham, mau_sac, ram, rom, gia_ban, so_luong_ton, trang_thai, hinh_anh
                FROM san_pham
                WHERE ma_thuong_hieu = ? AND LOWER(dong_may) = LOWER(?)
                ORDER BY rom ASC, mau_sac ASC, ma_san_pham ASC
                """;
        List<ProductVariantSummaryDTO> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maThuongHieu);
            ps.setString(2, dongMay.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(ProductVariantSummaryDTO.builder()
                            .maSanPham(rs.getString("ma_san_pham"))
                            .tenSanPham(rs.getString("ten_san_pham"))
                            .mauSac(rs.getString("mau_sac"))
                            .ram(rs.getString("ram"))
                            .rom(rs.getString("rom"))
                            .giaBan(DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("gia_ban")))
                            .soLuongTon(rs.getInt("so_luong_ton"))
                            .trangThai(rs.getString("trang_thai"))
                            .hinhAnh(rs.getString("hinh_anh"))
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tải các biến thể cùng dòng máy", e);
        }
        return list;
    }

    private Map<String, List<ProductGalleryDTO>> loadAllGalleriesGrouped() {
        String sql = """
                SELECT ma_hinh_anh, ma_san_pham, duong_dan_anh, la_anh_chinh, thu_tu, ngay_tao
                FROM hinh_anh_san_pham
                ORDER BY la_anh_chinh DESC, thu_tu ASC, ma_hinh_anh ASC
                """;
        Map<String, List<ProductGalleryDTO>> map = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ProductGalleryDTO g = mapGalleryRow(rs);
                map.computeIfAbsent(g.getMaSanPham(), k -> new ArrayList<>()).add(g);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tải thư viện ảnh sản phẩm", e);
        }
        return map;
    }

    private List<String> queryDistinctValues(String sql) {
        List<String> values = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String v = rs.getString(1);
                if (v != null && !v.isBlank()) {
                    values.add(v.trim());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi truy vấn danh sách thuộc tính lọc", e);
        }
        return values;
    }

    private ProductDTO mapRow(ResultSet rs) throws SQLException {
        return ProductDTO.builder()
                .maSanPham(rs.getString("ma_san_pham"))
                .tenSanPham(rs.getString("ten_san_pham"))
                .maThuongHieu(rs.getInt("ma_thuong_hieu"))
                .tenThuongHieu(rs.getString("ten_thuong_hieu"))
                .maDanhMuc(rs.getInt("ma_danh_muc"))
                .tenDanhMuc(rs.getString("ten_danh_muc"))
                .dongMay(rs.getString("dong_may"))
                .mauSac(rs.getString("mau_sac"))
                .ram(rs.getString("ram"))
                .rom(rs.getString("rom"))
                .manHinh(rs.getString("man_hinh"))
                .chipXuLy(rs.getString("chip_xu_ly"))
                .pin(rs.getString("pin"))
                .camera(rs.getString("camera"))
                .heDieuHanh(rs.getString("he_dieu_hanh"))
                .giaNhapThamKhao(DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("gia_nhap_tham_khao")))
                .giaBan(DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("gia_ban")))
                .thoiGianBaoHanhThang(rs.getInt("thoi_gian_bao_hanh_thang"))
                .soLuongTon(rs.getInt("so_luong_ton"))
                .dinhMucTonToiThieu(rs.getInt("dinh_muc_ton_toi_thieu"))
                .hinhAnh(rs.getString("hinh_anh"))
                .trangThai(rs.getString("trang_thai"))
                .ngayTao(DaoJdbcHelper.toLocalDateTime(rs.getTimestamp("ngay_tao")))
                .build();
    }

    private ProductGalleryDTO mapGalleryRow(ResultSet rs) throws SQLException {
        return ProductGalleryDTO.builder()
                .maHinhAnh(rs.getInt("ma_hinh_anh"))
                .maSanPham(rs.getString("ma_san_pham"))
                .duongDanAnh(rs.getString("duong_dan_anh"))
                .laAnhChinh(rs.getBoolean("la_anh_chinh"))
                .thuTu(rs.getInt("thu_tu"))
                .ngayTao(DaoJdbcHelper.toLocalDateTime(rs.getTimestamp("ngay_tao")))
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
