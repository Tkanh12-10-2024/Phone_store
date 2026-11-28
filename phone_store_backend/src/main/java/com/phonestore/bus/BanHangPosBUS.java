
package com.phonestore.bus;

import com.phonestore.dao.HoaDonDAO;
import com.phonestore.dao.KhuyenMaiDAO;
import com.phonestore.dao.PosCatalogDAO;
import com.phonestore.dao.PosEmployeeDAO;

import com.phonestore.dto.pos.PosCheckoutRequest;
import com.phonestore.dto.pos.PosCheckoutResponse;
import com.phonestore.dto.pos.PosInvoiceDetailResponse;
import com.phonestore.dto.pos.PosInvoiceResponse;
import com.phonestore.dto.pos.PosProductResponse;
import com.phonestore.dto.pos.PosCustomerResponse;
import com.phonestore.dto.pos.PosVoucherRequest;
import com.phonestore.dto.pos.PosVoucherResponse;

import com.phonestore.model.ChiTietHoaDon;
import com.phonestore.model.HoaDon;
import com.phonestore.model.KhuyenMai;
import com.phonestore.model.SanPham;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BanHangPosBUS {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    private final KhuyenMaiDAO khuyenMaiDAO;
    private final HoaDonDAO hoaDonDAO;
    private final PosCatalogDAO posCatalogDAO;
    private final PosEmployeeDAO posEmployeeDAO;

    public BanHangPosBUS(
            KhuyenMaiDAO khuyenMaiDAO,
            HoaDonDAO hoaDonDAO,
            PosCatalogDAO posCatalogDAO,
            PosEmployeeDAO posEmployeeDAO
    ) {
        this.khuyenMaiDAO = khuyenMaiDAO;
        this.hoaDonDAO = hoaDonDAO;
        this.posCatalogDAO = posCatalogDAO;
        this.posEmployeeDAO = posEmployeeDAO;
    }

    // =====================================================
    // 1. DANH SÁCH SẢN PHẨM
    // =====================================================

    public List<PosProductResponse> layDanhSachSanPham() {
        return posCatalogDAO.layDanhSachSanPham();
    }

    // =====================================================
    // 2. DANH SÁCH KHÁCH HÀNG
    // =====================================================

    public List<PosCustomerResponse> layDanhSachKhachHang() {
        return posCatalogDAO.layDanhSachKhachHang();
    }

    // =====================================================
    // 3. KIỂM TRA VOUCHER - DTO REQUEST
    // =====================================================

    public PosVoucherResponse kiemTraVaApDungVoucher(
            PosVoucherRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Dữ liệu kiểm tra voucher không hợp lệ."
            );
        }

        BigDecimal vat = request.getVatPhanTram() != null
                ? request.getVatPhanTram()
                : ZERO;

        kiemTraVAT(vat);

        List<ChiTietHoaDon> items =
                chuanHoaItemsVoucher(request.getItems());

        BigDecimal tamTinh = tinhTongTien(items);

        PosVoucherResponse response =
                new PosVoucherResponse();

        response.setCodeVoucher(request.getCodeVoucher());
        response.setTamTinh(tamTinh);

        String code = chuanHoaCodeVoucher(
                request.getCodeVoucher()
        );

        if (code == null) {
            return taoVoucherKhongHopLe(
                    response,
                    "Vui lòng nhập mã voucher.",
                    "EMPTY_CODE",
                    tamTinh,
                    vat
            );
        }

        Optional<KhuyenMai> optional =
                khuyenMaiDAO.timTheoCodeVoucher(code);

        if (optional.isEmpty()) {
            return taoVoucherKhongHopLe(
                    response,
                    "Voucher không tồn tại.",
                    "NOT_FOUND",
                    tamTinh,
                    vat
            );
        }

        KhuyenMai km = optional.get();

        String loiHieuLuc = kiemTraHieuLucVoucher(
                km,
                tamTinh
        );

        if (loiHieuLuc != null) {
            return taoVoucherKhongHopLe(
                    response,
                    loiHieuLuc,
                    xacDinhErrorCodeHieuLuc(km, tamTinh),
                    tamTinh,
                    vat
            );
        }

        String loiDieuKien = kiemTraDieuKienVoucher(
                km,
                request.getMaKhachHang(),
                items
        );

        if (loiDieuKien != null) {
            return taoVoucherKhongHopLe(
                    response,
                    loiDieuKien,
                    xacDinhErrorCodeDieuKien(
                            km,
                            request.getMaKhachHang(),
                            items
                    ),
                    tamTinh,
                    vat
            );
        }

        BigDecimal giamGia = km.tinhGiaTriGiam(tamTinh);

        BigDecimal sauGiam = tamTinh
                .subtract(giamGia)
                .max(ZERO);

        BigDecimal tienVat = tinhVAT(sauGiam, vat);

        BigDecimal tongThanhToan = sauGiam.add(tienVat);

        response.setValid(true);
        response.setSoTienGiam(giamGia);
        response.setTienVat(tienVat);
        response.setTongThanhToan(tongThanhToan);
        response.setQuaTang(km.getQuaTang());
        response.setMessage("Áp dụng voucher thành công.");
        response.setErrorCode(null);

        return response;
    }

    // =====================================================
    // 4. KIỂM TRA VOUCHER - TƯƠNG THÍCH PHƯƠNG THỨC CŨ
    // =====================================================

    public Map<String, Object> kiemTraVaApDungVoucher(
            String codeVoucher,
            BigDecimal tamTinh,
            BigDecimal vatPhanTram
    ) {
        Map<String, Object> result = new LinkedHashMap<>();

        BigDecimal subtotal = tamTinh != null
                ? tamTinh
                : ZERO;

        BigDecimal vat = vatPhanTram != null
                ? vatPhanTram
                : ZERO;

        kiemTraVAT(vat);

        result.put("valid", false);
        result.put("codeVoucher", codeVoucher);
        result.put("tamTinh", subtotal);
        result.put("soTienGiam", ZERO);
        result.put("tienVat", tinhVAT(subtotal, vat));
        result.put("tongThanhToan",
                subtotal.add(tinhVAT(subtotal, vat)));

        String code = chuanHoaCodeVoucher(codeVoucher);

        if (code == null) {
            result.put("message", "Vui lòng nhập mã voucher.");
            result.put("errorCode", "EMPTY_CODE");
            return result;
        }

        Optional<KhuyenMai> optional =
                khuyenMaiDAO.timTheoCodeVoucher(code);

        if (optional.isEmpty()) {
            result.put("message", "Voucher không tồn tại.");
            result.put("errorCode", "NOT_FOUND");
            return result;
        }

        KhuyenMai km = optional.get();

        String loi = kiemTraHieuLucVoucher(km, subtotal);

        if (loi != null) {
            result.put("message", loi);
            result.put(
                    "errorCode",
                    xacDinhErrorCodeHieuLuc(km, subtotal)
            );
            return result;
        }

        BigDecimal giamGia = km.tinhGiaTriGiam(subtotal);
        BigDecimal sauGiam = subtotal.subtract(giamGia).max(ZERO);
        BigDecimal tienVat = tinhVAT(sauGiam, vat);

        result.put("valid", true);
        result.put("soTienGiam", giamGia);
        result.put("tienVat", tienVat);
        result.put("tongThanhToan", sauGiam.add(tienVat));
        result.put("quaTang", km.getQuaTang());
        result.put("message", "Áp dụng voucher thành công.");
        result.put("errorCode", null);

        return result;
    }

    // =====================================================
    // 5. THANH TOÁN POS - DTO REQUEST
    // =====================================================

    @Transactional(rollbackFor = Exception.class)
    public PosCheckoutResponse thanhToan(
            PosCheckoutRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Dữ liệu thanh toán không hợp lệ."
            );
        }

        String maNhanVien = request.getMaNhanVien();

        if (maNhanVien == null || maNhanVien.isBlank()) {
            throw new IllegalArgumentException(
                    "Vui lòng xác định nhân viên thanh toán."
            );
        }

        if (!posEmployeeDAO.kiemTraNhanVienHoatDong(maNhanVien)) {
            throw new IllegalArgumentException(
                    "Nhân viên không tồn tại hoặc đã ngừng làm việc."
            );
        }

        BigDecimal vat = request.getVatPhanTram() != null
                ? request.getVatPhanTram()
                : ZERO;

        kiemTraVAT(vat);

        String phuongThuc = request.getPhuongThucThanhToan();

        kiemTraPhuongThucThanhToan(phuongThuc);

        List<ChiTietHoaDon> items =
                chuanHoaItemsCheckout(request.getItems());

        BigDecimal tamTinh = tinhTongTien(items);

        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Giỏ hàng chưa có sản phẩm."
            );
        }

        HoaDon hoaDon = new HoaDon();

        hoaDon.setMaNhanVien(maNhanVien);
        hoaDon.setMaKhachHang(chuanHoaMaKhachHang(
                request.getMaKhachHang()
        ));

        hoaDon.setPhuongThucThanhToan(phuongThuc);

        hoaDon.setTienKhachDua(
                request.getTienKhachDua() != null
                        ? request.getTienKhachDua()
                        : ZERO
        );

        hoaDon.setThueVatPhanTram(vat);

        for (ChiTietHoaDon item : items) {
            hoaDon.themChiTiet(item);
        }

        String codeVoucher = chuanHoaCodeVoucher(
                request.getCodeVoucher()
        );

        if (codeVoucher != null) {
            KhuyenMai km = khuyenMaiDAO
                    .timTheoCodeVoucher(codeVoucher)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Voucher không tồn tại."
                            )
                    );

            String loiHieuLuc = kiemTraHieuLucVoucher(
                    km,
                    tamTinh
            );

            if (loiHieuLuc != null) {
                throw new IllegalArgumentException(loiHieuLuc);
            }

            String loiDieuKien = kiemTraDieuKienVoucher(
                    km,
                    hoaDon.getMaKhachHang(),
                    items
            );

            if (loiDieuKien != null) {
                throw new IllegalArgumentException(loiDieuKien);
            }

            hoaDon.apDungKhuyenMai(km);
        }

        hoaDon.tinhTienVatTheoPhanTram(vat);
        hoaDon.tinhTongThanhToan();

        if (!hoaDon.thanhToan()) {
            throw new IllegalArgumentException(
                    "Số tiền khách đưa không đủ thanh toán."
            );
        }

        /*
         * HoaDonDAO chịu trách nhiệm:
         * - Khóa và phân bổ IMEI.
         * - Lưu đơn hàng.
         * - Lưu hóa đơn và chi tiết.
         * - Tăng số lượng voucher đã sử dụng trong transaction.
         *
         * BUS không tăng lượt sử dụng voucher lần nữa.
         */
        HoaDon hoaDonDaLuu =
                hoaDonDAO.luuHoaDonVaChiTiet(hoaDon);

        if (hoaDonDaLuu == null) {
            throw new IllegalStateException(
                    "Không nhận được hóa đơn sau khi lưu."
            );
        }

        return taoCheckoutResponse(
                hoaDonDaLuu,
                codeVoucher
        );
    }

    // =====================================================
    // 6. THANH TOÁN - PHƯƠNG THỨC TƯƠNG THÍCH CŨ
    // =====================================================

    @Transactional(rollbackFor = Exception.class)
    public HoaDon thanhToanHoaDon(
            String maKhachHang,
            String maNhanVien,
            String codeVoucher,
            String phuongThucThanhToan,
            BigDecimal vatPhanTram,
            BigDecimal tienKhachDua,
            List<ChiTietHoaDon> items,
            Map<String, SanPham> danhSachSanPham
    ) {
        if (maNhanVien == null || maNhanVien.isBlank()) {
            throw new IllegalArgumentException(
                    "Vui lòng xác định nhân viên."
            );
        }

        if (!posEmployeeDAO.kiemTraNhanVienHoatDong(maNhanVien)) {
            throw new IllegalArgumentException(
                    "Nhân viên không hoạt động."
            );
        }

        kiemTraVAT(vatPhanTram);
        kiemTraPhuongThucThanhToan(phuongThucThanhToan);

        List<ChiTietHoaDon> normalized =
                chuanHoaDanhSachSanPham(
                        items,
                        danhSachSanPham
                );

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Giỏ hàng chưa có sản phẩm."
            );
        }

        HoaDon hoaDon = new HoaDon();

        hoaDon.setMaKhachHang(chuanHoaMaKhachHang(maKhachHang));
        hoaDon.setMaNhanVien(maNhanVien);
        hoaDon.setPhuongThucThanhToan(phuongThucThanhToan);
        hoaDon.setTienKhachDua(
                tienKhachDua != null ? tienKhachDua : ZERO
        );

        for (ChiTietHoaDon item : normalized) {
            hoaDon.themChiTiet(item);
        }

        String code = chuanHoaCodeVoucher(codeVoucher);

        if (code != null) {
            KhuyenMai km = khuyenMaiDAO
                    .timTheoCodeVoucher(code)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Voucher không tồn tại."
                            )
                    );

            String loi = kiemTraHieuLucVoucher(
                    km,
                    hoaDon.tinhTongTienHang()
            );

            if (loi != null) {
                throw new IllegalArgumentException(loi);
            }

            String loiDieuKien = kiemTraDieuKienVoucher(
                    km,
                    hoaDon.getMaKhachHang(),
                    normalized
            );

            if (loiDieuKien != null) {
                throw new IllegalArgumentException(loiDieuKien);
            }

            hoaDon.apDungKhuyenMai(km);
        }

        hoaDon.tinhTienVatTheoPhanTram(
                vatPhanTram != null ? vatPhanTram : ZERO
        );

        if (!hoaDon.thanhToan()) {
            throw new IllegalArgumentException(
                    "Số tiền khách đưa không đủ thanh toán."
            );
        }

        return hoaDonDAO.luuHoaDonVaChiTiet(hoaDon);
    }

    // =====================================================
    // 7. CHUẨN HÓA SẢN PHẨM TỪ MAP
    // =====================================================

    public List<ChiTietHoaDon> chuanHoaDanhSachSanPham(
            List<ChiTietHoaDon> items,
            Map<String, SanPham> danhSachSanPham
    ) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        if (danhSachSanPham == null) {
            throw new IllegalArgumentException(
                    "Không tìm thấy danh sách sản phẩm."
            );
        }

        Map<String, ChiTietHoaDon> merged =
                new LinkedHashMap<>();

        for (ChiTietHoaDon item : items) {
            if (item == null
                    || item.getMaSanPham() == null
                    || item.getMaSanPham().isBlank()) {
                throw new IllegalArgumentException(
                        "Thông tin sản phẩm không hợp lệ."
                );
            }

            SanPham sp = danhSachSanPham.get(
                    item.getMaSanPham()
            );

            if (sp == null) {
                throw new IllegalArgumentException(
                        "Sản phẩm không tồn tại: "
                                + item.getMaSanPham()
                );
            }

            kiemTraSanPham(sp, item.getSoLuong());

            ChiTietHoaDon current = merged.get(
                    item.getMaSanPham()
            );

            if (current == null) {
                ChiTietHoaDon detail = new ChiTietHoaDon();

                detail.setMaSanPham(sp.getMaSanPham());
                detail.setTenSanPham(sp.getTenSanPham());
                detail.setSoLuong(item.getSoLuong());
                detail.setDonGia(sp.getGiaBan());

                detail.setThoiGianBaoHanhThang(
                        sp.getThoiGianBaoHanhThang() > 0
                                ? sp.getThoiGianBaoHanhThang()
                                : 12
                );

                merged.put(sp.getMaSanPham(), detail);

            } else {
                current.setSoLuong(
                        current.getSoLuong() + item.getSoLuong()
                );
            }
        }

        for (Map.Entry<String, ChiTietHoaDon> entry
                : merged.entrySet()) {
            SanPham sp = danhSachSanPham.get(entry.getKey());

            if (sp.getSoLuongTon()
                    < entry.getValue().getSoLuong()) {
                throw new IllegalArgumentException(
                        "Sản phẩm không đủ tồn kho: "
                                + sp.getTenSanPham()
                );
            }
        }

        return new ArrayList<>(merged.values());
    }

    // =====================================================
    // 8. CHUẨN HÓA REQUEST CHECKOUT
    // =====================================================

    private List<ChiTietHoaDon> chuanHoaItemsCheckout(
            List<PosCheckoutRequest.ItemRequest> requestItems
    ) {
        if (requestItems == null || requestItems.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, ChiTietHoaDon> merged =
                new LinkedHashMap<>();

        for (PosCheckoutRequest.ItemRequest item : requestItems) {
            if (item == null
                    || item.getMaSanPham() == null
                    || item.getMaSanPham().isBlank()) {
                throw new IllegalArgumentException(
                        "Mã sản phẩm không hợp lệ."
                );
            }

            Optional<SanPham> optional =
                    posCatalogDAO.timSanPhamTheoMa(
                            item.getMaSanPham()
                    );

            if (optional.isEmpty()) {
                throw new IllegalArgumentException(
                        "Không tìm thấy sản phẩm: "
                                + item.getMaSanPham()
                );
            }

            SanPham sp = optional.get();

            kiemTraSanPham(sp, item.getSoLuong());

            ChiTietHoaDon current =
                    merged.get(sp.getMaSanPham());

            if (current == null) {
                ChiTietHoaDon detail = new ChiTietHoaDon();

                detail.setMaSanPham(sp.getMaSanPham());
                detail.setTenSanPham(sp.getTenSanPham());
                detail.setSoLuong(item.getSoLuong());
                detail.setDonGia(sp.getGiaBan());

                detail.setThoiGianBaoHanhThang(
                        sp.getThoiGianBaoHanhThang() > 0
                                ? sp.getThoiGianBaoHanhThang()
                                : 12
                );

                merged.put(sp.getMaSanPham(), detail);

            } else {
                current.setSoLuong(
                        current.getSoLuong() + item.getSoLuong()
                );
            }
        }

        for (ChiTietHoaDon detail : merged.values()) {
            SanPham sp = posCatalogDAO
                    .timSanPhamTheoMa(detail.getMaSanPham())
                    .orElseThrow();

            kiemTraSanPham(sp, detail.getSoLuong());
        }

        return new ArrayList<>(merged.values());
    }

    // =====================================================
    // 9. CHUẨN HÓA REQUEST VOUCHER
    // =====================================================

    private List<ChiTietHoaDon> chuanHoaItemsVoucher(
            List<PosVoucherRequest.ItemRequest> requestItems
    ) {
        if (requestItems == null || requestItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "Giỏ hàng chưa có sản phẩm."
            );
        }

        Map<String, ChiTietHoaDon> merged =
                new LinkedHashMap<>();

        for (PosVoucherRequest.ItemRequest item : requestItems) {
            if (item == null
                    || item.getMaSanPham() == null
                    || item.getMaSanPham().isBlank()) {
                throw new IllegalArgumentException(
                        "Mã sản phẩm không hợp lệ."
                );
            }

            SanPham sp = posCatalogDAO
                    .timSanPhamTheoMa(item.getMaSanPham())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Sản phẩm không tồn tại: "
                                            + item.getMaSanPham()
                            )
                    );

            kiemTraSanPham(sp, item.getSoLuong());

            ChiTietHoaDon detail = merged.get(sp.getMaSanPham());

            if (detail == null) {
                detail = new ChiTietHoaDon();

                detail.setMaSanPham(sp.getMaSanPham());
                detail.setTenSanPham(sp.getTenSanPham());
                detail.setSoLuong(item.getSoLuong());
                detail.setDonGia(sp.getGiaBan());

                merged.put(sp.getMaSanPham(), detail);

            } else {
                detail.setSoLuong(
                        detail.getSoLuong() + item.getSoLuong()
                );
            }
        }

        return new ArrayList<>(merged.values());
    }

    // =====================================================
    // 10. KIỂM TRA SẢN PHẨM
    // =====================================================

    private void kiemTraSanPham(
            SanPham sp,
            int soLuong
    ) {
        if (soLuong <= 0) {
            throw new IllegalArgumentException(
                    "Số lượng sản phẩm phải lớn hơn 0."
            );
        }

        if (sp.getGiaBan() == null
                || sp.getGiaBan().compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Giá bán sản phẩm không hợp lệ."
            );
        }

        if (!"Dang_kinh_doanh".equalsIgnoreCase(
                sp.getTrangThai()
        )) {
            throw new IllegalArgumentException(
                    "Sản phẩm đã ngừng kinh doanh: "
                            + sp.getTenSanPham()
            );
        }

        if (sp.getSoLuongTon() < soLuong) {
            throw new IllegalArgumentException(
                    "Sản phẩm không đủ tồn kho: "
                            + sp.getTenSanPham()
            );
        }
    }

    // =====================================================
    // 11. KIỂM TRA ĐIỀU KIỆN VOUCHER
    // =====================================================

    private String kiemTraDieuKienVoucher(
            KhuyenMai km,
            String maKhachHang,
            List<ChiTietHoaDon> items
    ) {
        if (km.getDoiTuongKhachHang() != null
                && !km.getDoiTuongKhachHang().isBlank()
                && !"Tat_ca".equalsIgnoreCase(
                km.getDoiTuongKhachHang()
        )) {

            String hangKhachHang =
                    posCatalogDAO.layHangKhachHang(maKhachHang);

            if (!km.getDoiTuongKhachHang()
                    .equalsIgnoreCase(hangKhachHang)) {
                return "Voucher không áp dụng cho hạng khách hàng này.";
            }
        }

        boolean coDieuKienSanPham =
                km.getMaThuongHieuApDung() != null
                        || km.getMaDanhMucApDung() != null
                        || (km.getDongMayApDung() != null
                        && !km.getDongMayApDung().isBlank());

        if (!coDieuKienSanPham) {
            return null;
        }

        for (ChiTietHoaDon item : items) {
            SanPham sp = posCatalogDAO
                    .timSanPhamTheoMa(item.getMaSanPham())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Không tìm thấy sản phẩm."
                            )
                    );

            if (km.getMaThuongHieuApDung() != null
                    && !km.getMaThuongHieuApDung().equals(
                    sp.getMaThuongHieu()
            )) {
                return "Voucher không áp dụng cho thương hiệu "
                        + sp.getTenThuongHieu() + ".";
            }

            if (km.getMaDanhMucApDung() != null
                    && !km.getMaDanhMucApDung().equals(
                    sp.getMaDanhMuc()
            )) {
                return "Voucher không áp dụng cho danh mục sản phẩm này.";
            }

            if (km.getDongMayApDung() != null
                    && !km.getDongMayApDung().isBlank()
                    && !km.getDongMayApDung().equalsIgnoreCase(
                    sp.getDongMay()
            )) {
                return "Voucher không áp dụng cho dòng máy "
                        + sp.getDongMay() + ".";
            }
        }

        return null;
    }

    // =====================================================
    // 12. KIỂM TRA HIỆU LỰC VOUCHER
    // =====================================================

    private String kiemTraHieuLucVoucher(
            KhuyenMai km,
            BigDecimal tamTinh
    ) {
        if (km == null) {
            return "Voucher không tồn tại.";
        }

        if (!km.kiemTraHieuLucChoDonHang(tamTinh, true)) {
            return "Voucher không còn hiệu lực hoặc đơn hàng chưa đạt giá trị tối thiểu.";
        }

        return null;
    }

    private String xacDinhErrorCodeHieuLuc(
            KhuyenMai km,
            BigDecimal tamTinh
    ) {
        if (km.getTrangThai() != null
                && "Ngung_hoat_dong".equalsIgnoreCase(
                km.getTrangThai()
        )) {
            return "INACTIVE";
        }

        if (km.getSoLuongPhatHanh() > 0
                && km.getSoLuongDaDung()
                >= km.getSoLuongPhatHanh()) {
            return "OUT_OF_STOCK";
        }

        if (km.getGiaTriDonHangToiThieu() != null
                && tamTinh.compareTo(
                km.getGiaTriDonHangToiThieu()
        ) < 0) {
            return "MIN_ORDER_VALUE";
        }

        return "EXPIRED";
    }

    private String xacDinhErrorCodeDieuKien(
            KhuyenMai km,
            String maKhachHang,
            List<ChiTietHoaDon> items
    ) {
        if (km.getDoiTuongKhachHang() != null
                && !"Tat_ca".equalsIgnoreCase(
                km.getDoiTuongKhachHang()
        )) {
            return "CUSTOMER_TIER_MISMATCH";
        }

        return "PRODUCT_MISMATCH";
    }

    // =====================================================
    // 13. RESPONSE VOUCHER KHÔNG HỢP LỆ
    // =====================================================

    private PosVoucherResponse taoVoucherKhongHopLe(
            PosVoucherResponse response,
            String message,
            String errorCode,
            BigDecimal tamTinh,
            BigDecimal vat
    ) {
        BigDecimal tienVat = tinhVAT(tamTinh, vat);

        response.setValid(false);
        response.setSoTienGiam(ZERO);
        response.setTienVat(tienVat);
        response.setTongThanhToan(tamTinh.add(tienVat));
        response.setMessage(message);
        response.setErrorCode(errorCode);

        return response;
    }

    // =====================================================
    // 14. TẠO RESPONSE CHECKOUT
    // =====================================================

    private PosCheckoutResponse taoCheckoutResponse(
            HoaDon hoaDon,
            String codeVoucher
    ) {
        PosCheckoutResponse response =
                new PosCheckoutResponse();

        response.setSuccess(true);
        response.setMaHoaDon(hoaDon.getMaHoaDon());
        response.setMaKhachHang(hoaDon.getMaKhachHang());
        response.setTenKhachHang(hoaDon.getTenKhachHang());
        response.setMaNhanVien(hoaDon.getMaNhanVien());
        response.setTenNhanVien(hoaDon.getTenNhanVien());
        response.setNgayLap(hoaDon.getNgayLap());

        response.setTamTinh(hoaDon.getTongTienHang());
        response.setSoTienGiam(hoaDon.getTienGiamGia());
        response.setTienVat(hoaDon.getTienThueVat());
        response.setTongThanhToan(hoaDon.getTongThanhToan());

        response.setTienKhachDua(hoaDon.getTienKhachDua());
        response.setTienThua(hoaDon.getTienThua());

        response.setPhuongThucThanhToan(
                hoaDon.getPhuongThucThanhToan()
        );

        response.setCodeVoucher(codeVoucher);
        response.setMessage("Thanh toán thành công.");

        List<PosInvoiceDetailResponse> detailResponses =
                new ArrayList<>();

        if (hoaDon.getItems() != null) {
            for (ChiTietHoaDon item : hoaDon.getItems()) {
                PosInvoiceDetailResponse detail =
                        new PosInvoiceDetailResponse();

                detail.setMaSanPham(item.getMaSanPham());
                detail.setTenSanPham(item.getTenSanPham());
                detail.setSoLuong(item.getSoLuong());
                detail.setDonGia(item.getDonGia());
                detail.setThanhTien(item.tinhThanhTien());

                detail.setThoiGianBaoHanhThang(
                        item.getThoiGianBaoHanhThang()
                );

                List<String> imeis = new ArrayList<>();

                if (item.getMaImei() != null
                        && !item.getMaImei().isBlank()) {
                    imeis.add(item.getMaImei());
                }

                detail.setDanhSachImei(imeis);

                detailResponses.add(detail);
            }
        }

        response.setItems(detailResponses);

        return response;
    }

    // =====================================================
    // 15. HÓA ĐƠN GẦN NHẤT
    // =====================================================

    public List<HoaDon> layHoaDonGanNhat(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));

        return hoaDonDAO.layHoaDonGanNhat(safeLimit);
    }

    // =====================================================
    // 16. CÁC HÀM TIỆN ÍCH
    // =====================================================

    private BigDecimal tinhTongTien(
            List<ChiTietHoaDon> items
    ) {
        if (items == null) {
            return ZERO;
        }

        return items.stream()
                .map(ChiTietHoaDon::tinhThanhTien)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal tinhVAT(
            BigDecimal tienTinhVAT,
            BigDecimal vatPhanTram
    ) {
        return tienTinhVAT
                .multiply(vatPhanTram)
                .divide(ONE_HUNDRED, 0,
                        java.math.RoundingMode.HALF_UP);
    }

    private void kiemTraVAT(BigDecimal vat) {
        if (vat == null
                || vat.compareTo(ZERO) < 0
                || vat.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException(
                    "VAT phải nằm trong khoảng từ 0 đến 100."
            );
        }
    }

    private void kiemTraPhuongThucThanhToan(
            String phuongThuc
    ) {
        if (phuongThuc == null) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn phương thức thanh toán."
            );
        }

        if (!"Tien_mat".equals(phuongThuc)
                && !"Chuyen_khoan_QR".equals(phuongThuc)
                && !"The".equals(phuongThuc)) {
            throw new IllegalArgumentException(
                    "Phương thức thanh toán không hợp lệ."
            );
        }
    }

    private String chuanHoaCodeVoucher(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        return code.trim();
    }

    private String chuanHoaMaKhachHang(String maKhachHang) {
        if (maKhachHang == null
                || maKhachHang.isBlank()
                || "KH_LE".equalsIgnoreCase(maKhachHang.trim())) {
            return null;
        }

        return maKhachHang.trim();
    }
}
