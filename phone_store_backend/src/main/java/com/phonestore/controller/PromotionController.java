package com.phonestore.controller;

import com.phonestore.dto.request.PromotionRequest;
import com.phonestore.dto.response.ApiMessageResponse;
import com.phonestore.dto.response.PromotionResponse;
import com.phonestore.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public ResponseEntity<List<PromotionResponse>> listPromotions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String loaiGiamGia
    ) {
        return ResponseEntity.ok(promotionService.listPromotions(keyword, trangThai, loaiGiamGia));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<PromotionResponse>> listActiveAlerts() {
        return ResponseEntity.ok(promotionService.listActiveAlerts());
    }

    @GetMapping("/options")
    public ResponseEntity<Map<String, Object>> getMetadataOptions() {
        return ResponseEntity.ok(promotionService.getMetadataOptions());
    }

    @GetMapping("/{maKhuyenMai}")
    public ResponseEntity<PromotionResponse> getPromotion(@PathVariable String maKhuyenMai) {
        return ResponseEntity.ok(promotionService.getPromotionById(maKhuyenMai));
    }

    @PostMapping
    public ResponseEntity<PromotionResponse> createPromotion(@RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.createPromotion(request));
    }

    @PutMapping("/{maKhuyenMai}")
    public ResponseEntity<PromotionResponse> updatePromotion(
            @PathVariable String maKhuyenMai,
            @RequestBody PromotionRequest request
    ) {
        return ResponseEntity.ok(promotionService.updatePromotion(maKhuyenMai, request));
    }

    @PutMapping("/{maKhuyenMai}/status")
    public ResponseEntity<PromotionResponse> updateStatus(
            @PathVariable String maKhuyenMai,
            @RequestBody Map<String, String> payload
    ) {
        return ResponseEntity.ok(promotionService.updateStatus(maKhuyenMai, payload.get("trangThai")));
    }

    @DeleteMapping("/{maKhuyenMai}")
    public ResponseEntity<ApiMessageResponse> deleteOrStopPromotion(@PathVariable String maKhuyenMai) {
        return ResponseEntity.ok(promotionService.deleteOrStopPromotion(maKhuyenMai));
    }
}

