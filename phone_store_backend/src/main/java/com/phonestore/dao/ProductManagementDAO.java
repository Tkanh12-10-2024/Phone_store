package com.phonestore.dao;

import com.phonestore.dto.product.BrandRequest;
import com.phonestore.dto.product.CategoryRequest;
import com.phonestore.dto.product.ProductRequest;
import com.phonestore.dto.product.ProductVariantRequest;
import com.phonestore.dto.product.ProductConflictException;
import com.phonestore.dto.product.ProductValidationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@Repository
public class ProductManagementDAO {

    private static final String PRODUCT_SELECT = """
            SELECT sp.ma_san_pham, sp.ten_san_pham, sp.ma_thuong_hieu,
                   th.ten_thuong_hieu, sp.ma_danh_muc, dm.ten_danh_muc,
                   sp.dong_may, sp.mau_sac, sp.ram, sp.rom, sp.man_hinh,
                   sp.chip_xu_ly, sp.pin, sp.camera, sp.he_dieu_hanh,
                   sp.gia_nhap_tham_khao, sp.gia_ban,
                   sp.thoi_gian_bao_hanh_thang,
                   COALESCE((
                       SELECT COUNT(*) FROM imei_san_pham i
                       WHERE i.ma_san_pham = sp.ma_san_pham
                         AND i.trang_thai = 'Trong_kho'
                   ), 0) AS so_luong_ton_hien_tai,
                   sp.dinh_muc_ton_toi_thieu, sp.hinh_anh, sp.trang_thai,
                   sp.ngay_tao
            FROM san_pham sp
            LEFT JOIN thuong_hieu th ON th.ma_thuong_hieu = sp.ma_thuong_hieu
            LEFT JOIN danh_muc dm ON dm.ma_danh_muc = sp.ma_danh_muc
            """;

    private final JdbcTemplate jdbcTemplate;

    public ProductManagementDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> layDanhSachSanPham(
            String keyword,
            String maThuongHieu,
            String maDanhMuc,
            String trangThai,
            String minPrice,
            String maxPrice,
            String ram,
            String rom,
            String mauSac,
            String sort
    ) {
        StringBuilder sql = new StringBuilder(PRODUCT_SELECT).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();

        if (hasText(keyword)) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            sql.append("""
                     AND (
                         LOWER(sp.ma_san_pham) LIKE ?
                         OR LOWER(sp.ten_san_pham) LIKE ?
                         OR LOWER(sp.dong_may) LIKE ?
                         OR LOWER(COALESCE(sp.chip_xu_ly, '')) LIKE ?
                         OR LOWER(COALESCE(th.ten_thuong_hieu, '')) LIKE ?
                     )
                    """);
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        addIntegerFilter(sql, args, "sp.ma_thuong_hieu", maThuongHieu, "Mã thương hiệu");
        addIntegerFilter(sql, args, "sp.ma_danh_muc", maDanhMuc, "Mã danh mục");
        if (hasText(trangThai)) {
            validateStatus(trangThai.trim());
            sql.append(" AND sp.trang_thai = ?");
            args.add(trangThai.trim());
        }
        if (hasText(minPrice)) {
            sql.append(" AND sp.gia_ban >= ?");
            args.add(parsePrice(minPrice, "Giá tối thiểu"));
        }
        if (hasText(maxPrice)) {
            sql.append(" AND sp.gia_ban <= ?");
            args.add(parsePrice(maxPrice, "Giá tối đa"));
        }
        if (hasText(ram)) {
            sql.append(" AND LOWER(sp.ram) = LOWER(?)");
            args.add(ram.trim());
        }
        if (hasText(rom)) {
            sql.append(" AND LOWER(sp.rom) = LOWER(?)");
            args.add(rom.trim());
        }
        if (hasText(mauSac)) {
            sql.append(" AND LOWER(sp.mau_sac) LIKE ?");
            args.add("%" + mauSac.trim().toLowerCase() + "%");
        }

        BigDecimal min = hasText(minPrice) ? parsePrice(minPrice, "Giá tối thiểu") : null;
        BigDecimal max = hasText(maxPrice) ? parsePrice(maxPrice, "Giá tối đa") : null;
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new ProductValidationException("Giá tối thiểu không được lớn hơn giá tối đa.");
        }

        sql.append(orderBy(sort));
        return jdbcTemplate.query(sql.toString(), this::mapProduct, args.toArray());
    }

    public Map<String, Object> layTuyChonBoLoc() {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("brands", layThuongHieu(null));
        options.put("categories", layDanhMuc(null));
        options.put("rams", layGiaTriPhanBiet("ram"));
        options.put("roms", layGiaTriPhanBiet("rom"));
        options.put("colors", layGiaTriPhanBiet("mau_sac"));
        options.put("productLines", layGiaTriPhanBiet("dong_may"));
        return options;
    }

    public Map<String, Object> timSanPham(String maSanPham) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                PRODUCT_SELECT + " WHERE sp.ma_san_pham = ?",
                this::mapProduct,
                maSanPham
        );
        if (rows.isEmpty()) {
            throw new NoSuchElementException("Không tìm thấy sản phẩm.");
        }

        Map<String, Object> product = rows.get(0);
        product.put("galleries", jdbcTemplate.query(
                """
                SELECT ma_hinh_anh, duong_dan_anh, la_anh_chinh, thu_tu
                FROM hinh_anh_san_pham
                WHERE ma_san_pham = ?
                ORDER BY thu_tu, ma_hinh_anh
                """,
                (rs, rowNum) -> {
                    Map<String, Object> image = new LinkedHashMap<>();
                    image.put("maHinhAnh", rs.getInt("ma_hinh_anh"));
                    image.put("duongDanAnh", rs.getString("duong_dan_anh"));
                    image.put("laAnhChinh", rs.getBoolean("la_anh_chinh"));
                    image.put("thuTu", rs.getInt("thu_tu"));
                    return image;
                },
                maSanPham
        ));
        product.put("siblingVariants", jdbcTemplate.query(
                """
                SELECT ma_san_pham, mau_sac, ram, rom, gia_ban, trang_thai
                FROM san_pham
                WHERE dong_may = (
                    SELECT dong_may FROM san_pham WHERE ma_san_pham = ?
                )
                ORDER BY rom, ram, mau_sac, ma_san_pham
                """,
                (rs, rowNum) -> {
                    Map<String, Object> variant = new LinkedHashMap<>();
                    variant.put("maSanPham", rs.getString("ma_san_pham"));
                    variant.put("mauSac", rs.getString("mau_sac"));
                    variant.put("ram", rs.getString("ram"));
                    variant.put("rom", rs.getString("rom"));
                    variant.put("giaBan", rs.getBigDecimal("gia_ban"));
                    variant.put("trangThai", rs.getString("trang_thai"));
                    return variant;
                },
                maSanPham
        ));
        return product;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> taoSanPham(ProductRequest request) {
        validateBase(request);
        if (request.variants() == null || request.variants().isEmpty()) {
            throw new ProductValidationException("Sản phẩm phải có ít nhất một biến thể.");
        }
        verifyReferences(request.maThuongHieu(), request.maDanhMuc());

        Set<String> usedSkus = new HashSet<>();
        List<ProductVariantRequest> variants = new ArrayList<>(request.variants());
        List<String> skuList = new ArrayList<>();
        for (ProductVariantRequest variant : variants) {
            validateVariant(variant);
            String sku = hasText(variant.maSanPham())
                    ? variant.maSanPham().trim()
                    : taoMaSanPham(request.dongMay(), variant, usedSkus);
            if (sku.length() > 20) {
                throw new ProductValidationException("Mã sản phẩm không được vượt quá 20 ký tự.");
            }
            if (!usedSkus.add(sku.toUpperCase())) {
                throw new ProductValidationException("Mã sản phẩm bị trùng trong danh sách biến thể: " + sku);
            }
            if (exists("SELECT COUNT(*) FROM san_pham WHERE ma_san_pham = ?", sku)) {
                throw new ProductValidationException("Mã sản phẩm đã tồn tại: " + sku);
            }
            skuList.add(sku);
        }

        List<String> imageUrls = normalizeGallery(request.galleryUrls(), request.hinhAnh());
        String primaryImage = primaryImage(imageUrls, request.hinhAnh());
        List<Map<String, Object>> created = new ArrayList<>();
        for (int i = 0; i < variants.size(); i++) {
            ProductVariantRequest variant = variants.get(i);
            String sku = skuList.get(i);
            jdbcTemplate.update(
                    """
                    INSERT INTO san_pham (
                        ma_san_pham, ten_san_pham, ma_thuong_hieu, ma_danh_muc,
                        dong_may, mau_sac, ram, rom, man_hinh, chip_xu_ly,
                        pin, camera, he_dieu_hanh, gia_nhap_tham_khao, gia_ban,
                        thoi_gian_bao_hanh_thang, so_luong_ton,
                        dinh_muc_ton_toi_thieu, hinh_anh, trang_thai
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?)
                    """,
                    sku,
                    request.tenSanPham().trim(),
                    request.maThuongHieu(),
                    request.maDanhMuc(),
                    request.dongMay().trim(),
                    variant.mauSac().trim(),
                    variant.ram().trim(),
                    variant.rom().trim(),
                    emptyToNull(request.manHinh()),
                    emptyToNull(request.chipXuLy()),
                    emptyToNull(request.pin()),
                    emptyToNull(request.camera()),
                    emptyToNull(request.heDieuHanh()),
                    valueOrZero(variant.giaNhapThamKhao()),
                    variant.giaBan(),
                    request.thoiGianBaoHanhThang() == null ? 12 : request.thoiGianBaoHanhThang(),
                    variant.dinhMucTonToiThieu() == null ? 2 : variant.dinhMucTonToiThieu(),
                    primaryImage,
                    statusOrDefault(request.trangThai())
            );
            replaceGallery(sku, imageUrls, primaryImage);
            created.add(timSanPham(sku));
        }
        return created;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> capNhatSanPham(String maSanPham, ProductRequest request) {
        Map<String, Object> existing = timSanPham(maSanPham);
        validateBase(request);
        verifyReferences(request.maThuongHieu(), request.maDanhMuc());

        String color = required(request.mauSac(), "Màu sắc");
        String ram = required(request.ram(), "RAM");
        String rom = required(request.rom(), "ROM");
        BigDecimal cost = request.giaNhapThamKhao() == null ? BigDecimal.ZERO : request.giaNhapThamKhao();
        BigDecimal price = request.giaBan();
        int minStock = request.dinhMucTonToiThieu() == null ? 2 : request.dinhMucTonToiThieu();
        validateVariant(new ProductVariantRequest(maSanPham, color, ram, rom, cost, price, minStock));

        String status = hasText(request.trangThai())
                ? request.trangThai().trim()
                : String.valueOf(existing.get("trangThai"));
        validateStatus(status);
        List<String> imageUrls = request.galleryUrls() == null
                && request.hinhAnh() == null
                ? null
                : normalizeGallery(request.galleryUrls(), request.hinhAnh());
        String primaryImage = imageUrls == null
                ? (String) existing.get("hinhAnh")
                : primaryImage(imageUrls, request.hinhAnh());

        jdbcTemplate.update(
                """
                UPDATE san_pham
                SET ten_san_pham = ?, ma_thuong_hieu = ?, ma_danh_muc = ?,
                    dong_may = ?, mau_sac = ?, ram = ?, rom = ?, man_hinh = ?,
                    chip_xu_ly = ?, pin = ?, camera = ?, he_dieu_hanh = ?,
                    gia_nhap_tham_khao = ?, gia_ban = ?,
                    thoi_gian_bao_hanh_thang = ?, dinh_muc_ton_toi_thieu = ?,
                    hinh_anh = ?, trang_thai = ?
                WHERE ma_san_pham = ?
                """,
                request.tenSanPham().trim(),
                request.maThuongHieu(),
                request.maDanhMuc(),
                request.dongMay().trim(),
                color,
                ram,
                rom,
                emptyToNull(request.manHinh()),
                emptyToNull(request.chipXuLy()),
                emptyToNull(request.pin()),
                emptyToNull(request.camera()),
                emptyToNull(request.heDieuHanh()),
                cost,
                price,
                request.thoiGianBaoHanhThang() == null ? 12 : request.thoiGianBaoHanhThang(),
                minStock,
                primaryImage,
                status,
                maSanPham
        );
        if (imageUrls != null) {
            replaceGallery(maSanPham, imageUrls, primaryImage);
        }
        return timSanPham(maSanPham);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> capNhatTrangThai(String maSanPham, String status) {
        timSanPham(maSanPham);
        validateStatus(status);
        jdbcTemplate.update(
                "UPDATE san_pham SET trang_thai = ? WHERE ma_san_pham = ?",
                status,
                maSanPham
        );
        return timSanPham(maSanPham);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> xoaSanPham(String maSanPham) {
        timSanPham(maSanPham);
        boolean hasHistory = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                """
                SELECT CASE WHEN
                    EXISTS (SELECT 1 FROM imei_san_pham WHERE ma_san_pham = ?)
                    OR EXISTS (SELECT 1 FROM chi_tiet_phieu_nhap WHERE ma_san_pham = ?)
                    OR EXISTS (SELECT 1 FROM chi_tiet_hoa_don WHERE ma_san_pham = ?)
                    OR EXISTS (SELECT 1 FROM chi_tiet_don_hang WHERE ma_san_pham = ?)
                    OR EXISTS (SELECT 1 FROM chi_tiet_kiem_ke WHERE ma_san_pham = ?)
                    OR EXISTS (SELECT 1 FROM phieu_bao_hanh WHERE ma_san_pham = ?)
                THEN TRUE ELSE FALSE END
                """,
                Boolean.class,
                maSanPham,
                maSanPham,
                maSanPham,
                maSanPham,
                maSanPham,
                maSanPham
        ));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        if (hasHistory) {
            jdbcTemplate.update(
                    "UPDATE san_pham SET trang_thai = 'Ngung_kinh_doanh' WHERE ma_san_pham = ?",
                    maSanPham
            );
            response.put("action", "DEACTIVATED");
            response.put("message", "Sản phẩm có lịch sử giao dịch nên đã được ngừng kinh doanh.");
        } else {
            jdbcTemplate.update("DELETE FROM hinh_anh_san_pham WHERE ma_san_pham = ?", maSanPham);
            jdbcTemplate.update("DELETE FROM san_pham WHERE ma_san_pham = ?", maSanPham);
            response.put("action", "DELETED");
            response.put("message", "Đã xóa sản phẩm.");
        }
        return response;
    }

    public List<Map<String, Object>> layDanhMuc(String keyword) {
        String sql = """
                SELECT dm.ma_danh_muc, dm.ten_danh_muc, dm.mo_ta,
                       (SELECT COUNT(*) FROM san_pham sp
                        WHERE sp.ma_danh_muc = dm.ma_danh_muc) AS so_luong_san_pham
                FROM danh_muc dm
                """;
        List<Object> args = new ArrayList<>();
        if (hasText(keyword)) {
            sql += " WHERE LOWER(dm.ten_danh_muc) LIKE ? OR LOWER(COALESCE(dm.mo_ta, '')) LIKE ?";
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            args.add(pattern);
            args.add(pattern);
        }
        return jdbcTemplate.query(sql + " ORDER BY dm.ten_danh_muc", (rs, rowNum) -> {
            Map<String, Object> category = new LinkedHashMap<>();
            category.put("maDanhMuc", rs.getInt("ma_danh_muc"));
            category.put("tenDanhMuc", rs.getString("ten_danh_muc"));
            category.put("moTa", rs.getString("mo_ta"));
            category.put("soLuongSanPham", rs.getInt("so_luong_san_pham"));
            return category;
        }, args.toArray());
    }

    public List<Map<String, Object>> layThuongHieu(String keyword) {
        String sql = """
                SELECT th.ma_thuong_hieu, th.ten_thuong_hieu, th.quoc_gia,
                       th.mo_ta,
                       (SELECT COUNT(*) FROM san_pham sp
                        WHERE sp.ma_thuong_hieu = th.ma_thuong_hieu) AS so_luong_san_pham
                FROM thuong_hieu th
                """;
        List<Object> args = new ArrayList<>();
        if (hasText(keyword)) {
            sql += """
                     WHERE LOWER(th.ten_thuong_hieu) LIKE ?
                        OR LOWER(COALESCE(th.quoc_gia, '')) LIKE ?
                        OR LOWER(COALESCE(th.mo_ta, '')) LIKE ?
                    """;
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        return jdbcTemplate.query(sql + " ORDER BY th.ten_thuong_hieu", (rs, rowNum) -> {
            Map<String, Object> brand = new LinkedHashMap<>();
            brand.put("maThuongHieu", rs.getInt("ma_thuong_hieu"));
            brand.put("tenThuongHieu", rs.getString("ten_thuong_hieu"));
            brand.put("quocGia", rs.getString("quoc_gia"));
            brand.put("moTa", rs.getString("mo_ta"));
            brand.put("soLuongSanPham", rs.getInt("so_luong_san_pham"));
            return brand;
        }, args.toArray());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> taoDanhMuc(CategoryRequest request) {
        String name = required(request == null ? null : request.tenDanhMuc(), "Tên danh mục");
        String description = emptyToNull(request.moTa());
        validateLength(name, 100, "Tên danh mục");
        validateLength(description, 255, "Mô tả danh mục");
        ensureUnique("danh_muc", "ten_danh_muc", name, null);
        jdbcTemplate.update(
                "INSERT INTO danh_muc (ten_danh_muc, mo_ta) VALUES (?, ?)",
                name,
                description
        );
        return timDanhMucTheoTen(name);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> capNhatDanhMuc(int id, CategoryRequest request) {
        timDanhMuc(id);
        String name = required(request == null ? null : request.tenDanhMuc(), "Tên danh mục");
        String description = emptyToNull(request.moTa());
        validateLength(name, 100, "Tên danh mục");
        validateLength(description, 255, "Mô tả danh mục");
        ensureUnique("danh_muc", "ten_danh_muc", name, id);
        jdbcTemplate.update(
                "UPDATE danh_muc SET ten_danh_muc = ?, mo_ta = ? WHERE ma_danh_muc = ?",
                name,
                description,
                id
        );
        return timDanhMuc(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void xoaDanhMuc(int id) {
        timDanhMuc(id);
        if (exists("SELECT COUNT(*) FROM san_pham WHERE ma_danh_muc = ?", id)) {
            throw new ProductConflictException("Không thể xóa danh mục đang được sản phẩm sử dụng.");
        }
        jdbcTemplate.update("DELETE FROM danh_muc WHERE ma_danh_muc = ?", id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> taoThuongHieu(BrandRequest request) {
        String name = required(request == null ? null : request.tenThuongHieu(), "Tên thương hiệu");
        String country = hasText(request.quocGia()) ? request.quocGia().trim() : "Quốc tế";
        String description = emptyToNull(request.moTa());
        validateLength(name, 50, "Tên thương hiệu");
        validateLength(country, 50, "Quốc gia");
        ensureUnique("thuong_hieu", "ten_thuong_hieu", name, null);
        jdbcTemplate.update(
                "INSERT INTO thuong_hieu (ten_thuong_hieu, quoc_gia, mo_ta) VALUES (?, ?, ?)",
                name,
                country,
                description
        );
        return timThuongHieuTheoTen(name);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> capNhatThuongHieu(int id, BrandRequest request) {
        timThuongHieu(id);
        String name = required(request == null ? null : request.tenThuongHieu(), "Tên thương hiệu");
        String country = hasText(request.quocGia()) ? request.quocGia().trim() : "Quốc tế";
        String description = emptyToNull(request.moTa());
        validateLength(name, 50, "Tên thương hiệu");
        validateLength(country, 50, "Quốc gia");
        ensureUnique("thuong_hieu", "ten_thuong_hieu", name, id);
        jdbcTemplate.update(
                """
                UPDATE thuong_hieu
                SET ten_thuong_hieu = ?, quoc_gia = ?, mo_ta = ?
                WHERE ma_thuong_hieu = ?
                """,
                name,
                country,
                description,
                id
        );
        return timThuongHieu(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void xoaThuongHieu(int id) {
        timThuongHieu(id);
        if (exists("SELECT COUNT(*) FROM san_pham WHERE ma_thuong_hieu = ?", id)) {
            throw new ProductConflictException("Không thể xóa thương hiệu đang được sản phẩm sử dụng.");
        }
        jdbcTemplate.update("DELETE FROM thuong_hieu WHERE ma_thuong_hieu = ?", id);
    }

    private Map<String, Object> mapProduct(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("maSanPham", rs.getString("ma_san_pham"));
        product.put("tenSanPham", rs.getString("ten_san_pham"));
        product.put("maThuongHieu", rs.getInt("ma_thuong_hieu"));
        product.put("tenThuongHieu", rs.getString("ten_thuong_hieu"));
        product.put("maDanhMuc", rs.getInt("ma_danh_muc"));
        product.put("tenDanhMuc", rs.getString("ten_danh_muc"));
        product.put("dongMay", rs.getString("dong_may"));
        product.put("mauSac", rs.getString("mau_sac"));
        product.put("ram", rs.getString("ram"));
        product.put("rom", rs.getString("rom"));
        product.put("manHinh", rs.getString("man_hinh"));
        product.put("chipXuLy", rs.getString("chip_xu_ly"));
        product.put("pin", rs.getString("pin"));
        product.put("camera", rs.getString("camera"));
        product.put("heDieuHanh", rs.getString("he_dieu_hanh"));
        product.put("giaNhapThamKhao", rs.getBigDecimal("gia_nhap_tham_khao"));
        product.put("giaBan", rs.getBigDecimal("gia_ban"));
        product.put("thoiGianBaoHanhThang", rs.getInt("thoi_gian_bao_hanh_thang"));
        product.put("soLuongTon", rs.getInt("so_luong_ton_hien_tai"));
        product.put("dinhMucTonToiThieu", rs.getInt("dinh_muc_ton_toi_thieu"));
        product.put("hinhAnh", rs.getString("hinh_anh"));
        product.put("trangThai", rs.getString("trang_thai"));
        product.put("ngayTao", rs.getTimestamp("ngay_tao"));
        return product;
    }

    private List<String> layGiaTriPhanBiet(String column) {
        if (!Set.of("ram", "rom", "mau_sac", "dong_may").contains(column)) {
            throw new ProductValidationException("Trường lọc không hợp lệ.");
        }
        return jdbcTemplate.query(
                "SELECT DISTINCT " + column + " FROM san_pham WHERE " + column
                        + " IS NOT NULL AND TRIM(" + column + ") <> '' ORDER BY " + column,
                (rs, rowNum) -> rs.getString(1)
        );
    }

    private void validateBase(ProductRequest request) {
        if (request == null) {
            throw new ProductValidationException("Thiếu thông tin sản phẩm.");
        }
        String name = required(request.tenSanPham(), "Tên sản phẩm");
        String line = required(request.dongMay(), "Dòng máy");
        validateLength(name, 150, "Tên sản phẩm");
        validateLength(line, 100, "Dòng máy");
        validateLength(request.manHinh(), 100, "Màn hình");
        validateLength(request.chipXuLy(), 100, "Chip xử lý");
        validateLength(request.pin(), 50, "Pin");
        validateLength(request.camera(), 100, "Camera");
        validateLength(request.heDieuHanh(), 50, "Hệ điều hành");
        if (request.maThuongHieu() == null || request.maThuongHieu() <= 0
                || request.maDanhMuc() == null || request.maDanhMuc() <= 0) {
            throw new ProductValidationException("Vui lòng chọn thương hiệu và danh mục hợp lệ.");
        }
        if (request.thoiGianBaoHanhThang() != null && request.thoiGianBaoHanhThang() < 0) {
            throw new ProductValidationException("Thời gian bảo hành không được âm.");
        }
        if (hasText(request.trangThai())) {
            validateStatus(request.trangThai().trim());
        }
        if (request.hinhAnh() != null) {
            validateLength(request.hinhAnh(), 255, "Đường dẫn ảnh chính");
        }
    }

    private void validateVariant(ProductVariantRequest variant) {
        if (variant == null) {
            throw new ProductValidationException("Thông tin biến thể không hợp lệ.");
        }
        validateLength(required(variant.mauSac(), "Màu sắc"), 50, "Màu sắc");
        validateLength(required(variant.ram(), "RAM"), 20, "RAM");
        validateLength(required(variant.rom(), "ROM"), 20, "ROM");
        BigDecimal price = variant.giaBan();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ProductValidationException("Giá bán phải lớn hơn 0.");
        }
        if (variant.giaNhapThamKhao() != null
                && variant.giaNhapThamKhao().compareTo(BigDecimal.ZERO) < 0) {
            throw new ProductValidationException("Giá nhập tham khảo không được âm.");
        }
        if (variant.dinhMucTonToiThieu() != null && variant.dinhMucTonToiThieu() < 0) {
            throw new ProductValidationException("Định mức tồn tối thiểu không được âm.");
        }
        if (variant.maSanPham() != null) {
            validateLength(variant.maSanPham().trim(), 20, "Mã sản phẩm");
        }
    }

    private void verifyReferences(int brandId, int categoryId) {
        if (!exists("SELECT COUNT(*) FROM thuong_hieu WHERE ma_thuong_hieu = ?", brandId)) {
            throw new ProductValidationException("Thương hiệu không tồn tại.");
        }
        if (!exists("SELECT COUNT(*) FROM danh_muc WHERE ma_danh_muc = ?", categoryId)) {
            throw new ProductValidationException("Danh mục không tồn tại.");
        }
    }

    private List<String> normalizeGallery(List<String> supplied, String mainImage) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        if (supplied != null) {
            for (String url : supplied) {
                if (!hasText(url)) {
                    continue;
                }
                String clean = url.trim();
                validateGalleryUrl(clean);
                urls.add(clean);
            }
        }
        if (hasText(mainImage)) {
            String clean = mainImage.trim();
            validateGalleryUrl(clean);
            urls.add(clean);
        }
        return new ArrayList<>(urls);
    }

    private void validateGalleryUrl(String url) {
        validateLength(url, 500, "Đường dẫn ảnh");
        if (url.chars().anyMatch(Character::isISOControl)) {
            throw new ProductValidationException("Đường dẫn ảnh không hợp lệ.");
        }
    }

    private String primaryImage(List<String> urls, String requestedMain) {
        String primary = hasText(requestedMain) ? requestedMain.trim()
                : (urls.isEmpty() ? null : urls.get(0));
        validateLength(primary, 255, "Đường dẫn ảnh chính");
        return primary;
    }

    private void replaceGallery(String sku, List<String> urls, String primary) {
        jdbcTemplate.update("DELETE FROM hinh_anh_san_pham WHERE ma_san_pham = ?", sku);
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            jdbcTemplate.update(
                    """
                    INSERT INTO hinh_anh_san_pham
                        (ma_san_pham, duong_dan_anh, la_anh_chinh, thu_tu)
                    VALUES (?, ?, ?, ?)
                    """,
                    sku,
                    url,
                    url.equals(primary),
                    i + 1
            );
        }
    }

    private String taoMaSanPham(
            String dongMay,
            ProductVariantRequest variant,
            Set<String> used
    ) {
        String raw = "SP_" + slug(dongMay) + "_" + slug(variant.ram())
                + slug(variant.rom()) + "_" + slug(variant.mauSac());
        String base = raw.substring(0, Math.min(raw.length(), 20));
        String candidate = base;
        int suffix = 2;
        while (used.contains(candidate.toUpperCase())
                || exists("SELECT COUNT(*) FROM san_pham WHERE ma_san_pham = ?", candidate)) {
            String ending = "_" + suffix++;
            candidate = base.substring(0, Math.min(base.length(), 20 - ending.length())) + ending;
        }
        return candidate;
    }

    private String slug(String value) {
        if (!hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("[^A-Za-z0-9]", "");
        return normalized.toUpperCase();
    }

    private void addIntegerFilter(
            StringBuilder sql,
            List<Object> args,
            String column,
            String value,
            String label
    ) {
        if (!hasText(value)) {
            return;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            sql.append(" AND ").append(column).append(" = ?");
            args.add(parsed);
        } catch (NumberFormatException e) {
            throw new ProductValidationException(label + " không hợp lệ.");
        }
    }

    private String orderBy(String sort) {
        if (!hasText(sort) || "newest".equals(sort)) {
            return " ORDER BY sp.ngay_tao DESC, sp.ma_san_pham";
        }
        return switch (sort) {
            case "price_asc" -> " ORDER BY sp.gia_ban ASC, sp.ma_san_pham";
            case "price_desc" -> " ORDER BY sp.gia_ban DESC, sp.ma_san_pham";
            case "name_asc" -> " ORDER BY sp.ten_san_pham ASC, sp.ma_san_pham";
            case "stock_desc" -> " ORDER BY so_luong_ton_hien_tai DESC, sp.ma_san_pham";
            default -> throw new ProductValidationException("Kiểu sắp xếp không hợp lệ.");
        };
    }

    private BigDecimal parsePrice(String value, String label) {
        try {
            BigDecimal price = new BigDecimal(value.trim());
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new NumberFormatException();
            }
            return price;
        } catch (NumberFormatException e) {
            throw new ProductValidationException(label + " không hợp lệ.");
        }
    }

    private void validateStatus(String status) {
        if (!"Dang_kinh_doanh".equals(status) && !"Ngung_kinh_doanh".equals(status)) {
            throw new ProductValidationException("Trạng thái kinh doanh không hợp lệ.");
        }
    }

    private String statusOrDefault(String status) {
        if (!hasText(status)) {
            return "Dang_kinh_doanh";
        }
        validateStatus(status.trim());
        return status.trim();
    }

    private void ensureUnique(String table, String column, String name, Integer excludedId) {
        String idColumn = "danh_muc".equals(table) ? "ma_danh_muc" : "ma_thuong_hieu";
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE LOWER(" + column + ") = LOWER(?)";
        List<Object> args = new ArrayList<>();
        args.add(name);
        if (excludedId != null) {
            sql += " AND " + idColumn + " <> ?";
            args.add(excludedId);
        }
        if (exists(sql, args.toArray())) {
            throw new ProductValidationException("Tên đã được sử dụng.");
        }
    }

    private Map<String, Object> timDanhMuc(int id) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                """
                SELECT ma_danh_muc, ten_danh_muc, mo_ta,
                       (SELECT COUNT(*) FROM san_pham sp
                        WHERE sp.ma_danh_muc = danh_muc.ma_danh_muc) AS so_luong_san_pham
                FROM danh_muc WHERE ma_danh_muc = ?
                """,
                (rs, rowNum) -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("maDanhMuc", rs.getInt("ma_danh_muc"));
                    result.put("tenDanhMuc", rs.getString("ten_danh_muc"));
                    result.put("moTa", rs.getString("mo_ta"));
                    result.put("soLuongSanPham", rs.getInt("so_luong_san_pham"));
                    return result;
                },
                id
        );
        if (rows.isEmpty()) {
            throw new NoSuchElementException("Không tìm thấy danh mục.");
        }
        return rows.get(0);
    }

    private Map<String, Object> timDanhMucTheoTen(String name) {
        List<Map<String, Object>> rows = layDanhMuc(name);
        return rows.stream()
                .filter(row -> name.equals(row.get("tenDanhMuc")))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy danh mục vừa tạo."));
    }

    private Map<String, Object> timThuongHieu(int id) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                """
                SELECT ma_thuong_hieu, ten_thuong_hieu, quoc_gia, mo_ta,
                       (SELECT COUNT(*) FROM san_pham sp
                        WHERE sp.ma_thuong_hieu = thuong_hieu.ma_thuong_hieu) AS so_luong_san_pham
                FROM thuong_hieu WHERE ma_thuong_hieu = ?
                """,
                (rs, rowNum) -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("maThuongHieu", rs.getInt("ma_thuong_hieu"));
                    result.put("tenThuongHieu", rs.getString("ten_thuong_hieu"));
                    result.put("quocGia", rs.getString("quoc_gia"));
                    result.put("moTa", rs.getString("mo_ta"));
                    result.put("soLuongSanPham", rs.getInt("so_luong_san_pham"));
                    return result;
                },
                id
        );
        if (rows.isEmpty()) {
            throw new NoSuchElementException("Không tìm thấy thương hiệu.");
        }
        return rows.get(0);
    }

    private Map<String, Object> timThuongHieuTheoTen(String name) {
        List<Map<String, Object>> rows = layThuongHieu(name);
        return rows.stream()
                .filter(row -> name.equals(row.get("tenThuongHieu")))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thương hiệu vừa tạo."));
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private String required(String value, String label) {
        if (!hasText(value)) {
            throw new ProductValidationException(label + " không được để trống.");
        }
        return value.trim();
    }

    private String emptyToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void validateLength(String value, int maxLength, String label) {
        if (value != null && value.length() > maxLength) {
            throw new ProductValidationException(label + " không được vượt quá " + maxLength + " ký tự.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
