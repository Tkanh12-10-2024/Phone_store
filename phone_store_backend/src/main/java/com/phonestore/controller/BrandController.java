package com.phonestore.controller;

import com.phonestore.bus.BrandBUS;
import com.phonestore.dto.request.BrandRequest;
import com.phonestore.dto.response.ApiMessageResponse;
import com.phonestore.dto.response.BrandDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * HTTP adapter for brand endpoints.
 * Keeps the existing routes and JSON contract used by the HTML/JavaScript frontend.
 */
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BrandController {

    private final BrandBUS brandBUS;

    @GetMapping
    public ResponseEntity<List<BrandDTO>> listBrands(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(brandBUS.listBrands(keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BrandDTO> getBrand(@PathVariable int id) {
        return ResponseEntity.ok(brandBUS.getBrandById(id));
    }

    @PostMapping
    public ResponseEntity<BrandDTO> createBrand(@RequestBody BrandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brandBUS.createBrand(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BrandDTO> updateBrand(@PathVariable int id, @RequestBody BrandRequest request) {
        return ResponseEntity.ok(brandBUS.updateBrand(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiMessageResponse> deleteBrand(@PathVariable int id) {
        return ResponseEntity.ok(brandBUS.deleteBrand(id));
    }
}