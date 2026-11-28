
package com.phonestore.controller;

import com.phonestore.bus.BanHangPosBUS;
import com.phonestore.dao.KhuyenMaiDAO;
import com.phonestore.dto.pos.PosCheckoutRequest;
import com.phonestore.dto.pos.PosCheckoutResponse;
import com.phonestore.dto.pos.PosVoucherRequest;
import com.phonestore.dto.pos.PosVoucherResponse;
import com.phonestore.model.HoaDon;
import com.phonestore.model.KhuyenMai;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pos")
public class PosController {

    private final BanHangPosBUS banHangPosBUS;
    private final KhuyenMaiDAO khuyenMaiDAO;
    private final JdbcTemplate jdbcTemplate;

    public PosController(
            BanHangPosBUS banHangPosBUS,
            KhuyenMaiDAO khuyenMaiDAO,
            JdbcTemplate jdbcTemplate
    ) {
        this.banHangPosBUS = banHangPosBUS;
        this.khuyenMaiDAO = khuyenMaiDAO;
        this.jdbcTemplate = jdbcTemplate;
    }

    // =====================================================
    // 1. DANH SÁCH SẢN PHẨM
    // GET /api/pos/products
    // =====================================================

    @GetMapping("/products")
    public ResponseEntity<?> layDanhSachSanPham() {
        try {
            return ResponseEntity.ok(
                    banHangPosBUS.layDanhSachSanPham()
            );
        } catch (Exception e) {
            return loiServer("Không thể tải danh sách sản phẩm.", e);
        }
    }

    // =====================================================
    // 2. DANH SÁCH KHÁCH HÀNG
    // GET /api/pos/customers
    // =====================================================

    @GetMapping("/customers")
    public ResponseEntity<?> layDanhSachKhachHang() {
        try {
            return ResponseEntity.ok(
                    banHangPosBUS.layDanhSachKhachHang()
            );
        } catch (Exception e) {
            return loiServer("Không thể tải danh sách khách hàng.", e);
        }
    }

    // =====================================================
    // 3. DANH SÁCH THƯƠNG HIỆU
    // GET /api/pos/brands
    // =====================================================

    @GetMapping("/brands")
    public ResponseEntity<?> layDanhSachThuongHieu() {
        try {
            String sql = """
                    SELECT
                        ma_thuong_hieu AS maThuongHieu,
                        ten_thuong_hieu AS tenThuongHieu
                    FROM thuong_hieu
                    ORDER BY ten_thuong_hieu ASC
                    """;

            List<Map<String, Object>> brands =
                    jdbcTemplate.queryForList(sql);

            return ResponseEntity.ok(brands);

        } catch (Exception e) {
            return loiServer("Không thể tải danh sách thương hiệu.", e);
        }
    }

    // =====================================================
    // 4. DANH SÁCH VOUCHER
    // GET /api/pos/vouchers
    // =====================================================

    @GetMapping("/vouchers")
    public ResponseEntity<?> layDanhSachVoucher() {
        try {
            List<KhuyenMai> vouchers = khuyenMaiDAO.layDanhSach();

            return ResponseEntity.ok(vouchers);

        } catch (Exception e) {
            return loiServer("Không thể tải danh sách voucher.", e);
        }
    }

    // =====================================================
    // 5. KIỂM TRA VÀ ÁP DỤNG VOUCHER
    // POST /api/pos/apply-voucher
    // =====================================================

    @PostMapping("/apply-voucher")
    public ResponseEntity<?> apDungVoucher(
            @RequestBody PosVoucherRequest request
    ) {
        if (request == null) {
            return ResponseEntity.badRequest().body(
                    taoLoi("Dữ liệu kiểm tra voucher không hợp lệ.")
            );
        }

        try {
            PosVoucherResponse response =
                    banHangPosBUS.kiemTraVaApDungVoucher(request);

            /*
             * Voucher không hợp lệ vẫn trả HTTP 200.
             * Frontend đọc trường valid và errorCode để hiển thị.
             */
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    taoLoi(e.getMessage())
            );
        } catch (Exception e) {
            return loiServer("Không thể kiểm tra voucher.", e);
        }
    }

    // =====================================================
    // 6. THANH TOÁN POS
    // POST /api/pos/checkout
    // =====================================================

    @PostMapping("/checkout")
    public ResponseEntity<?> thanhToan(
            @RequestBody PosCheckoutRequest request
    ) {
        if (request == null) {
            return ResponseEntity.badRequest().body(
                    taoLoi("Dữ liệu thanh toán không hợp lệ.")
            );
        }

        try {
            PosCheckoutResponse response =
                    banHangPosBUS.thanhToan(request);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    taoLoi(e.getMessage())
            );
        } catch (Exception e) {
            return loiServer("Thanh toán thất bại.", e);
        }
    }

    // =====================================================
    // 7. DANH SÁCH HÓA ĐƠN GẦN NHẤT
    // GET /api/pos/invoices?limit=10
    // =====================================================

    @GetMapping("/invoices")
    public ResponseEntity<?> layDanhSachHoaDon(
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            int soLuong = Math.max(1, Math.min(limit, 100));

            List<HoaDon> invoices =
                    banHangPosBUS.layHoaDonGanNhat(soLuong);

            return ResponseEntity.ok(invoices);

        } catch (Exception e) {
            return loiServer("Không thể tải danh sách hóa đơn.", e);
        }
    }

    // =====================================================
    // 8. HÀM TẠO RESPONSE LỖI
    // =====================================================

    private Map<String, Object> taoLoi(String message) {
        Map<String, Object> body = new HashMap<>();

        body.put("success", false);
        body.put("message", message);

        return body;
    }

    private ResponseEntity<Map<String, Object>> loiServer(
            String message,
            Exception exception
    ) {
        /*
         * Không trả stack trace hoặc thông tin database
         * về phía frontend.
         */
        Map<String, Object> body = taoLoi(message);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }
}
