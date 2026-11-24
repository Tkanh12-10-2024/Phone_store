package com.phonestore.controller;

import com.phonestore.service.ImeiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/imeis")
@CrossOrigin(origins = "*")
public class ImeiController {

    @Autowired
    private ImeiService imeiService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getImeis(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai) {
        return ResponseEntity.ok(imeiService.listImeis(keyword, trangThai));
    }

    @GetMapping("/{maImei}")
    public ResponseEntity<?> getImeiDetail(@PathVariable String maImei) {
        try {
            return ResponseEntity.ok(imeiService.getImeiDetail(maImei));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> addImeis(@RequestBody Map<String, Object> payload) {
        try {
            String maSanPham = payload.get("maSanPham") != null ? String.valueOf(payload.get("maSanPham")) : null;
            String maPhieuNhap = payload.get("maPhieuNhap") != null ? String.valueOf(payload.get("maPhieuNhap")) : null;
            Object imeisObj = payload.get("imeis");
            List<String> imeis = new java.util.ArrayList<>();
            if (imeisObj instanceof List<?>) {
                for (Object o : (List<?>) imeisObj) {
                    if (o != null) imeis.add(String.valueOf(o));
                }
            } else if (imeisObj instanceof String) {
                String s = (String) imeisObj;
                for (String line : s.split("\\r?\\n|,|;|\\s+")) {
                    String t = line.trim();
                    if (!t.isEmpty()) imeis.add(t);
                }
            }
            return ResponseEntity.ok(imeiService.addImeis(maSanPham, maPhieuNhap, imeis));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
