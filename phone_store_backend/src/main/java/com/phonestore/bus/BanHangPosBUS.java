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

    public BanHangPosBUS(KhuyenMaiDAO khuyenMaiDAO, HoaDonDAO hoaDonDAO) {
        this.khuyenMaiDAO = khuyenMaiDAO;
        this.hoaDonDAO = hoaDonDAO;
    }

    public Map<String, Object> kiemTraVaApDungVoucher(String codeVoucher,
                                                      BigDecimal tamTinh,
                                                      BigDecimal vatPhanTram) {
        BigDecimal tienHang = tamTinh != null ? tamTinh : BigDecimal.ZERO;
        BigDecimal vatRate = vatPhanTram != null ? vatPhanTram : BigDecimal.ZERO;

        KhuyenMai km = khuyenMaiDAO.timTheoCodeVoucher(
                codeVoucher != null ? codeVoucher.trim() : ""
        ).orElse(null);

        Map<String, Object> res = new HashMap<>();
        if (km == null) {
            res.put("valid", false);
            res.put("codeVoucher", codeVoucher);
            res.put("message", "Mã voucher không tồn tại.");
            return res;
        }

        if (!km.kiemTraHieuLucChoDonHang(tienHang, false)) {
            res.put("valid", false);
            res.put("codeVoucher", km.getCodeVoucher());
            res.put("message", "Voucher đã hết hạn, ngừng hoạt động hoặc hết lượt sử dụng.");
            return res;
        }

        if (km.getGiaTriDonHangToiThieu() != null
                && tienHang.compareTo(km.getGiaTriDonHangToiThieu()) < 0) {
            res.put("valid", false);
            res.put("codeVoucher", km.getCodeVoucher());
            res.put("message", "Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher.");
            return res;
        }

        BigDecimal soTienGiam = km.tinhGiaTriGiam(tienHang);
        BigDecimal sauGiam = tienHang.subtract(soTienGiam).max(BigDecimal.ZERO);
        BigDecimal tienVat = sauGiam.multiply(vatRate)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal tongThanhToan = sauGiam.add(tienVat);
        String quaTang = "Qua_tang".equalsIgnoreCase(km.getLoaiGiamGia())
                ? km.getTenKhuyenMai() : null;

        res.put("valid", true);
        res.put("codeVoucher", km.getCodeVoucher());
        res.put("tamTinh", tienHang);
        res.put("soTienGiam", soTienGiam);
        res.put("tienVat", tienVat);
        res.put("tongThanhToan", tongThanhToan);
        res.put("quaTang", quaTang);
        res.put("message", "Áp dụng mã " + km.getCodeVoucher() + " thành công.");
        return res;
    }

    public HoaDon thanhToanHoaDon(String maKhachHang,
                                  String tenKhachHang,
                                  String maNhanVien,
                                  String tenNhanVien,
                                  String codeVoucher,
                                  String phuongThucThanhToan,
                                  BigDecimal vatPhanTram,
                                  BigDecimal tienKhachDua,
                                  List<ChiTietHoaDon> danhSachItems,
                                  Map<String, SanPham> sanPhamMap) {
        HoaDon hoaDon = new HoaDon();
        hoaDon.setMaHoaDon(hoaDonDAO.sinhMaHoaDonMoi());
        hoaDon.setMaKhachHang(maKhachHang != null && !maKhachHang.isBlank() ? maKhachHang : "KH_LE");
        hoaDon.setTenKhachHang(tenKhachHang != null && !tenKhachHang.isBlank() ? tenKhachHang : "Khách vãng lai");
        hoaDon.setMaNhanVien(maNhanVien);
        hoaDon.setTenNhanVien(tenNhanVien);
        hoaDon.setPhuongThucThanhToan(
                phuongThucThanhToan != null && !phuongThucThanhToan.isBlank() ? phuongThucThanhToan : "Tien_mat"
        );
        hoaDon.taoHoaDon();

        List<SanPham> sanPhamCanLuu = new ArrayList<>();
        if (danhSachItems != null) {
            for (ChiTietHoaDon item : danhSachItems) {
                SanPham sp = sanPhamMap != null ? sanPhamMap.get(item.getMaSanPham()) : null;
                if (sp != null) {
                    if (!sp.giamTraTonKho(item.getSoLuong())) {
                        throw new IllegalStateException("Sản phẩm " + sp.getMaSanPham() + " không đủ tồn kho.");
                    }
                    sanPhamCanLuu.add(sp);
                    if (item.getTenSanPham() == null || item.getTenSanPham().isBlank()) {
                        item.setTenSanPham(sp.getTenSanPham());
                    }
                    if (item.getDonGia() == null || item.getDonGia().compareTo(BigDecimal.ZERO) <= 0) {
                        item.setDonGia(sp.getGiaBan());
                    }
                    if (item.getThoiGianBaoHanhThang() <= 0) {
                        item.setThoiGianBaoHanhThang(sp.getThoiGianBaoHanhThang());
                    }
                }
                hoaDon.themChiTiet(item);
            }
        }

        KhuyenMai voucherSuDung = null;
        if (codeVoucher != null && !codeVoucher.isBlank()) {
            KhuyenMai km = khuyenMaiDAO.timTheoCodeVoucher(codeVoucher.trim()).orElse(null);
            if (km != null && km.kiemTraHieuLucChoDonHang(hoaDon.getTongTienHang(), true)) {
                hoaDon.apDungKhuyenMai(km);
                if (km.suDungKhuyenMai()) {
                    voucherSuDung = km;
                }
            }
        }

        hoaDon.tinhTienVatTheoPhanTram(vatPhanTram);
        hoaDon.setTienKhachDua(tienKhachDua);

        if (!hoaDon.thanhToan()) {
            throw new IllegalArgumentException("Tiền khách đưa chưa đủ để thanh toán hóa đơn.");
        }

        HoaDon daLuu = hoaDonDAO.luuHoaDonVaChiTiet(hoaDon);
        if (voucherSuDung != null) {
            khuyenMaiDAO.capNhatSoLuongDaDung(voucherSuDung);
        }
        // TODO: Lưu danh sách sanPhamCanLuu và cập nhật trạng thái IMEI đã bán nếu cần
        return daLuu;
    }

    public List<HoaDon> layHoaDonGanNhat(int limit) {
        int soLuong = limit > 0 ? limit : 10;
        return hoaDonDAO.layHoaDonGanNhat(soLuong);
    }
}