package com.phonestore.controller;

import com.phonestore.dto.request.InventoryCheckCreateRequest;
import com.phonestore.dto.request.UpdateMinStockRequest;
import com.phonestore.dto.response.InventoryCheckDetailResponse;
import com.phonestore.entity.InventoryCheck;
import com.phonestore.entity.Product;
import com.phonestore.entity.ProductImei;
import com.phonestore.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(inventoryService.getAllProducts());
    }

    @GetMapping("/warnings")
    public ResponseEntity<List<Product>> getLowStockWarnings() {
        return ResponseEntity.ok(inventoryService.getLowStockWarnings());
    }

    @PutMapping("/products/{maSanPham}/min-stock")
    public ResponseEntity<Product> updateMinStock(
            @PathVariable String maSanPham,
            @RequestBody UpdateMinStockRequest request) {
        return ResponseEntity.ok(inventoryService.updateMinStock(maSanPham, request.getDinhMucTonToiThieu()));
    }

    @GetMapping("/products/{maSanPham}/imeis")
    public ResponseEntity<List<ProductImei>> getProductImeis(@PathVariable String maSanPham) {
        return ResponseEntity.ok(inventoryService.getImeisInStock(maSanPham));
    }

    @GetMapping("/checks")
    public ResponseEntity<List<InventoryCheck>> getAllInventoryChecks() {
        return ResponseEntity.ok(inventoryService.getAllInventoryChecks());
    }

    @GetMapping("/checks/{maPhieuKiemKe}")
    public ResponseEntity<InventoryCheckDetailResponse> getInventoryCheckDetail(@PathVariable String maPhieuKiemKe) {
        return ResponseEntity.ok(inventoryService.getInventoryCheckDetail(maPhieuKiemKe));
    }

    @PostMapping("/checks")
    public ResponseEntity<InventoryCheck> createInventoryCheck(@RequestBody InventoryCheckCreateRequest request) {
        return ResponseEntity.ok(inventoryService.createInventoryCheck(request));
    }
}
