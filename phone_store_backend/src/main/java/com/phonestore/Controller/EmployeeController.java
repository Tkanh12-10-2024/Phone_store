package com.phonestore.Controller;

import com.phonestore.dao.EmployeeAccountDAO;
import com.phonestore.dto.employee.EmployeeAccountRequest;
import com.phonestore.dto.employee.EmployeeAccountResponse;
import com.phonestore.dto.employee.EmployeeValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeAccountDAO employeeAccountDAO;

    public EmployeeController(EmployeeAccountDAO employeeAccountDAO) {
        this.employeeAccountDAO = employeeAccountDAO;
    }

    @GetMapping
    public List<EmployeeAccountResponse> layDanhSach() {
        return employeeAccountDAO.layDanhSach();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> layChiTiet(@PathVariable String id) {
        try {
            return ResponseEntity.ok(employeeAccountDAO.timTheoMa(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(taoLoi(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> taoMoi(@RequestBody EmployeeAccountRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(employeeAccountDAO.taoMoi(request));
        } catch (EmployeeValidationException e) {
            return ResponseEntity.badRequest().body(taoLoi(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.warn("Không thể tạo tài khoản nhân viên: dữ liệu bị trùng hoặc vi phạm ràng buộc.", e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(taoLoi("Mã nhân viên, số điện thoại hoặc tên đăng nhập đã tồn tại."));
        } catch (Exception e) {
            log.error("Lỗi khi tạo tài khoản nhân viên.", e);
            return ResponseEntity.internalServerError()
                    .body(taoLoi("Không thể tạo tài khoản nhân viên."));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> capNhat(
            @PathVariable String id,
            @RequestBody EmployeeAccountRequest request
    ) {
        try {
            return ResponseEntity.ok(employeeAccountDAO.capNhat(id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(taoLoi(e.getMessage()));
        } catch (EmployeeValidationException e) {
            return ResponseEntity.badRequest().body(taoLoi(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.warn("Không thể cập nhật tài khoản nhân viên {}: dữ liệu bị trùng hoặc vi phạm ràng buộc.", id, e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(taoLoi("Số điện thoại hoặc tên đăng nhập đã tồn tại."));
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật tài khoản nhân viên {}.", id, e);
            return ResponseEntity.internalServerError()
                    .body(taoLoi("Không thể cập nhật tài khoản nhân viên."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> xoa(@PathVariable String id) {
        try {
            employeeAccountDAO.xoa(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã khóa tài khoản và chuyển nhân viên sang trạng thái nghỉ việc.");
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(taoLoi(e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi khóa tài khoản nhân viên {}.", id, e);
            return ResponseEntity.internalServerError()
                    .body(taoLoi("Không thể khóa tài khoản nhân viên."));
        }
    }

    private Map<String, Object> taoLoi(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        return body;
    }
}
