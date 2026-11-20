package com.phonestore.controller;

import com.phonestore.dto.request.PurchaseOrderCreateRequest;
import com.phonestore.dto.response.PurchaseOrderDetailResponse;
import com.phonestore.entity.PurchaseOrder;
import com.phonestore.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@CrossOrigin(origins = "*")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> getAllPurchaseOrders() {
        return ResponseEntity.ok(purchaseOrderService.getAllPurchaseOrders());
    }

    @GetMapping("/{maPhieuNhap}")
    public ResponseEntity<PurchaseOrderDetailResponse> getOrderDetail(@PathVariable String maPhieuNhap) {
        return ResponseEntity.ok(purchaseOrderService.getOrderDetail(maPhieuNhap));
    }

    @PostMapping
    public ResponseEntity<PurchaseOrder> createPurchaseOrder(@RequestBody PurchaseOrderCreateRequest request) {
        return ResponseEntity.ok(purchaseOrderService.createPurchaseOrder(request));
    }
}
