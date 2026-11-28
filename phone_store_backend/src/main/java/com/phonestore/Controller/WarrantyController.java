package com.phonestore.Controller;

import com.phonestore.dao.WarrantyManagementDAO;
import com.phonestore.dto.warranty.WarrantyCheckRequest;
import com.phonestore.dto.warranty.WarrantyCloseRequest;
import com.phonestore.dto.warranty.WarrantyProgressRequest;
import com.phonestore.dto.warranty.WarrantyReceiveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/warranty")
public class WarrantyController {

    private static final Logger log = LoggerFactory.getLogger(WarrantyController.class);

    private final WarrantyManagementDAO warrantyManagementDAO;

    public WarrantyController(WarrantyManagementDAO warrantyManagementDAO) {
        this.warrantyManagementDAO = warrantyManagementDAO;
    }

    @GetMapping
    public ResponseEntity<?> layDanhSach(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai
    ) {
        try {
            return ResponseEntity.ok(warrantyManagementDAO.layDanhSach(keyword, trangThai));
        } catch (Exception e) {
            log.error("Không thể tải danh sách phiếu bảo hành.", e);
            return ResponseEntity.internalServerError().body(error("Không thể tải danh sách phiếu bảo hành."));
        }
    }

    @PostMapping("/check")
    public ResponseEntity<?> kiemTraImei(@RequestBody(required = false) WarrantyCheckRequest request) {
        try {
            return ResponseEntity.ok(warrantyManagementDAO.kiemTraImei(request == null ? null : request.imei()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Không thể kiểm tra IMEI bảo hành.", e);
            return ResponseEntity.internalServerError().body(error("Không thể kiểm tra IMEI."));
        }
    }

    @PostMapping
    public ResponseEntity<?> tiepNhan(@RequestBody(required = false) WarrantyReceiveRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(warrantyManagementDAO.tiepNhan(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Không thể tiếp nhận phiếu bảo hành.", e);
            return ResponseEntity.internalServerError().body(error("Không thể tiếp nhận phiếu bảo hành."));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> layChiTiet(@PathVariable String id) {
        try {
            return ResponseEntity.ok(warrantyManagementDAO.timChiTiet(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Không thể tải phiếu bảo hành {}.", id, e);
            return ResponseEntity.internalServerError().body(error("Không thể tải chi tiết phiếu bảo hành."));
        }
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> layLichSu(@PathVariable String id) {
        try {
            return ResponseEntity.ok(warrantyManagementDAO.layLichSu(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Không thể tải lịch sử phiếu bảo hành {}.", id, e);
            return ResponseEntity.internalServerError().body(error("Không thể tải lịch sử phiếu bảo hành."));
        }
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<?> capNhatTienDo(
            @PathVariable String id,
            @RequestBody(required = false) WarrantyProgressRequest request
    ) {
        try {
            return ResponseEntity.ok(warrantyManagementDAO.capNhatTienDo(id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Không thể cập nhật tiến độ phiếu bảo hành {}.", id, e);
            return ResponseEntity.internalServerError().body(error("Không thể cập nhật tiến độ."));
        }
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<?> hoanTat(
            @PathVariable String id,
            @RequestBody(required = false) WarrantyCloseRequest request
    ) {
        try {
            return ResponseEntity.ok(warrantyManagementDAO.hoanTat(id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Không thể hoàn tất phiếu bảo hành {}.", id, e);
            return ResponseEntity.internalServerError().body(error("Không thể hoàn tất phiếu bảo hành."));
        }
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        return body;
    }
}
