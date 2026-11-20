package com.phonestore.controller;

import com.phonestore.dto.request.ProductGalleryRequest;
import com.phonestore.dto.request.ProductRequest;
import com.phonestore.dto.response.ApiMessageResponse;
import com.phonestore.dto.response.ProductDTO;
import com.phonestore.dto.response.ProductGalleryDTO;
import com.phonestore.service.FileStorageService;
import com.phonestore.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<List<ProductDTO>> listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer maThuongHieu,
            @RequestParam(required = false) Integer maDanhMuc,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String ram,
            @RequestParam(required = false) String rom,
            @RequestParam(required = false) String mauSac,
            @RequestParam(required = false) String heDieuHanh,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(productService.listProducts(
                keyword, maThuongHieu, maDanhMuc, minPrice, maxPrice,
                ram, rom, mauSac, heDieuHanh, trangThai, sort
        ));
    }

    @GetMapping("/filters")
    public ResponseEntity<Map<String, Object>> getFilterOptions() {
        return ResponseEntity.ok(productService.getFilterOptions());
    }

    @GetMapping("/{maSanPham}")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable String maSanPham) {
        return ResponseEntity.ok(productService.getProductById(maSanPham));
    }

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PostMapping("/with-variants")
    public ResponseEntity<List<ProductDTO>> createProductWithVariants(@RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProductWithVariants(request));
    }

    @PutMapping("/{maSanPham}")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable String maSanPham,
            @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(productService.updateProduct(maSanPham, request));
    }

    @PutMapping("/{maSanPham}/status")
    public ResponseEntity<ProductDTO> updateProductStatus(
            @PathVariable String maSanPham,
            @RequestBody Map<String, String> payload
    ) {
        String trangThai = payload == null ? null : payload.get("trangThai");
        return ResponseEntity.ok(productService.updateStatus(maSanPham, trangThai));
    }

    @DeleteMapping("/{maSanPham}")
    public ResponseEntity<Map<String, Object>> deleteOrDeactivateProduct(@PathVariable String maSanPham) {
        return ResponseEntity.ok(productService.deleteOrDeactivateProduct(maSanPham));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadProductImages(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile singleFile
    ) {
        List<String> urls = new ArrayList<>();
        if (files != null) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) {
                    urls.add(fileStorageService.storeProductImage(f));
                }
            }
        }
        if (singleFile != null && !singleFile.isEmpty()) {
            urls.add(fileStorageService.storeProductImage(singleFile));
        }
        if (urls.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một tệp hình ảnh để tải lên");
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("urls", urls);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{maSanPham}/galleries")
    public ResponseEntity<List<ProductGalleryDTO>> listGalleries(@PathVariable String maSanPham) {
        return ResponseEntity.ok(productService.getGalleries(maSanPham));
    }

    @PostMapping("/{maSanPham}/galleries")
    public ResponseEntity<ProductGalleryDTO> addGalleryImage(
            @PathVariable String maSanPham,
            @RequestBody ProductGalleryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addGalleryImage(maSanPham, request));
    }

    @PutMapping("/{maSanPham}/galleries/{maHinhAnh}/primary")
    public ResponseEntity<List<ProductGalleryDTO>> setPrimaryGalleryImage(
            @PathVariable String maSanPham,
            @PathVariable int maHinhAnh
    ) {
        return ResponseEntity.ok(productService.setPrimaryGalleryImage(maSanPham, maHinhAnh));
    }

    @DeleteMapping("/galleries/{maHinhAnh}")
    public ResponseEntity<ApiMessageResponse> deleteGalleryImage(@PathVariable int maHinhAnh) {
        return ResponseEntity.ok(productService.deleteGalleryImage(maHinhAnh));
    }
}
