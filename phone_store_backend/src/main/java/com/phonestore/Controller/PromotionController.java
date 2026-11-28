package com.phonestore.controller;

import com.phonestore.dao.KhuyenMaiDAO;
import com.phonestore.model.KhuyenMai;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private static final Set<String> DISCOUNT_TYPES =
            Set.of("Phan_tram", "Tien_mat", "Qua_tang");
    private static final Set<String> STATUSES =
            Set.of("Dang_dien_ra", "Tam_dung", "Het_han");
    private static final Set<String> CUSTOMER_TIERS =
            Set.of("Tat_ca", "Khach_moi", "Thanh_vien", "Khach_VIP");

    private final KhuyenMaiDAO khuyenMaiDAO;
    private final JdbcTemplate jdbcTemplate;

    public PromotionController(KhuyenMaiDAO khuyenMaiDAO, JdbcTemplate jdbcTemplate) {
        this.khuyenMaiDAO = khuyenMaiDAO;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<?> layDanhSach(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String loaiGiamGia
    ) {
        try {
            List<KhuyenMai> promotions = enrich(khuyenMaiDAO.layDanhSach());
            String query = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
            return ResponseEntity.ok(promotions.stream()
                    .filter(p -> query.isEmpty()
                            || contains(p.getCodeVoucher(), query)
                            || contains(p.getTenKhuyenMai(), query)
                            || contains(p.getMoTa(), query))
                    .filter(p -> trangThai == null || trangThai.isBlank()
                            || trangThai.equals("ALL") || trangThai.equals(p.getTrangThai()))
                    .filter(p -> loaiGiamGia == null || loaiGiamGia.isBlank()
                            || loaiGiamGia.equals("ALL") || loaiGiamGia.equals(p.getLoaiGiamGia()))
                    .toList());
        } catch (Exception e) {
            return serverError("Không thể tải danh sách khuyến mãi.");
        }
    }

    @GetMapping("/options")
    public ResponseEntity<?> layTuyChon() {
        try {
            List<Map<String, Object>> brands = jdbcTemplate.query("""
                    SELECT ma_thuong_hieu, ten_thuong_hieu
                    FROM thuong_hieu
                    ORDER BY ten_thuong_hieu
                    """, (rs, rowNum) -> option(rs.getObject(1), rs.getString(2)));
            List<Map<String, Object>> categories = jdbcTemplate.query("""
                    SELECT ma_danh_muc, ten_danh_muc
                    FROM danh_muc
                    ORDER BY ten_danh_muc
                    """, (rs, rowNum) -> option(rs.getObject(1), rs.getString(2)));
            List<String> productLines = jdbcTemplate.queryForList("""
                    SELECT DISTINCT dong_may
                    FROM san_pham
                    WHERE dong_may IS NOT NULL AND TRIM(dong_may) <> ''
                    ORDER BY dong_may
                    """, String.class);
            Map<String, Object> options = new LinkedHashMap<>();
            options.put("brands", brands);
            options.put("categories", categories);
            options.put("productLines", productLines);
            return ResponseEntity.ok(options);
        } catch (Exception e) {
            return serverError("Không thể tải tùy chọn khuyến mãi.");
        }
    }

    @GetMapping("/alerts")
    public ResponseEntity<?> layCanhBao() {
        try {
            return ResponseEntity.ok(enrich(khuyenMaiDAO.layDanhSach()).stream()
                    .filter(KhuyenMai::isCoCanhBao)
                    .toList());
        } catch (Exception e) {
            return serverError("Không thể tải cảnh báo khuyến mãi.");
        }
    }

    @GetMapping("/{maKhuyenMai}")
    public ResponseEntity<?> layChiTiet(@PathVariable String maKhuyenMai) {
        Optional<KhuyenMai> promotion = khuyenMaiDAO.timTheoMa(maKhuyenMai);
        if (promotion.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Không tìm thấy khuyến mãi.");
        }
        return ResponseEntity.ok(enrich(List.of(promotion.get())).get(0));
    }

    @PostMapping
    public ResponseEntity<?> tao(@RequestBody KhuyenMai promotion) {
        String invalid = validate(promotion, null);
        if (invalid != null) {
            return error(HttpStatus.BAD_REQUEST, invalid);
        }
        promotion.setCodeVoucher(promotion.getCodeVoucher().trim().toUpperCase(Locale.ROOT));
        if (khuyenMaiDAO.daTonTaiCodeVoucher(promotion.getCodeVoucher(), null)) {
            return error(HttpStatus.BAD_REQUEST, "Mã voucher đã tồn tại.");
        }

        try {
            if (!khuyenMaiDAO.taoKhuyenMai(promotion)) {
                return error(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo khuyến mãi.");
            }
            KhuyenMai created = khuyenMaiDAO.timTheoCodeVoucher(promotion.getCodeVoucher()).orElseThrow();
            return ResponseEntity.status(HttpStatus.CREATED).body(enrich(List.of(created)).get(0));
        } catch (DataIntegrityViolationException e) {
            return error(HttpStatus.BAD_REQUEST, "Thương hiệu hoặc danh mục áp dụng không hợp lệ.");
        } catch (Exception e) {
            return serverError("Không thể tạo khuyến mãi.");
        }
    }

    @PutMapping("/{maKhuyenMai}")
    public ResponseEntity<?> capNhat(
            @PathVariable String maKhuyenMai,
            @RequestBody KhuyenMai promotion
    ) {
        Optional<KhuyenMai> existing = khuyenMaiDAO.timTheoMa(maKhuyenMai);
        if (existing.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Không tìm thấy khuyến mãi.");
        }
        String invalid = validate(promotion, existing.get());
        if (invalid != null) {
            return error(HttpStatus.BAD_REQUEST, invalid);
        }
        promotion.setCodeVoucher(promotion.getCodeVoucher().trim().toUpperCase(Locale.ROOT));
        if (khuyenMaiDAO.daTonTaiCodeVoucher(promotion.getCodeVoucher(), maKhuyenMai)) {
            return error(HttpStatus.BAD_REQUEST, "Mã voucher đã tồn tại.");
        }

        try {
            if (!khuyenMaiDAO.capNhatKhuyenMai(maKhuyenMai, promotion)) {
                return error(HttpStatus.NOT_FOUND, "Không tìm thấy khuyến mãi.");
            }
            KhuyenMai updated = khuyenMaiDAO.timTheoMa(maKhuyenMai).orElseThrow();
            return ResponseEntity.ok(enrich(List.of(updated)).get(0));
        } catch (DataIntegrityViolationException e) {
            return error(HttpStatus.BAD_REQUEST, "Thương hiệu hoặc danh mục áp dụng không hợp lệ.");
        } catch (Exception e) {
            return serverError("Không thể cập nhật khuyến mãi.");
        }
    }

    @PutMapping("/{maKhuyenMai}/status")
    public ResponseEntity<?> capNhatTrangThai(
            @PathVariable String maKhuyenMai,
            @RequestBody Map<String, String> request
    ) {
        String status = request == null ? null : request.get("trangThai");
        if (status == null || !STATUSES.contains(status)) {
            return error(HttpStatus.BAD_REQUEST, "Trạng thái khuyến mãi không hợp lệ.");
        }
        try {
            if (!khuyenMaiDAO.capNhatTrangThai(maKhuyenMai, status)) {
                return error(HttpStatus.NOT_FOUND, "Không tìm thấy khuyến mãi.");
            }
            return ResponseEntity.ok(khuyenMaiDAO.timTheoMa(maKhuyenMai)
                    .map(p -> enrich(List.of(p)).get(0)).orElseThrow());
        } catch (Exception e) {
            return serverError("Không thể cập nhật trạng thái khuyến mãi.");
        }
    }

    @DeleteMapping("/{maKhuyenMai}")
    public ResponseEntity<?> xoa(@PathVariable String maKhuyenMai) {
        Optional<KhuyenMai> existing = khuyenMaiDAO.timTheoMa(maKhuyenMai);
        if (existing.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Không tìm thấy khuyến mãi.");
        }
        try {
            if (khuyenMaiDAO.demDonHangDaSuDung(maKhuyenMai) > 0) {
                khuyenMaiDAO.capNhatTrangThai(maKhuyenMai, "Het_han");
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Khuyến mãi đã phát sinh đơn hàng nên được chuyển sang trạng thái Hết hạn để bảo toàn lịch sử."
                ));
            }
            khuyenMaiDAO.xoaKhuyenMai(maKhuyenMai);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã xóa khuyến mãi."
            ));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Khuyến mãi đã phát sinh giao dịch nên được chuyển sang trạng thái Hết hạn để bảo toàn lịch sử."
            ));
        } catch (Exception e) {
            return serverError("Không thể xóa khuyến mãi.");
        }
    }

    private String validate(KhuyenMai p, KhuyenMai existing) {
        if (p == null) {
            return "Dữ liệu khuyến mãi không hợp lệ.";
        }
        String code = p.getCodeVoucher() == null ? "" : p.getCodeVoucher().trim();
        if (!code.matches("[A-Za-z0-9_-]{3,50}")) {
            return "Mã voucher phải gồm 3-50 ký tự chữ, số hoặc dấu gạch dưới/gạch ngang.";
        }
        if (p.getTenKhuyenMai() == null || p.getTenKhuyenMai().isBlank()
                || p.getTenKhuyenMai().length() > 150) {
            return "Tên chương trình khuyến mãi không được để trống và tối đa 150 ký tự.";
        }
        if (!DISCOUNT_TYPES.contains(p.getLoaiGiamGia())) {
            return "Loại giảm giá không hợp lệ.";
        }
        BigDecimal value = p.getGiaTriGiam();
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0
                || ("Phan_tram".equals(p.getLoaiGiamGia())
                && value.compareTo(BigDecimal.valueOf(100)) > 0)) {
            return "Giá trị giảm giá không hợp lệ.";
        }
        if ("Qua_tang".equals(p.getLoaiGiamGia())
                && (p.getQuaTang() == null || p.getQuaTang().isBlank())) {
            return "Vui lòng nhập mô tả quà tặng hoặc combo.";
        }
        if (p.getGiaTriDonHangToiThieu() == null
                || p.getGiaTriDonHangToiThieu().compareTo(BigDecimal.ZERO) < 0
                || (p.getGiamToiDa() != null
                && p.getGiamToiDa().compareTo(BigDecimal.ZERO) < 0)) {
            return "Giá trị đơn tối thiểu hoặc mức giảm tối đa không hợp lệ.";
        }
        if (p.getSoLuongPhatHanh() <= 0
                || (existing != null && p.getSoLuongPhatHanh() < existing.getSoLuongDaDung())) {
            return "Số lượng phát hành phải lớn hơn 0 và không được thấp hơn số lượt đã sử dụng.";
        }
        if (p.getNgayBatDau() == null || p.getNgayKetThuc() == null
                || p.getNgayKetThuc().isBefore(p.getNgayBatDau())) {
            return "Ngày bắt đầu/kết thúc không hợp lệ.";
        }
        if (p.getTrangThai() == null) {
            p.setTrangThai(existing == null ? "Dang_dien_ra" : existing.getTrangThai());
        }
        if (!STATUSES.contains(p.getTrangThai())) {
            return "Trạng thái khuyến mãi không hợp lệ.";
        }
        if (p.getDoiTuongKhachHang() == null) {
            p.setDoiTuongKhachHang("Tat_ca");
        }
        if (!CUSTOMER_TIERS.contains(p.getDoiTuongKhachHang())) {
            return "Đối tượng khách hàng không hợp lệ.";
        }
        if (existing != null) {
            p.setSoLuongDaDung(existing.getSoLuongDaDung());
        } else {
            p.setSoLuongDaDung(0);
        }
        return null;
    }

    private List<KhuyenMai> enrich(List<KhuyenMai> promotions) {
        Map<Integer, String> brands = jdbcTemplate.queryForList("""
                SELECT ma_thuong_hieu, ten_thuong_hieu
                FROM thuong_hieu
                """).stream().collect(Collectors.toMap(
                row -> ((Number) row.get("ma_thuong_hieu")).intValue(),
                row -> String.valueOf(row.get("ten_thuong_hieu")),
                (left, right) -> left,
                HashMap::new
        ));
        Map<Integer, String> categories = jdbcTemplate.queryForList("""
                SELECT ma_danh_muc, ten_danh_muc
                FROM danh_muc
                """).stream().collect(Collectors.toMap(
                row -> ((Number) row.get("ma_danh_muc")).intValue(),
                row -> String.valueOf(row.get("ten_danh_muc")),
                (left, right) -> left,
                HashMap::new
        ));
        LocalDateTime now = LocalDateTime.now();
        List<KhuyenMai> result = new ArrayList<>(promotions.size());
        for (KhuyenMai p : promotions) {
            p.setTenThuongHieu(brands.get(p.getMaThuongHieuApDung()));
            p.setTenDanhMuc(categories.get(p.getMaDanhMucApDung()));
            int remaining = Math.max(0, p.getSoLuongPhatHanh() - p.getSoLuongDaDung());
            int percentage = p.getSoLuongPhatHanh() > 0
                    ? (int) Math.floor((remaining * 100.0) / p.getSoLuongPhatHanh())
                    : 100;
            p.setSoLuongConLai(remaining);
            p.setTiLeConLai(percentage);

            boolean active = "Dang_dien_ra".equals(p.getTrangThai());
            boolean lowQuantity = active && p.getSoLuongPhatHanh() > 0
                    && percentage <= 5;
            boolean nearExpiry = active && p.getNgayKetThuc() != null
                    && !p.getNgayKetThuc().isBefore(now)
                    && Duration.between(now, p.getNgayKetThuc()).toHours() < 24;
            p.setSapHetSoLuong(lowQuantity);
            p.setSapHetHan(nearExpiry);
            p.setCoCanhBao(lowQuantity || nearExpiry);
            List<String> reasons = new ArrayList<>();
            if (nearExpiry) {
                reasons.add("Sắp hết hạn trong 24 giờ");
            }
            if (lowQuantity) {
                reasons.add("Sắp hết số lượng (còn " + percentage + "%)");
            }
            p.setLyDoCanhBao(String.join(" • ", reasons));
            result.add(p);
        }
        return result;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private Map<String, Object> option(Object value, String label) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("value", value);
        option.put("label", label);
        return option;
    }

    private ResponseEntity<Map<String, Object>> serverError(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("success", false, "message", message));
    }
}
