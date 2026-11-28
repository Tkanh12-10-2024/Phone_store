package com.phonestore.Controller;

import com.phonestore.dao.CustomerManagementDAO;
import com.phonestore.dto.customer.CustomerRequest;
import com.phonestore.dto.customer.CustomerResponse;
import com.phonestore.dto.customer.CustomerValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerManagementDAO customerManagementDAO;

    public CustomerController(CustomerManagementDAO customerManagementDAO) {
        this.customerManagementDAO = customerManagementDAO;
    }

    @GetMapping
    public ResponseEntity<?> layDanhSach(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai
    ) {
        try {
            return ResponseEntity.ok(customerManagementDAO.layDanhSach(keyword, trangThai));
        } catch (Exception e) {
            log.error("Không thể tải danh sách khách hàng.", e);
            return ResponseEntity.internalServerError().body(error("Không thể tải danh sách khách hàng."));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> layChiTiet(@PathVariable String id) {
        try {
            return ResponseEntity.ok(customerManagementDAO.timTheoMa(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Không thể tải thông tin khách hàng {}.", id, e);
            return ResponseEntity.internalServerError().body(error("Không thể tải thông tin khách hàng."));
        }
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> layLichSu(@PathVariable String id) {
        try {
            return ResponseEntity.ok(customerManagementDAO.layLichSuMuaHang(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Không thể tải lịch sử mua hàng khách hàng {}.", id, e);
            return ResponseEntity.internalServerError().body(error("Không thể tải lịch sử mua hàng."));
        }
    }

    @PostMapping
    public ResponseEntity<?> taoMoi(@RequestBody CustomerRequest request) {
        try {
            CustomerResponse customer = customerManagementDAO.taoMoi(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(customer);
        } catch (CustomerValidationException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.warn("Không thể tạo khách hàng do trùng mã hoặc số điện thoại.", e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(error("Mã khách hàng hoặc số điện thoại đã tồn tại."));
        } catch (Exception e) {
            log.error("Không thể tạo khách hàng.", e);
            return ResponseEntity.internalServerError().body(error("Không thể tạo khách hàng."));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> capNhat(
            @PathVariable String id,
            @RequestBody CustomerRequest request
    ) {
        try {
            return ResponseEntity.ok(customerManagementDAO.capNhat(id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (CustomerValidationException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.warn("Không thể cập nhật khách hàng {} do trùng số điện thoại.", id, e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(error("Số điện thoại đã được đăng ký cho khách hàng khác."));
        } catch (Exception e) {
            log.error("Không thể cập nhật khách hàng {}.", id, e);
            return ResponseEntity.internalServerError().body(error("Không thể cập nhật khách hàng."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> xoa(@PathVariable String id) {
        try {
            CustomerManagementDAO.DeleteCustomerResult result = customerManagementDAO.xoa(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("action", result.action());
            response.put("message", result.message());
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.warn("Không thể xóa khách hàng {} do đang được dữ liệu khác tham chiếu.", id, e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(error("Khách hàng đang được dữ liệu khác sử dụng."));
        } catch (Exception e) {
            log.error("Không thể xóa khách hàng {}.", id, e);
            return ResponseEntity.internalServerError().body(error("Không thể xóa khách hàng."));
        }
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        return body;
    }
}
