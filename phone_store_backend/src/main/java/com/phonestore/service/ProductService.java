package com.phonestore.service;

import com.phonestore.dao.BrandDao;
import com.phonestore.dao.CategoryDao;
import com.phonestore.dao.ProductDao;
import com.phonestore.dto.request.ProductGalleryRequest;
import com.phonestore.dto.request.ProductRequest;
import com.phonestore.dto.request.ProductVariantItemRequest;
import com.phonestore.dto.response.ApiMessageResponse;
import com.phonestore.dto.response.ProductDTO;
import com.phonestore.dto.response.ProductGalleryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Set<String> VALID_STATUSES = Set.of("Dang_kinh_doanh", "Ngung_kinh_doanh");
    private static final Pattern SKU_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{2,20}$");

    private final ProductDao productDao;
    private final BrandDao brandDao;
    private final CategoryDao categoryDao;

    public List<ProductDTO> listProducts(
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
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Giá tối thiểu không được lớn hơn giá tối đa");
        }
        return productDao.findAll(keyword, maThuongHieu, maDanhMuc, minPrice, maxPrice,
                ram, rom, mauSac, heDieuHanh, trangThai, sort);
    }

    public ProductDTO getProductById(String maSanPham) {
        return productDao.findById(maSanPham)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm mã: " + maSanPham));
    }

    public Map<String, Object> getFilterOptions() {
        return productDao.getFilterOptions();
    }

    @Transactional
    public ProductDTO createProduct(ProductRequest req) {
        validateCommonFields(req, false);
        validateSingleVariantFields(req.getTenSanPham(), req.getMauSac(), req.getRam(), req.getRom(),
                req.getGiaNhapThamKhao(), req.getGiaBan(), req.getDinhMucTonToiThieu());

        Set<String> emptyBatch = new HashSet<>();
        String sku = resolveUniqueSku(req.getMaSanPham(), req.getDongMay(), req.getRom(), req.getMauSac(), emptyBatch);
        if (productDao.existsById(sku)) {
            throw new IllegalArgumentException("Mã sản phẩm '" + sku + "' đã tồn tại trên hệ thống");
        }

        String primaryImage = resolvePrimaryImage(req.getHinhAnh(), req.getGalleryUrls());
        productDao.insert(sku, req, primaryImage);
        productDao.replaceGalleries(sku, req.getGalleryUrls(), primaryImage);
        return getProductById(sku);
    }

    @Transactional
    public List<ProductDTO> createProductWithVariants(ProductRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Dữ liệu sản phẩm không được để trống");
        }
        if (req.getVariants() == null || req.getVariants().isEmpty()) {
            return List.of(createProduct(req));
        }

        validateCommonFields(req, true);

        Set<String> batchSkus = new HashSet<>();
        List<ProductDTO> createdProducts = new ArrayList<>();

        for (int i = 0; i < req.getVariants().size(); i++) {
            ProductVariantItemRequest item = req.getVariants().get(i);
            if (item == null) {
                throw new IllegalArgumentException("Biến thể thứ " + (i + 1) + " không hợp lệ");
            }

            String effectiveName = (item.getTenSanPham() != null && !item.getTenSanPham().isBlank())
                    ? item.getTenSanPham().trim()
                    : buildVariantProductName(req.getTenSanPham(), item.getRom(), item.getMauSac());
            BigDecimal giaNhap = item.getGiaNhapThamKhao() != null ? item.getGiaNhapThamKhao() : req.getGiaNhapThamKhao();
            BigDecimal giaBan = item.getGiaBan() != null ? item.getGiaBan() : req.getGiaBan();
            Integer minStock = item.getDinhMucTonToiThieu() != null ? item.getDinhMucTonToiThieu() : req.getDinhMucTonToiThieu();

            validateSingleVariantFields(effectiveName, item.getMauSac(), item.getRam(), item.getRom(), giaNhap, giaBan, minStock);

            String sku = resolveUniqueSku(item.getMaSanPham(), req.getDongMay(), item.getRom(), item.getMauSac(), batchSkus);
            if (productDao.existsById(sku)) {
                throw new IllegalArgumentException("Mã biến thể '" + sku + "' đã tồn tại trên hệ thống");
            }
            batchSkus.add(sku);

            String variantPrimaryImg = resolvePrimaryImage(
                    item.getHinhAnh() != null && !item.getHinhAnh().isBlank() ? item.getHinhAnh() : req.getHinhAnh(),
                    req.getGalleryUrls()
            );

            ProductRequest singleReq = ProductRequest.builder()
                    .maSanPham(sku)
                    .tenSanPham(effectiveName)
                    .maThuongHieu(req.getMaThuongHieu())
                    .maDanhMuc(req.getMaDanhMuc())
                    .dongMay(req.getDongMay())
                    .mauSac(item.getMauSac())
                    .ram(item.getRam())
                    .rom(item.getRom())
                    .manHinh(req.getManHinh())
                    .chipXuLy(req.getChipXuLy())
                    .pin(req.getPin())
                    .camera(req.getCamera())
                    .heDieuHanh(req.getHeDieuHanh())
                    .giaNhapThamKhao(giaNhap)
                    .giaBan(giaBan)
                    .thoiGianBaoHanhThang(req.getThoiGianBaoHanhThang())
                    .dinhMucTonToiThieu(minStock)
                    .hinhAnh(variantPrimaryImg)
                    .trangThai(req.getTrangThai())
                    .galleryUrls(req.getGalleryUrls())
                    .build();

            productDao.insert(sku, singleReq, variantPrimaryImg);
            productDao.replaceGalleries(sku, req.getGalleryUrls(), variantPrimaryImg);
            createdProducts.add(getProductById(sku));
        }

        return createdProducts;
    }

    @Transactional
    public ProductDTO updateProduct(String maSanPham, ProductRequest req) {
        ProductDTO existing = getProductById(maSanPham);
        validateCommonFields(req, false);
        validateSingleVariantFields(req.getTenSanPham(), req.getMauSac(), req.getRam(), req.getRom(),
                req.getGiaNhapThamKhao(), req.getGiaBan(), req.getDinhMucTonToiThieu());

        String primaryImage = resolvePrimaryImage(
                req.getHinhAnh() != null ? req.getHinhAnh() : existing.getHinhAnh(),
                req.getGalleryUrls()
        );

        productDao.update(maSanPham, req, primaryImage);
        if (req.getGalleryUrls() != null) {
            productDao.replaceGalleries(maSanPham, req.getGalleryUrls(), primaryImage);
        }
        return getProductById(maSanPham);
    }

    public ProductDTO updateStatus(String maSanPham, String trangThai) {
        getProductById(maSanPham);
        if (trangThai == null || !VALID_STATUSES.contains(trangThai.trim())) {
            throw new IllegalArgumentException("Trạng thái sản phẩm chỉ chấp nhận 'Dang_kinh_doanh' hoặc 'Ngung_kinh_doanh'");
        }
        productDao.updateStatus(maSanPham, trangThai.trim());
        return getProductById(maSanPham);
    }

    public Map<String, Object> deleteOrDeactivateProduct(String maSanPham) {
        ProductDTO existing = getProductById(maSanPham);
        if (productDao.hasRelatedHistory(maSanPham)) {
            productDao.updateStatus(maSanPham, "Ngung_kinh_doanh");
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("action", "DEACTIVATED");
            resp.put("message", "Sản phẩm '" + existing.getTenSanPham()
                    + "' đã phát sinh dữ liệu nhập kho/bán hàng/bảo hành nên hệ thống đã chuyển sang 'Ngừng kinh doanh' để bảo toàn lịch sử.");
            return resp;
        }
        productDao.deleteById(maSanPham);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("action", "DELETED");
        resp.put("message", "Đã xóa vĩnh viễn sản phẩm '" + existing.getTenSanPham() + "'.");
        return resp;
    }

    public List<ProductGalleryDTO> getGalleries(String maSanPham) {
        getProductById(maSanPham);
        return productDao.findGalleriesByProduct(maSanPham);
    }

    public ProductGalleryDTO addGalleryImage(String maSanPham, ProductGalleryRequest req) {
        getProductById(maSanPham);
        if (req == null || req.getDuongDanAnh() == null || req.getDuongDanAnh().isBlank()) {
            throw new IllegalArgumentException("Đường dẫn ảnh không được để trống");
        }
        boolean isPrimary = Boolean.TRUE.equals(req.getLaAnhChinh());
        return productDao.addGalleryImage(maSanPham, req.getDuongDanAnh(), isPrimary, req.getThuTu());
    }

    public List<ProductGalleryDTO> setPrimaryGalleryImage(String maSanPham, int maHinhAnh) {
        getProductById(maSanPham);
        if (!productDao.setPrimaryGalleryImage(maSanPham, maHinhAnh)) {
            throw new IllegalArgumentException("Không tìm thấy hình ảnh #" + maHinhAnh + " của sản phẩm " + maSanPham);
        }
        return productDao.findGalleriesByProduct(maSanPham);
    }

    public ApiMessageResponse deleteGalleryImage(int maHinhAnh) {
        if (!productDao.deleteGalleryImage(maHinhAnh)) {
            throw new IllegalArgumentException("Không tìm thấy hình ảnh mã #" + maHinhAnh);
        }
        return new ApiMessageResponse(true, "Đã xóa hình ảnh khỏi thư viện sản phẩm");
    }

    private void validateCommonFields(ProductRequest req, boolean hasVariantsList) {
        if (req == null) {
            throw new IllegalArgumentException("Dữ liệu sản phẩm không được để trống");
        }
        if (!hasVariantsList && (req.getTenSanPham() == null || req.getTenSanPham().isBlank())) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }
        if (hasVariantsList && (req.getTenSanPham() == null || req.getTenSanPham().isBlank())) {
            boolean allVariantNamesProvided = req.getVariants().stream()
                    .allMatch(v -> v != null && v.getTenSanPham() != null && !v.getTenSanPham().isBlank());
            if (!allVariantNamesProvided) {
                throw new IllegalArgumentException("Vui lòng nhập tên dòng sản phẩm chung hoặc tên riêng cho từng biến thể");
            }
        }
        if (req.getMaThuongHieu() == null || !brandDao.existsById(req.getMaThuongHieu())) {
            throw new IllegalArgumentException("Thương hiệu không hợp lệ hoặc không tồn tại");
        }
        if (req.getMaDanhMuc() == null || !categoryDao.existsById(req.getMaDanhMuc())) {
            throw new IllegalArgumentException("Danh mục không hợp lệ hoặc không tồn tại");
        }
        if (req.getDongMay() == null || req.getDongMay().isBlank()) {
            throw new IllegalArgumentException("Dòng máy không được để trống");
        }
        if (req.getThoiGianBaoHanhThang() != null && req.getThoiGianBaoHanhThang() < 0) {
            throw new IllegalArgumentException("Thời gian bảo hành không được âm");
        }
        if (req.getTrangThai() != null && !req.getTrangThai().isBlank()
                && !VALID_STATUSES.contains(req.getTrangThai().trim())) {
            throw new IllegalArgumentException("Trạng thái sản phẩm không hợp lệ");
        }
    }

    private void validateSingleVariantFields(
            String tenSanPham,
            String mauSac,
            String ram,
            String rom,
            BigDecimal giaNhap,
            BigDecimal giaBan,
            Integer dinhMucTon
    ) {
        if (tenSanPham == null || tenSanPham.isBlank()) {
            throw new IllegalArgumentException("Tên sản phẩm biến thể không được để trống");
        }
        if (tenSanPham.trim().length() > 150) {
            throw new IllegalArgumentException("Tên sản phẩm tối đa 150 ký tự");
        }
        if (mauSac == null || mauSac.isBlank()) {
            throw new IllegalArgumentException("Màu sắc không được để trống");
        }
        if (ram == null || ram.isBlank()) {
            throw new IllegalArgumentException("Dung lượng RAM không được để trống");
        }
        if (rom == null || rom.isBlank()) {
            throw new IllegalArgumentException("Dung lượng ROM không được để trống");
        }
        if (giaNhap != null && giaNhap.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá nhập tham khảo không được âm");
        }
        if (giaBan == null || giaBan.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá bán phải lớn hơn 0đ");
        }
        if (dinhMucTon != null && dinhMucTon < 0) {
            throw new IllegalArgumentException("Định mức tồn tối thiểu không được âm");
        }
    }

    private String resolveUniqueSku(String requestedCode, String dongMay, String rom, String mauSac, Set<String> batchSkus) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            String clean = requestedCode.trim().toUpperCase(Locale.ROOT);
            if (clean.length() > 20 || !SKU_PATTERN.matcher(clean).matches()) {
                throw new IllegalArgumentException("Mã sản phẩm '" + clean + "' phải từ 2-20 ký tự chữ, số hoặc dấu _/-");
            }
            if (batchSkus.contains(clean)) {
                throw new IllegalArgumentException("Mã biến thể '" + clean + "' bị trùng trong danh sách tạo mới");
            }
            return clean;
        }

        String linePart = compactAscii(dongMay, 8);
        String romPart = compactAscii(rom, 4);
        String colorPart = compactAscii(mauSac, 3);
        String base = "SP_" + (linePart.isEmpty() ? "PHONE" : linePart);
        if (!romPart.isEmpty()) base += "_" + romPart;
        if (!colorPart.isEmpty()) base += colorPart;
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }

        String candidate = base;
        int seq = 1;
        while (batchSkus.contains(candidate) || productDao.existsById(candidate)) {
            String suffix = "_" + seq;
            int maxBaseLen = 20 - suffix.length();
            candidate = (base.length() > maxBaseLen ? base.substring(0, maxBaseLen) : base) + suffix;
            seq++;
        }
        return candidate;
    }

    private String buildVariantProductName(String baseName, String rom, String mauSac) {
        String base = baseName == null ? "" : baseName.trim();
        String combined = (base + " " + rom.trim() + " " + mauSac.trim()).trim();
        return combined.length() > 150 ? combined.substring(0, 150) : combined;
    }

    private String resolvePrimaryImage(String explicitPrimary, List<String> galleryUrls) {
        if (explicitPrimary != null && !explicitPrimary.isBlank()) {
            return explicitPrimary.trim();
        }
        if (galleryUrls != null) {
            for (String url : galleryUrls) {
                if (url != null && !url.isBlank()) {
                    return url.trim();
                }
            }
        }
        return null;
    }

    private String compactAscii(String input, int maxLen) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");
        return normalized.length() > maxLen ? normalized.substring(0, maxLen) : normalized;
    }
}
