package com.phonestore.controller;

import com.phonestore.dto.request.WarrantyCreateRequest;
import com.phonestore.dto.request.WarrantyUpdateRequest;
import com.phonestore.dto.response.ApiMessageResponse;
import com.phonestore.dto.response.WarrantyCheckResult;
import com.phonestore.dto.response.WarrantyTicketDTO;
import com.phonestore.service.WarrantyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warranty")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WarrantyController {

    private final WarrantyService warrantyService;

    // POST /api/warranty/check  body: {imei: "..."}
    @PostMapping("/check")
    public ResponseEntity<?> checkWarranty(@RequestBody Map<String, String> body) {
        try {
            String imei = body.get("imei");
            WarrantyCheckResult result = warrantyService.checkWarranty(imei);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(false, e.getMessage()));
        }
    }

    // GET /api/warranty?keyword=&trangThai=
    @GetMapping
    public ResponseEntity<List<WarrantyTicketDTO>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai) {
        return ResponseEntity.ok(warrantyService.getAll(keyword, trangThai));
    }

    // GET /api/warranty/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(warrantyService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(false, e.getMessage()));
        }
    }

    // POST /api/warranty
    @PostMapping
    public ResponseEntity<?> create(@RequestBody WarrantyCreateRequest req) {
        try {
            String maPhieu = warrantyService.createTicket(req);
            return ResponseEntity.ok(Map.of("maPhieuBh", maPhieu, "message", "Tạo phiếu bảo hành thành công."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(false, e.getMessage()));
        }
    }

    // PUT /api/warranty/{id}/progress
    @PutMapping("/{id}/progress")
    public ResponseEntity<?> updateProgress(@PathVariable String id, @RequestBody WarrantyUpdateRequest req) {
        try {
            warrantyService.updateProgress(id, req);
            return ResponseEntity.ok(new ApiMessageResponse(true, "Cập nhật tiến độ thành công."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(false, e.getMessage()));
        }
    }

    // PUT /api/warranty/{id}/close
    @PutMapping("/{id}/close")
    public ResponseEntity<?> closeTicket(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            warrantyService.closeTicket(id, body);
            return ResponseEntity.ok(new ApiMessageResponse(true, "Phiếu bảo hành đã được hoàn tất và đóng."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(false, e.getMessage()));
        }
    }
}
