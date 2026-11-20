package com.phonestore.service;

import com.phonestore.dao.PosDao;
import com.phonestore.dao.PromotionDao;
import com.phonestore.dto.request.ApplyVoucherRequest;
import com.phonestore.dto.request.PosCheckoutRequest;
import com.phonestore.dto.response.ApplyVoucherResponse;
import com.phonestore.dto.response.PosCheckoutResponse;
import com.phonestore.dto.response.PosCustomerDTO;
import com.phonestore.dto.response.PosProductDTO;
import com.phonestore.dto.response.PromotionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PosService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PosDao posDao;
    private final PromotionDao promotionDao;

    public List<PosProductDTO> listPosProducts(String keyword, Integer maThuongHieu) {
        return posDao.findPosProducts(keyword, maThuongHieu);
    }

    public List<PosCustomerDTO> listCustomers() {
        return posDao.findAllCustomers();
    }

    public List<PosCheckoutResponse> listRecentInvoices(int limit) {
        return posDao.findRecentInvoices(limit);
    }

    /**
     * UC 2.14.2: Kiểm tra tính hợp lệ và tính toán chiết khấu khuyến mãi bằng BigDecimal.
     */
    public ApplyVoucherResponse validateAndApplyVoucher(ApplyVoucherRequest req) {
        BigDecimal vatPercent = req.getVatPhanTram() == null ? BigDecimal.ZERO : req.getVatPhanTram().max(BigDecimal.ZERO);
        List<ResolvedCartLine> lines = resolveCartLines(req.getItems());

        BigDecimal subTotal = lines.isEmpty()
                ? (req.getTamTinh() == null ? BigDecimal.ZERO : req.getTamTinh().max(BigDecimal.ZERO))
                : lines.stream()
                    .map(ResolvedCartLine::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (subTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return invalidResponse("EMPTY_CART", "Giỏ hàng trống, không thể áp dụng mã khuyến mãi", subTotal, vatPercent);
        }

        if (req.getCodeVoucher() == null || req.getCodeVoucher().isBlank()) {
            return invalidResponse("EMPTY_CODE", "Vui lòng nhập hoặc chọn mã voucher", subTotal, vatPercent);
        }

        Optional<PromotionResponse> promoOpt = promotionDao.findByCodeVoucher(req.getCodeVoucher().trim());
        if (promoOpt.isEmpty()) {
            return invalidResponse("NOT_FOUND", "Mã khuyến mãi '" + req.getCodeVoucher().trim() + "' không tồn tại trên hệ thống", subTotal, vatPercent);
        }

        PromotionResponse promo = promoOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if ("Tam_dung".equalsIgnoreCase(promo.getTrangThai())) {
            return invalidResponse("PAUSED", "Mã khuyến mãi '" + promo.getCodeVoucher() + "' đang tạm dừng áp dụng", subTotal, vatPercent);
        }
        if ("Het_han".equalsIgnoreCase(promo.getTrangThai()) || (promo.getNgayKetThuc() != null && now.isAfter(promo.getNgayKetThuc()))) {
            return invalidResponse("EXPIRED", "Mã khuyến mãi '" + promo.getCodeVoucher() + "' đã hết hạn sử dụng", subTotal, vatPercent);
        }
        if (promo.getNgayBatDau() != null && now.isBefore(promo.getNgayBatDau())) {
            return invalidResponse("NOT_STARTED", "Mã khuyến mãi '" + promo.getCodeVoucher() + "' chưa tới thời gian áp dụng", subTotal, vatPercent);
        }
        if (promo.getSoLuongConLai() == null || promo.getSoLuongConLai() <= 0) {
            return invalidResponse("OUT_OF_QUOTA", "Mã khuyến mãi '" + promo.getCodeVoucher() + "' đã hết lượt sử dụng", subTotal, vatPercent);
        }

        // Kiểm tra hạng khách hàng (UC 2.14.2.2)
        String tierReq = promo.getDoiTuongKhachHang();
        if (tierReq != null && !"Tat_ca".equalsIgnoreCase(tierReq)) {
            Optional<PosCustomerDTO> customerOpt = posDao.findCustomerById(req.getMaKhachHang());
            if (customerOpt.isEmpty()) {
                return invalidResponse("CUSTOMER_TIER_MISMATCH",
                        "Mã '" + promo.getCodeVoucher() + "' chỉ áp dụng cho khách hàng có tài khoản (" + formatTier(tierReq) + ")",
                        subTotal, vatPercent);
            }
            String custTier = customerOpt.get().getHangKhachHang();
            if ("Khach_VIP".equalsIgnoreCase(tierReq) && !"Khach_VIP".equalsIgnoreCase(custTier)) {
                return invalidResponse("CUSTOMER_TIER_MISMATCH",
                        "Mã '" + promo.getCodeVoucher() + "' chỉ áp dụng cho Khách hàng VIP (>= 200 điểm tích lũy)",
                        subTotal, vatPercent);
            }
        }

        // Kiểm tra hãng / dòng máy / danh mục phù hợp
        boolean hasScopeFilter = promo.getMaThuongHieuApDung() != null
                || (promo.getDongMayApDung() != null && !promo.getDongMayApDung().isBlank())
                || promo.getMaDanhMucApDung() != null;

        BigDecimal eligibleSubtotal = subTotal;
        if (hasScopeFilter && !lines.isEmpty()) {
            eligibleSubtotal = lines.stream()
                    .filter(line -> matchesScope(line.product(), promo))
                    .map(ResolvedCartLine::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (eligibleSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
                String scopeName = promo.getDongMayApDung() != null && !promo.getDongMayApDung().isBlank()
                        ? promo.getDongMayApDung()
                        : (promo.getTenThuongHieu() != null ? promo.getTenThuongHieu() : promo.getTenDanhMuc());
                return invalidResponse("BRAND_MISMATCH",
                        "Mã '" + promo.getCodeVoucher() + "' chỉ áp dụng cho dòng/hãng: " + scopeName,
                        subTotal, vatPercent);
            }
        }

        BigDecimal minOrder = promo.getGiaTriDonHangToiThieu() == null ? BigDecimal.ZERO : promo.getGiaTriDonHangToiThieu();
        if (subTotal.compareTo(minOrder) < 0) {
            return invalidResponse("MIN_ORDER_NOT_MET",
                    "Đơn hàng cần tối thiểu " + formatVnd(minOrder) + " để áp dụng mã '" + promo.getCodeVoucher() + "'",
                    subTotal, vatPercent);
        }

        BigDecimal discountAmount = calculateDiscountAmount(promo, eligibleSubtotal, subTotal);
        BigDecimal afterDiscount = subTotal.subtract(discountAmount).max(BigDecimal.ZERO);
        BigDecimal vatAmount = afterDiscount.multiply(vatPercent).divide(HUNDRED, 0, RoundingMode.HALF_UP);
        BigDecimal grandTotal = afterDiscount.add(vatAmount);

        String successMessage;
        if ("Qua_tang".equalsIgnoreCase(promo.getLoaiGiamGia())) {
            successMessage = "Áp dụng thành công: " + promo.getTenKhuyenMai() + " (Quà tặng: " + promo.getQuaTang() + ")";
        } else {
            successMessage = "Áp dụng thành công mã '" + promo.getCodeVoucher() + "' (-" + formatVnd(discountAmount) + ")";
        }

        return ApplyVoucherResponse.builder()
                .valid(true)
                .errorCode(null)
                .message(successMessage)
                .maKhuyenMai(promo.getMaKhuyenMai())
                .codeVoucher(promo.getCodeVoucher())
                .tenKhuyenMai(promo.getTenKhuyenMai())
                .loaiGiamGia(promo.getLoaiGiamGia())
                .quaTang(promo.getQuaTang())
                .tamTinh(subTotal)
                .soTienGiam(discountAmount)
                .tienVat(vatAmount)
                .tongThanhToan(grandTotal)
                .build();
    }

    public PosCheckoutResponse checkout(PosCheckoutRequest req) {
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng đang trống, vui lòng chọn sản phẩm thanh toán");
        }

        BigDecimal vatPercent = req.getVatPhanTram() == null ? BigDecimal.ZERO : req.getVatPhanTram().max(BigDecimal.ZERO);
        List<ApplyVoucherRequest.CartItemLine> voucherLines = new ArrayList<>();
        BigDecimal subTotal = BigDecimal.ZERO;

        for (PosCheckoutRequest.CheckoutItem item : req.getItems()) {
            PosProductDTO prod = posDao.findProductById(item.getMaSanPham())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + item.getMaSanPham()));
            int qty = item.getSoLuong() != null && item.getSoLuong() > 0 ? item.getSoLuong() : 1;
            BigDecimal unitPrice = item.getDonGia() != null ? item.getDonGia() : prod.getGiaBan();
            item.setDonGia(unitPrice);
            item.setSoLuong(qty);
            subTotal = subTotal.add(unitPrice.multiply(BigDecimal.valueOf(qty)));

            ApplyVoucherRequest.CartItemLine line = new ApplyVoucherRequest.CartItemLine();
            line.setMaSanPham(prod.getMaSanPham());
            line.setSoLuong(qty);
            line.setDonGia(unitPrice);
            voucherLines.add(line);
        }

        String maKhuyenMai = null;
        String appliedCode = null;
        String quaTang = null;
        BigDecimal discount = BigDecimal.ZERO;

        if (req.getCodeVoucher() != null && !req.getCodeVoucher().isBlank()) {
            ApplyVoucherRequest vReq = new ApplyVoucherRequest();
            vReq.setCodeVoucher(req.getCodeVoucher().trim());
            vReq.setMaKhachHang(req.getMaKhachHang());
            vReq.setTamTinh(subTotal);
            vReq.setVatPhanTram(vatPercent);
            vReq.setItems(voucherLines);

            ApplyVoucherResponse vRes = validateAndApplyVoucher(vReq);
            if (!vRes.isValid()) {
                throw new IllegalArgumentException(vRes.getMessage());
            }
            maKhuyenMai = vRes.getMaKhuyenMai();
            appliedCode = vRes.getCodeVoucher();
            quaTang = vRes.getQuaTang();
            discount = vRes.getSoTienGiam();
        }

        BigDecimal afterDiscount = subTotal.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal vatAmount = afterDiscount.multiply(vatPercent).divide(HUNDRED, 0, RoundingMode.HALF_UP);
        BigDecimal grandTotal = afterDiscount.add(vatAmount);

        String method = (req.getPhuongThucThanhToan() == null || req.getPhuongThucThanhToan().isBlank())
                ? "Tien_mat" : req.getPhuongThucThanhToan().trim();

        BigDecimal customerGiven = req.getTienKhachDua();
        if ("Tien_mat".equalsIgnoreCase(method)) {
            if (customerGiven == null || customerGiven.compareTo(grandTotal) < 0) {
                throw new IllegalArgumentException("Tiền khách đưa phải lớn hơn hoặc bằng tổng tiền thanh toán (" + formatVnd(grandTotal) + ")");
            }
        } else if (customerGiven == null) {
            customerGiven = grandTotal;
        }
        BigDecimal change = customerGiven.subtract(grandTotal).max(BigDecimal.ZERO);

        String suffix = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
                + "_" + ThreadLocalRandom.current().nextInt(10, 99);
        String maDonHang = "DH" + suffix;
        String maHoaDon = "HD" + suffix;

        return posDao.executeCheckout(
                maDonHang, maHoaDon, req, maKhuyenMai, appliedCode, quaTang,
                subTotal, discount, vatPercent, vatAmount, grandTotal, customerGiven, change
        );
    }

    private List<ResolvedCartLine> resolveCartLines(List<ApplyVoucherRequest.CartItemLine> items) {
        List<ResolvedCartLine> list = new ArrayList<>();
        if (items == null) return list;
        for (ApplyVoucherRequest.CartItemLine item : items) {
            if (item.getMaSanPham() == null || item.getMaSanPham().isBlank()) continue;
            posDao.findProductById(item.getMaSanPham()).ifPresent(prod -> {
                int qty = item.getSoLuong() != null && item.getSoLuong() > 0 ? item.getSoLuong() : 1;
                BigDecimal unitPrice = item.getDonGia() != null ? item.getDonGia() : prod.getGiaBan();
                BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));
                list.add(new ResolvedCartLine(prod, total));
            });
        }
        return list;
    }

    private boolean matchesScope(PosProductDTO prod, PromotionResponse promo) {
        if (promo.getMaThuongHieuApDung() != null && !promo.getMaThuongHieuApDung().equals(prod.getMaThuongHieu())) {
            return false;
        }
        if (promo.getMaDanhMucApDung() != null && !promo.getMaDanhMucApDung().equals(prod.getMaDanhMuc())) {
            return false;
        }
        if (promo.getDongMayApDung() != null && !promo.getDongMayApDung().isBlank()) {
            return prod.getDongMay() != null && prod.getDongMay().toLowerCase().contains(promo.getDongMayApDung().trim().toLowerCase());
        }
        return true;
    }

    private BigDecimal calculateDiscountAmount(PromotionResponse promo, BigDecimal eligibleSubtotal, BigDecimal cartSubtotal) {
        if ("Qua_tang".equalsIgnoreCase(promo.getLoaiGiamGia())) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = promo.getGiaTriGiam() == null ? BigDecimal.ZERO : promo.getGiaTriGiam();
        BigDecimal computed;
        if ("Phan_tram".equalsIgnoreCase(promo.getLoaiGiamGia())) {
            computed = eligibleSubtotal.multiply(value).divide(HUNDRED, 0, RoundingMode.HALF_UP);
            if (promo.getGiamToiDa() != null && promo.getGiamToiDa().compareTo(BigDecimal.ZERO) > 0) {
                computed = computed.min(promo.getGiamToiDa());
            }
        } else {
            computed = value;
        }
        return computed.min(cartSubtotal).max(BigDecimal.ZERO);
    }

    private ApplyVoucherResponse invalidResponse(String errorCode, String message, BigDecimal subTotal, BigDecimal vatPercent) {
        BigDecimal vatAmount = subTotal.multiply(vatPercent).divide(HUNDRED, 0, RoundingMode.HALF_UP);
        return ApplyVoucherResponse.builder()
                .valid(false)
                .errorCode(errorCode)
                .message(message)
                .tamTinh(subTotal)
                .soTienGiam(BigDecimal.ZERO)
                .tienVat(vatAmount)
                .tongThanhToan(subTotal.add(vatAmount))
                .build();
    }

    private String formatVnd(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return nf.format(amount) + " đ";
    }

    private String formatTier(String tier) {
        return switch (tier) {
            case "Khach_VIP" -> "Khách hàng VIP";
            case "Thanh_vien" -> "Thành viên";
            case "Khach_moi" -> "Khách mới";
            default -> "Tất cả";
        };
    }

    private record ResolvedCartLine(PosProductDTO product, BigDecimal lineTotal) {
    }
}

