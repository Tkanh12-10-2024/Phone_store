package com.phonestore.service;

import com.phonestore.dao.BrandDao;
import com.phonestore.dto.request.BrandRequest;
import com.phonestore.dto.response.ApiMessageResponse;
import com.phonestore.dto.response.BrandDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandDao brandDao;

    public List<BrandDTO> listBrands(String keyword) {
        return brandDao.findAll(keyword);
    }

    public BrandDTO getBrandById(int id) {
        return brandDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu mã #" + id));
    }

    public BrandDTO createBrand(BrandRequest req) {
        validateRequest(req, null);
        return brandDao.insert(req);
    }

    public BrandDTO updateBrand(int id, BrandRequest req) {
        getBrandById(id);
        validateRequest(req, id);
        return brandDao.update(id, req);
    }

    public ApiMessageResponse deleteBrand(int id) {
        BrandDTO existing = getBrandById(id);
        long productCount = brandDao.countProducts(id);
        if (productCount > 0) {
            throw new IllegalStateException(
                    "Không thể xóa thương hiệu '" + existing.getTenThuongHieu()
                            + "' vì đang có " + productCount + " sản phẩm thuộc thương hiệu này.");
        }
        long promoCount = brandDao.countPromotions(id);
        if (promoCount > 0) {
            throw new IllegalStateException(
                    "Không thể xóa thương hiệu '" + existing.getTenThuongHieu()
                            + "' vì đang được gắn với " + promoCount + " chương trình khuyến mãi.");
        }
        brandDao.deleteById(id);
        return new ApiMessageResponse(true, "Đã xóa thương hiệu '" + existing.getTenThuongHieu() + "' thành công.");
    }

    private void validateRequest(BrandRequest req, Integer excludeId) {
        if (req == null || req.getTenThuongHieu() == null || req.getTenThuongHieu().isBlank()) {
            throw new IllegalArgumentException("Tên thương hiệu không được để trống");
        }
        if (req.getTenThuongHieu().trim().length() > 50) {
            throw new IllegalArgumentException("Tên thương hiệu không được vượt quá 50 ký tự");
        }
        if (req.getQuocGia() != null && req.getQuocGia().trim().length() > 50) {
            throw new IllegalArgumentException("Tên quốc gia không được vượt quá 50 ký tự");
        }
        if (brandDao.existsByName(req.getTenThuongHieu(), excludeId)) {
            throw new IllegalArgumentException("Tên thương hiệu '" + req.getTenThuongHieu().trim() + "' đã tồn tại");
        }
    }
}
