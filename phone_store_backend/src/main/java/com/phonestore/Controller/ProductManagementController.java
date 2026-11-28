package com.phonestore.Controller;

import com.phonestore.dao.ProductManagementDAO;
import com.phonestore.dto.product.BrandRequest;
import com.phonestore.dto.product.CategoryRequest;
import com.phonestore.dto.product.ProductRequest;
import com.phonestore.dto.product.ProductStatusRequest;
import com.phonestore.dto.product.ProductConflictException;
import com.phonestore.dto.product.ProductValidationException;
import com.phonestore.service.ProductImageStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
public class ProductManagementController {

    private static final Logger log = LoggerFactory.getLogger(ProductManagementController.class);

    private final ProductManagementDAO productManagementDAO;
    private final ProductImageStorageService imageStorageService;

    public ProductManagementController(
            ProductManagementDAO productManagementDAO,
            ProductImageStorageService imageStorageService
    ) {
        this.productManagementDAO = productManagementDAO;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/products")
    public ResponseEntity<?> layDanhSachSanPham(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String maThuongHieu,
            @RequestParam(required = false) String maDanhMuc,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(required = false) String ram,
            @RequestParam(required = false) String rom,
            @RequestParam(required = false) String mauSac,
            @RequestParam(required = false) String sort
    ) {
        try {
            return ResponseEntity.ok(productManagementDAO.layDanhSachSanPham(
                    keyword, maThuongHieu, maDanhMuc, trangThai,
                    minPrice, maxPrice, ram, rom, mauSac, sort
            ));
        } catch (ProductValidationException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Không thể tải danh sách sản phẩm.", e);
            return ResponseEntity.internalServerError().body(error("Không thể tải danh sách sản phẩm."));
        }
    }

    @GetMapping("/products/filters")
    public ResponseEntity<?> layTuyChonBoLoc() {
        try {
            return ResponseEntity.ok(productManagementDAO.layTuyChonBoLoc());
        } catch (Exception e) {
            log.error("Không thể tải tùy chọn lọc sản phẩm.", e);
            return ResponseEntity.internalServerError().body(error("Không thể tải tùy chọn lọc sản phẩm."));
        }
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> layChiTietSanPham(@PathVariable String id) {
        try {
            return ResponseEntity.ok(productManagementDAO.timSanPham(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Không thể tải chi tiết sản phẩm {}.", id, e);
            return ResponseEntity.internalServerError().body(error("Không thể tải chi tiết sản phẩm."));
        }
    }

    @PostMapping("/products/with-variants")
    public ResponseEntity<?> taoSanPham(@RequestBody ProductRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(productManagementDAO.taoSanPham(request));
        } catch (ProductValidationException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.warn("Không thể tạo sản phẩm: dữ liệu trùng hoặc vi phạm ràng buộc.", e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(error("Mã sản phẩm đã tồn tại hoặc dữ liệu tham chiếu không hợp lệ."));
        } catch (Exception e) {
            log.error("Không thể tạo sản phẩm.", e);
            return ResponseEntity.internalServerError().body(error("Không thể tạo sản phẩm."));
        }
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> capNhatSanPham(
            @PathVariable String id,
            @RequestBody ProductRequest request
    ) {
        try {
            return ResponseEntity.ok(productManagementDAO.capNhatSanPham(id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (ProductValidationException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.warn("Không thể cập nhật sản phẩm {} do vi phạm ràng buộc.", id, e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(error("Không thể cập nhật sản phẩm do dữ liệu tham chiếu không hợp lệ."));
        } catch (Exception e) {
            log.error("Không thể cập nhật sản phẩm {}.", id, e);
            return ResponseEntity.internalServerError().body(error("Không thể cập nhật sản phẩm."));
        }
    }

    @PutMapping("/products/{id}/status")
    public ResponseEntity<?> capNhatTrangThai(
            @PathVariable String id,
            @RequestBody ProductStatusRequest request
    ) {
        try {
            return ResponseEntity.ok(productManagementDAO.capNhatTrangThai(
                    id,
                    request == null ? null : request.trangThai()
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (ProductValidationException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Không thể cập nhật trạng thái sản phẩm {}.", id, e);
            return ResponseEntity.internalServerError().body(error("Không thể cập nhật trạng thái sản phẩm."));
        }
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> xoaSanPham(@PathVariable String id) {
        try {
            return ResponseEntity.ok(productManagementDAO.xoaSanPham(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.warn("Không thể xóa sản phẩm {} do đang được dữ liệu khác tham chiếu.", id, e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(error("Sản phẩm đang được dữ liệu khác sử dụng."));
        } catch (Exception e) {
            log.error("Không thể xóa sản phẩm {}.", id, e);
            return ResponseEntity.internalServerError().body(error("Không thể xóa sản phẩm."));
        }
    }

    @PostMapping(value = "/products/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> taiAnhSanPham(@RequestParam("files") List<MultipartFile> files) {
        try {
            List<String> urls = imageStorageService.store(files);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("urls", urls);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Không thể lưu ảnh sản phẩm.", e);
            return ResponseEntity.internalServerError().body(error("Không thể lưu ảnh sản phẩm."));
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<?> layDanhMuc(@RequestParam(required = false) String keyword) {
        try {
            return ResponseEntity.ok(productManagementDAO.layDanhMuc(keyword));
        } catch (Exception e) {
            log.error("Không thể tải danh mục sản phẩm.", e);
            return ResponseEntity.internalServerError().body(error("Không thể tải danh mục sản phẩm."));
        }
    }

    @PostMapping("/categories")
    public ResponseEntity<?> taoDanhMuc(@RequestBody CategoryRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(productManagementDAO.taoDanhMuc(request));
        } catch (ProductValidationException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.warn("Không thể tạo danh mục do dữ liệu trùng.", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error("Tên danh mục đã tồn tại."));
        } catch (Exception e) {
            log.error("Không thể tạo danh mục.", e);
            return ResponseEntity.internalServerError().body(error("Không thể tạo danh mục."));
        }
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<?> capNhatDanhMuc(
            @PathVariable int id,
            @RequestBody CategoryRequest request
    ) {
        try {
            return ResponseEntity.ok(productManagementDAO.capNhatDanhMuc(id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (ProductValidationException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.warn("Không thể cập nhật danh mục {} do dữ liệu trùng.", id, e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error("Tên danh mục đã tồn tại."));
        } catch (Exception e) {
            log.error("Không thể cập nhật danh mục {}.", id, e);
            return ResponseEntity.internalServerError().body(error("Không thể cập nhật danh mục."));
        }
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> xoaDanhMuc(@PathVariable int id) {
        try {
            productManagementDAO.xoaDanhMuc(id);
            return ResponseEntity.ok(success("Đã xóa danh mục."));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (ProductConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Không thể xóa danh mục {}.", id, e);
            return ResponseEntity.internalServerError().body(error("Không thể xóa danh mục."));
        }
    }

    @GetMapping("/brands")
    public ResponseEntity<?> layThuongHieu(@RequestParam(required = false) String keyword) {
        try {
            return ResponseEntity.ok(productManagementDAO.layThuongHieu(keyword));
        } catch (Exception e) {
            log.error("Không thể tải thương hiệu.", e);
            return ResponseEntity.internalServerError().body(error("Không thể tải thương hiệu."));
        }
    }

    @PostMapping("/brands")
    public ResponseEntity<?> taoThuongHieu(@RequestBody BrandRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(productManagementDAO.taoThuongHieu(request));
        } catch (ProductValidationException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.warn("Không thể tạo thương hiệu do dữ liệu trùng.", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error("Tên thương hiệu đã tồn tại."));
        } catch (Exception e) {
            log.error("Không thể tạo thương hiệu.", e);
            return ResponseEntity.internalServerError().body(error("Không thể tạo thương hiệu."));
        }
    }

    @PutMapping("/brands/{id}")
    public ResponseEntity<?> capNhatThuongHieu(
            @PathVariable int id,
            @RequestBody BrandRequest request
    ) {
        try {
            return ResponseEntity.ok(productManagementDAO.capNhatThuongHieu(id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (ProductValidationException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.warn("Không thể cập nhật thương hiệu {} do dữ liệu trùng.", id, e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error("Tên thương hiệu đã tồn tại."));
        } catch (Exception e) {
            log.error("Không thể cập nhật thương hiệu {}.", id, e);
            return ResponseEntity.internalServerError().body(error("Không thể cập nhật thương hiệu."));
        }
    }

    @DeleteMapping("/brands/{id}")
    public ResponseEntity<?> xoaThuongHieu(@PathVariable int id) {
        try {
            productManagementDAO.xoaThuongHieu(id);
            return ResponseEntity.ok(success("Đã xóa thương hiệu."));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (ProductConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Không thể xóa thương hiệu {}.", id, e);
            return ResponseEntity.internalServerError().body(error("Không thể xóa thương hiệu."));
        }
    }

    private Map<String, Object> success(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", message);
        return body;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        return body;
    }
}
