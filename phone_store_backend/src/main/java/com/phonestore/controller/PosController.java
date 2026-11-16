package com.phonestore.controller;

import com.phonestore.dto.request.ApplyVoucherRequest;
import com.phonestore.dto.request.PosCheckoutRequest;
import com.phonestore.dto.response.ApplyVoucherResponse;
import com.phonestore.dto.response.PosCheckoutResponse;
import com.phonestore.dto.response.PosCustomerDTO;
import com.phonestore.dto.response.PosProductDTO;
import com.phonestore.service.PosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class PosController {

    private final PosService posService;

    @GetMapping("/products")
    public ResponseEntity<List<PosProductDTO>> listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer maThuongHieu
    ) {
        return ResponseEntity.ok(posService.listPosProducts(keyword, maThuongHieu));
    }

    @GetMapping("/customers")
    public ResponseEntity<List<PosCustomerDTO>> listCustomers() {
        return ResponseEntity.ok(posService.listCustomers());
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<PosCheckoutResponse>> listRecentInvoices(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(posService.listRecentInvoices(limit));
    }

    @PostMapping("/apply-voucher")
    public ResponseEntity<ApplyVoucherResponse> applyVoucher(@RequestBody ApplyVoucherRequest request) {
        return ResponseEntity.ok(posService.validateAndApplyVoucher(request));
    }

    @PostMapping("/checkout")
    public ResponseEntity<PosCheckoutResponse> checkout(@RequestBody PosCheckoutRequest request) {
        return ResponseEntity.ok(posService.checkout(request));
    }
}

