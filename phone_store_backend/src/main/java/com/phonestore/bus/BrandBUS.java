package com.phonestore.bus;

import com.phonestore.dao.BrandDAO;
import com.phonestore.dto.request.BrandRequest;
import com.phonestore.dto.response.ApiMessageResponse;
import com.phonestore.dto.response.BrandDTO;
import com.phonestore.model.Brand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandBUS {
    private final BrandDAO brandDAO;

    public List<BrandDTO> listBrands(String keyword) {
        return brandDAO.findAll(keyword).stream().map(this::toDTO).toList();
    }

    public BrandDTO getBrandById(int id) {
        return toDTO(findExisting(id));
    }

    public BrandDTO createBrand(BrandRequest request) {
        validateRequest(request, null);
        return toDTO(brandDAO.insert(toModel(request)));
    }

    public BrandDTO updateBrand(int id, BrandRequest request) {
        findExisting(id);
        validateRequest(request, id);
        return toDTO(brandDAO.update(id, toModel(request)));
    }

    public ApiMessageResponse deleteBrand(int id) {
        Brand existing = findExisting(id);
        long productCount = brandDAO.countProducts(id);
        if (productCount > 0) {
            throw new IllegalStateException("Không thể xóa thương hiệu '" + existing.getTenThuongHieu()
                    + "' vì đang có " + productCount + " sản phẩm thuộc thương hiệu này.");
        }
        long promotionCount = brandDAO.countPromotions(id);
        if (promotionCount > 0) {
            throw new IllegalStateException("Không thể xóa thương hiệu '" + existing.getTenThuongHieu()
                    + "' vì đang được gắn với " + promotionCount + " chương trình khuyến mãi.");
        }
        brandDAO.deleteById(id);
        return new ApiMessageResponse(true, "Đã xóa thương hiệu '" + existing.getTenThuongHieu() + "' thành công.");
    }

    private Brand findExisting(int id) {
        return brandDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu mã #" + id));
    }

    private void validateRequest(BrandRequest request, Integer excludeId) {
        if (request == null || request.getTenThuongHieu() == null || request.getTenThuongHieu().isBlank())
            throw new IllegalArgumentException("Tên thương hiệu không được để trống");
        String name = request.getTenThuongHieu().trim();
        if (name.length() > 50) throw new IllegalArgumentException("Tên thương hiệu không được vượt quá 50 ký tự");
        if (request.getQuocGia() != null && request.getQuocGia().trim().length() > 50)
            throw new IllegalArgumentException("Tên quốc gia không được vượt quá 50 ký tự");
        if (brandDAO.existsByName(name, excludeId))
            throw new IllegalArgumentException("Tên thương hiệu '" + name + "' đã tồn tại");
    }

    private Brand toModel(BrandRequest request) {
        Brand brand = new Brand();
        brand.setTenThuongHieu(request.getTenThuongHieu().trim());
        brand.setQuocGia(trimOrDefault(request.getQuocGia(), "Quốc tế"));
        brand.setLogoUrl(trimToNull(request.getLogoUrl()));
        brand.setMoTa(trimToNull(request.getMoTa()));
        return brand;
    }

    private BrandDTO toDTO(Brand brand) {
        return BrandDTO.builder()
                .maThuongHieu(brand.getMaThuongHieu())
                .tenThuongHieu(brand.getTenThuongHieu())
                .quocGia(brand.getQuocGia())
                .logoUrl(brand.getLogoUrl())
                .moTa(brand.getMoTa())
                .soLuongSanPham(brand.getSoLuongSanPham())
                .build();
    }

    private String trimOrDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }
}