package com.phonestore.service;

import com.phonestore.dao.CategoryDao;
import com.phonestore.dto.request.CategoryRequest;
import com.phonestore.dto.response.ApiMessageResponse;
import com.phonestore.dto.response.CategoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryDao categoryDao;

    public List<CategoryDTO> listCategories(String keyword) {
        return categoryDao.findAll(keyword);
    }

    public CategoryDTO getCategoryById(int id) {
        return categoryDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục mã #" + id));
    }

    public CategoryDTO createCategory(CategoryRequest req) {
        validateRequest(req, null);
        return categoryDao.insert(req);
    }

    public CategoryDTO updateCategory(int id, CategoryRequest req) {
        getCategoryById(id);
        validateRequest(req, id);
        return categoryDao.update(id, req);
    }

    public ApiMessageResponse deleteCategory(int id) {
        CategoryDTO existing = getCategoryById(id);
        long productCount = categoryDao.countProducts(id);
        if (productCount > 0) {
            throw new IllegalStateException(
                    "Không thể xóa danh mục '" + existing.getTenDanhMuc()
                            + "' vì đang có " + productCount + " sản phẩm thuộc danh mục này.");
        }
        categoryDao.deleteById(id);
        return new ApiMessageResponse(true, "Đã xóa danh mục '" + existing.getTenDanhMuc() + "' thành công.");
    }

    private void validateRequest(CategoryRequest req, Integer excludeId) {
        if (req == null || req.getTenDanhMuc() == null || req.getTenDanhMuc().isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống");
        }
        if (req.getTenDanhMuc().trim().length() > 100) {
            throw new IllegalArgumentException("Tên danh mục không được vượt quá 100 ký tự");
        }
        if (req.getMoTa() != null && req.getMoTa().trim().length() > 255) {
            throw new IllegalArgumentException("Mô tả danh mục không được vượt quá 255 ký tự");
        }
        if (categoryDao.existsByName(req.getTenDanhMuc(), excludeId)) {
            throw new IllegalArgumentException("Tên danh mục '" + req.getTenDanhMuc().trim() + "' đã tồn tại");
        }
    }
}
