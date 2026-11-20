package com.phonestore.controller;

import com.phonestore.dto.request.CustomerCreateRequest;
import com.phonestore.dto.request.CustomerUpdateRequest;
import com.phonestore.dto.response.ApiMessageResponse;
import com.phonestore.dto.response.CustomerDTO;
import com.phonestore.dto.response.PurchaseHistoryDTO;
import com.phonestore.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CustomerController {

    private final CustomerService customerService;

    // GET /api/customers?keyword=...
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAll(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(customerService.search(keyword));
    }

    // GET /api/customers/{id}
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(customerService.getById(id));
    }

    // POST /api/customers
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CustomerCreateRequest req) {
        try {
            CustomerDTO dto = customerService.create(req);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(false, e.getMessage()));
        }
    }

    // PUT /api/customers/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody CustomerUpdateRequest req) {
        try {
            CustomerDTO dto = customerService.update(id, req);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(false, e.getMessage()));
        }
    }

    // DELETE /api/customers/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            String result = customerService.deactivateOrDelete(id);
            String msg = "DEACTIVATED".equals(result)
                    ? "Khách hàng đã được vô hiệu hóa (có lịch sử giao dịch)."
                    : "Khách hàng đã được xóa thành công.";
            return ResponseEntity.ok(Map.of("result", result, "message", msg));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(false, e.getMessage()));
        }
    }

    // GET /api/customers/{id}/history
    @GetMapping("/{id}/history")
    public ResponseEntity<List<PurchaseHistoryDTO>> getPurchaseHistory(@PathVariable String id) {
        return ResponseEntity.ok(customerService.getPurchaseHistory(id));
    }
}
