package com.phonestore.service;

import com.phonestore.dao.PromotionDao;
import com.phonestore.dto.request.PromotionRequest;
import com.phonestore.dto.response.ApiMessageResponse;
import com.phonestore.dto.response.OptionItemDTO;
import com.phonestore.dto.response.PromotionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private static final Pattern VOUCHER_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,50}$");
    private static final Set<String> VALID_TYPES = Set.of("Phan_tram", "Tien_mat", "Qua_tang");
    private static final Set<String> VALID_STATUSES = Set.of("Dang_dien_ra", "Tam_dung", "Het_han");
    private static final Set<String> VALID_TIERS = Set.of("Tat_ca", "Khach_moi", "Thanh_vien", "Khach_VIP");

    private final PromotionDao promotionDao;

    public List<PromotionResponse> listPromotions(String keyword, String trangThai, String loaiGiamGia) {
        return promotionDao.findAll(keyword, trangThai, loaiGiamGia);
    }

    public List<PromotionResponse> listActiveAlerts() {
        return promotionDao.findAll(null, "Dang_dien_ra", null).stream()
                .filter(PromotionResponse::isCoCanhBao)
                .toList();
    }

    public PromotionResponse getPromotionById(String maKhuyenMai) {
        return promotionDao.findById(maKhuyenMai)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi: " + maKhuyenMai));
    }

    public PromotionResponse createPromotion(PromotionRequest req) {
        validatePromotionRequest(req, null);
        String maKm = (req.getMaKhuyenMai() == null || req.getMaKhuyenMai().isBlank())
                ? "KM_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now())
                : req.getMaKhuyenMai().trim().toUpperCase();
        return promotionDao.insert(req, maKm);
    }

    public PromotionResponse updatePromotion(String maKhuyenMai, PromotionRequest req) {
        getPromotionById(maKhuyenMai);
        validatePromotionRequest(req, maKhuyenMai);
        return promotionDao.update(maKhuyenMai, req);
    }

    public PromotionResponse updateStatus(String maKhuyenMai, String trangThai) {
        if (trangThai == null || !VALID_STATUSES.contains(trangThai.trim())) {
            throw new IllegalArgumentException("Trạng thái khuyến mãi không hợp lệ");
        }
        boolean updated = promotionDao.updateStatus(maKhuyenMai, trangThai.trim());
        if (!updated) {
            throw new IllegalArgumentException("Không tìm thấy khuyến mãi: " + maKhuyenMai);
        }
        return getPromotionById(maKhuyenMai);
    }

    /**
     * UC 2.14.1.3: Dừng / xóa khuyến mãi.
     * Nếu đã phát sinh giao dịch thì chuyển sang Ngừng áp dụng (Het_han) để giữ toàn vẹn dữ liệu hóa đơn.
     */
    public ApiMessageResponse deleteOrStopPromotion(String maKhuyenMai) {
        PromotionResponse existing = getPromotionById(maKhuyenMai);
        if (promotionDao.isUsedInOrders(maKhuyenMai)) {
            promotionDao.updateStatus(maKhuyenMai, "Het_han");
            return new ApiMessageResponse(true,
                    "Khuyến mãi " + existing.getCodeVoucher() + " đã phát sinh giao dịch nên hệ thống đã dừng áp dụng (Hết hạn) để bảo toàn lịch sử.");
        }
        promotionDao.deleteById(maKhuyenMai);
        return new ApiMessageResponse(true, "Đã xóa vĩnh viễn chương trình khuyến mãi " + existing.getCodeVoucher());
    }

    public Map<String, Object> getMetadataOptions() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<OptionItemDTO> brands = promotionDao.findBrandOptions();
        List<OptionItemDTO> categories = promotionDao.findCategoryOptions();
        List<String> lines = promotionDao.findProductLineOptions();
        result.put("brands", brands);
        result.put("categories", categories);
        result.put("productLines", lines);
        return result;
    }

    private void validatePromotionRequest(PromotionRequest req, String excludeMaKhuyenMai) {
        if (req == null) {
            throw new IllegalArgumentException("Dữ liệu khuyến mãi không được để trống");
        }
        if (req.getCodeVoucher() == null || !VOUCHER_CODE_PATTERN.matcher(req.getCodeVoucher().trim()).matches()) {
            throw new IllegalArgumentException("Mã voucher phải gồm 3-50 ký tự chữ, số hoặc dấu gạch dưới/ngang, không chứa khoảng trắng");
        }
        if (promotionDao.existsByCodeVoucher(req.getCodeVoucher().trim(), excludeMaKhuyenMai)) {
            throw new IllegalArgumentException("Mã voucher '" + req.getCodeVoucher().trim().toUpperCase() + "' đã tồn tại trên hệ thống");
        }
        if (req.getTenKhuyenMai() == null || req.getTenKhuyenMai().isBlank()) {
            throw new IllegalArgumentException("Tên chương trình khuyến mãi không được để trống");
        }
        String type = req.getLoaiGiamGia() == null ? "" : req.getLoaiGiamGia().trim();
        if (!VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException("Loại giảm giá phải là Phan_tram, Tien_mat hoặc Qua_tang");
        }
        BigDecimal value = req.getGiaTriGiam() == null ? BigDecimal.ZERO : req.getGiaTriGiam();
        if ("Phan_tram".equals(type)) {
            if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Tỷ lệ giảm phần trăm phải lớn hơn 0% và tối đa 100%");
            }
        } else if ("Tien_mat".equals(type)) {
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Số tiền giảm cố định phải lớn hơn 0đ");
            }
        } else {
            if (req.getQuaTang() == null || req.getQuaTang().isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập mô tả quà tặng / combo kèm theo");
            }
        }

        if (req.getGiaTriDonHangToiThieu() != null && req.getGiaTriDonHangToiThieu().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá trị đơn hàng tối thiểu không được âm");
        }
        if (req.getGiamToiDa() != null && req.getGiamToiDa().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Mức giảm tối đa không được âm");
        }
        if (req.getSoLuongPhatHanh() == null || req.getSoLuongPhatHanh() <= 0) {
            throw new IllegalArgumentException("Số lượng voucher phát hành phải lớn hơn 0");
        }
        if (req.getNgayBatDau() == null || req.getNgayKetThuc() == null) {
            throw new IllegalArgumentException("Vui lòng chọn đầy đủ ngày bắt đầu và ngày kết thúc");
        }
        if (req.getNgayKetThuc().isBefore(req.getNgayBatDau())) {
            throw new IllegalArgumentException("Thời gian kết thúc không được nhỏ hơn thời gian bắt đầu");
        }
        if (req.getDoiTuongKhachHang() != null && !req.getDoiTuongKhachHang().isBlank()
                && !VALID_TIERS.contains(req.getDoiTuongKhachHang().trim())) {
            throw new IllegalArgumentException("Đối tượng khách hàng áp dụng không hợp lệ");
        }
        if (req.getTrangThai() != null && !req.getTrangThai().isBlank()
                && !VALID_STATUSES.contains(req.getTrangThai().trim())) {
            throw new IllegalArgumentException("Trạng thái khuyến mãi không hợp lệ");
        }
    }
}

