
package com.phonestore.bus;

import com.phonestore.dao.HoaDonDAO;
import com.phonestore.dao.KhuyenMaiDAO;
import com.phonestore.model.ChiTietHoaDon;
import com.phonestore.model.HoaDon;
import com.phonestore.model.KhuyenMai;
import com.phonestore.model.SanPham;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BanHangPosBUS {

    private final KhuyenMaiDAO khuyenMaiDAO;
    private final HoaDonDAO hoaDonDAO;

    public BanHangPosBUS(
            KhuyenMaiDAO khuyenMaiDAO,
            HoaDonDAO hoaDonDAO
    ) {
        this.khuyenMaiDAO = khuyenMaiDAO;
        this.hoaDonDAO = hoaDonDAO;
    }

    /*
     * Kiểm tra và tính thử voucher trước khi thanh toán.
     */
    public Map<String, Object> kiemTraVaApDungVoucher(
            String codeVoucher,
            BigDecimal tamTinh,
            BigDecimal vatPhanTram
    ) {

        BigDecimal tienHang = tamTinh != null
                ? tamTinh
                : BigDecimal.ZERO;

        BigDecimal vatRate = vatPhanTram != null
                ? vatPhanTram
                : BigDecimal.ZERO;

        Map<String, Object> res = new HashMap<>();

        if (codeVoucher == null || codeVoucher.isBlank()) {
            res.put("valid", false);
            res.put("message", "Vui lòng nhập mã voucher.");
            return res;
        }

        if (tienHang.compareTo(BigDecimal.ZERO) < 0
                || vatRate.compareTo(BigDecimal.ZERO) < 0
                || vatRate.compareTo(BigDecimal.valueOf(100)) > 0) {

            res.put("valid", false);
            res.put("message", "Thông tin tiền hàng hoặc VAT không hợp lệ.");
            return res;
        }

        KhuyenMai km = khuyenMaiDAO.timTheoCodeVoucher(
                codeVoucher.trim()
        ).orElse(null);

        if (km == null) {
            res.put("valid", false);
            res.put("codeVoucher", codeVoucher);
            res.put("message", "Mã voucher không tồn tại.");
            return res;
        }

        if (!km.kiemTraHieuLucChoDonHang(tienHang, true)) {
            res.put("valid", false);
            res.put("codeVoucher", km.getCodeVoucher());
            res.put("message",
                    "Voucher không hợp lệ, đã hết hạn hoặc hết lượt sử dụng.");
            return res;
        }

        BigDecimal soTienGiam = km.tinhGiaTriGiam(tienHang);

        BigDecimal sauGiam = tienHang
                .subtract(soTienGiam)
                .max(BigDecimal.ZERO);

        BigDecimal tienVat = sauGiam
                .multiply(vatRate)
                .divide(
                        BigDecimal.valueOf(100),
                        0,
                        RoundingMode.HALF_UP
                );

        BigDecimal tongThanhToan = sauGiam.add(tienVat);

        String quaTang = "Qua_tang".equalsIgnoreCase(
                km.getLoaiGiamGia()
        ) ? km.getTenKhuyenMai() : null;

        res.put("valid", true);
        res.put("codeVoucher", km.getCodeVoucher());
        res.put("tamTinh", tienHang);
        res.put("soTienGiam", soTienGiam);
        res.put("tienVat", tienVat);
        res.put("tongThanhToan", tongThanhToan);
        res.put("quaTang", quaTang);
        res.put("message",
                "Áp dụng mã " + km.getCodeVoucher() + " thành công.");

        return res;
    }

    /*
     * Chuẩn hóa danh sách sản phẩm gửi từ frontend.
     *
     * - Frontend chỉ gửi mã sản phẩm và số lượng.
     * - Backend tự lấy giá bán từ SanPham.
     * - Không nhận IMEI từ frontend.
     * - Gộp các dòng trùng mã sản phẩm.
     */
    private List<ChiTietHoaDon> chuanHoaDanhSachSanPham(
            List<ChiTietHoaDon> danhSachItems,
            Map<String, SanPham> sanPhamMap
    ) {

        if (danhSachItems == null || danhSachItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "Giỏ hàng chưa có sản phẩm."
            );
        }

        if (sanPhamMap == null || sanPhamMap.isEmpty()) {
            throw new IllegalArgumentException(
                    "Không tìm thấy dữ liệu sản phẩm."
            );
        }

        Map<String, ChiTietHoaDon> daGop = new HashMap<>();

        for (ChiTietHoaDon item : danhSachItems) {

            if (item == null) {
                throw new IllegalArgumentException(
                        "Chi tiết sản phẩm không được để trống."
                );
            }

            String maSanPham = item.getMaSanPham();

            if (maSanPham == null || maSanPham.isBlank()) {
                throw new IllegalArgumentException(
                        "Thiếu mã sản phẩm trong giỏ hàng."
                );
            }

            if (item.getSoLuong() <= 0) {
                throw new IllegalArgumentException(
                        "Số lượng sản phẩm phải lớn hơn 0."
                );
            }

            SanPham sp = sanPhamMap.get(maSanPham);

            if (sp == null) {
                throw new IllegalArgumentException(
                        "Không tìm thấy sản phẩm: " + maSanPham
                );
            }

            if (!"Dang_kinh_doanh".equals(sp.getTrangThai())) {
                throw new IllegalArgumentException(
                        "Sản phẩm đã ngừng kinh doanh: "
                                + sp.getTenSanPham()
                );
            }

            if (sp.getGiaBan() == null
                    || sp.getGiaBan().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Giá bán sản phẩm không hợp lệ: "
                                + sp.getTenSanPham()
                );
            }

            ChiTietHoaDon daCo = daGop.get(maSanPham);

            if (daCo == null) {

                ChiTietHoaDon moi = new ChiTietHoaDon();

                moi.setMaSanPham(sp.getMaSanPham());
                moi.setTenSanPham(sp.getTenSanPham());
                moi.setSoLuong(item.getSoLuong());

                // Giá bán luôn lấy từ dữ liệu backend.
                moi.setDonGia(sp.getGiaBan());

                moi.setThoiGianBaoHanhThang(
                        sp.getThoiGianBaoHanhThang() > 0
                                ? sp.getThoiGianBaoHanhThang()
                                : 12
                );

                daGop.put(maSanPham, moi);

            } else {

                daCo.setSoLuong(
                        daCo.getSoLuong() + item.getSoLuong()
                );
            }
        }

        return new ArrayList<>(daGop.values());
    }

    /*
     * Xử lý thanh toán POS.
     */
    public HoaDon thanhToanHoaDon(
            String maKhachHang,
            String tenKhachHang,
            String maNhanVien,
            String tenNhanVien,
            String codeVoucher,
            String phuongThucThanhToan,
            BigDecimal vatPhanTram,
            BigDecimal tienKhachDua,
            List<ChiTietHoaDon> danhSachItems,
            Map<String, SanPham> sanPhamMap
    ) {

        // 1. Kiểm tra thông tin nhân viên.
        if (maNhanVien == null || maNhanVien.isBlank()) {
            throw new IllegalArgumentException(
                    "Không xác định được nhân viên đang đăng nhập."
            );
        }

        // 2. Kiểm tra phương thức thanh toán.
        if (phuongThucThanhToan == null
                || phuongThucThanhToan.isBlank()) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn phương thức thanh toán."
            );
        }

        // 3. Kiểm tra VAT.
        BigDecimal vatRate = vatPhanTram != null
                ? vatPhanTram
                : BigDecimal.ZERO;

        if (vatRate.compareTo(BigDecimal.ZERO) < 0
                || vatRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    "Tỷ lệ VAT không hợp lệ."
            );
        }

        // 4. Chuẩn hóa sản phẩm và lấy giá từ backend.
        List<ChiTietHoaDon> itemsDaChuanHoa =
                chuanHoaDanhSachSanPham(
                        danhSachItems,
                        sanPhamMap
                );

        // 5. Khởi tạo hóa đơn.
        HoaDon hoaDon = new HoaDon();

        hoaDon.setMaHoaDon(
                hoaDonDAO.sinhMaHoaDonMoi()
        );

        hoaDon.setMaKhachHang(
                maKhachHang != null && !maKhachHang.isBlank()
                        ? maKhachHang.trim()
                        : null
        );

        hoaDon.setTenKhachHang(
                tenKhachHang != null && !tenKhachHang.isBlank()
                        ? tenKhachHang.trim()
                        : "Khách vãng lai"
        );

        hoaDon.setMaNhanVien(maNhanVien.trim());
        hoaDon.setTenNhanVien(tenNhanVien);

        hoaDon.setPhuongThucThanhToan(
                phuongThucThanhToan.trim()
        );

        hoaDon.taoHoaDon();

        // 6. Thêm sản phẩm vào hóa đơn.
        for (ChiTietHoaDon item : itemsDaChuanHoa) {
            hoaDon.themChiTiet(item);
        }

        // 7. Kiểm tra và áp dụng voucher.
        if (codeVoucher != null && !codeVoucher.isBlank()) {

            KhuyenMai km = khuyenMaiDAO.timTheoCodeVoucher(
                    codeVoucher.trim()
            ).orElse(null);

            if (km == null) {
                throw new IllegalArgumentException(
                        "Voucher không tồn tại."
                );
            }

            if (!km.kiemTraHieuLucChoDonHang(
                    hoaDon.getTongTienHang(),
                    true
            )) {
                throw new IllegalArgumentException(
                        "Voucher đã hết hạn, hết lượt hoặc không đủ điều kiện."
                );
            }

            hoaDon.apDungKhuyenMai(km);
        }

        // 8. Tính VAT và tổng thanh toán.
        hoaDon.tinhTienVatTheoPhanTram(vatRate);
        hoaDon.tinhTongThanhToan();

        // 9. Ghi nhận số tiền khách đưa.
        hoaDon.setTienKhachDua(
                tienKhachDua != null
                        ? tienKhachDua
                        : BigDecimal.ZERO
        );

        // 10. Kiểm tra thanh toán.
        if (!hoaDon.thanhToan()) {
            throw new IllegalArgumentException(
                    "Tiền khách đưa chưa đủ để thanh toán hóa đơn."
            );
        }

        // 11. DAO lưu đơn hàng, hóa đơn và tự phân bổ IMEI.
        // Không giảm tồn kho trong BUS.
        HoaDon daLuu = hoaDonDAO.luuHoaDonVaChiTiet(hoaDon);

        // Lưu ý:
        // Việc tăng lượt sử dụng voucher cần được chuyển vào
        // transaction của HoaDonDAO ở bước tiếp theo.

        return daLuu;
    }

    /*
     * Lấy danh sách hóa đơn gần nhất.
     */
    public List<HoaDon> layHoaDonGanNhat(int limit) {

        int soLuong = limit > 0
                ? Math.min(limit, 100)
                : 10;

        return hoaDonDAO.layHoaDonGanNhat(soLuong);
    }
}
