
package com.phonestore.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HoaDon {

    private String maHoaDon;
    private LocalDateTime ngayLap = LocalDateTime.now();

    // Thông tin tiền hàng và thuế
    private BigDecimal tongTienHang = BigDecimal.ZERO;
    private BigDecimal thueVatPhanTram = BigDecimal.ZERO;
    private BigDecimal tienThueVat = BigDecimal.ZERO;
    private BigDecimal tienGiamGia = BigDecimal.ZERO;
    private BigDecimal tongThanhToan = BigDecimal.ZERO;

    // Thông tin thanh toán
    private String phuongThucThanhToan = "Tien_mat";
    private BigDecimal tienKhachDua = BigDecimal.ZERO;
    private BigDecimal tienThua = BigDecimal.ZERO;
    private String trangThaiThanhToan = "Chua_thanh_toan";

    // Thông tin bổ sung
    private String ghiChu;

    // Khách hàng và nhân viên
    private String maKhachHang;
    private String tenKhachHang;
    private String maNhanVien;
    private String tenNhanVien;

    // Khuyến mãi
    private String codeVoucher;
    private String quaTang;

    // Danh sách sản phẩm trong hóa đơn
    private List<ChiTietHoaDon> items = new ArrayList<>();

    /*
     * Khởi tạo hóa đơn.
     */
    public void taoHoaDon() {
        this.ngayLap = LocalDateTime.now();
        this.trangThaiThanhToan = "Cho_thanh_toan";
        tinhTongThanhToan();
    }

    /*
     * Thêm sản phẩm vào hóa đơn.
     */
    public void themChiTiet(ChiTietHoaDon chiTiet) {
        if (chiTiet != null) {
            this.items.add(chiTiet);
            tinhTongTienHang();
        }
    }

    /*
     * Tính tổng tiền hàng.
     */
    public BigDecimal tinhTongTienHang() {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }

        this.tongTienHang = this.items.stream()
                .map(ChiTietHoaDon::tinhThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return this.tongTienHang;
    }

    /*
     * Áp dụng khuyến mãi.
     */
    public void apDungKhuyenMai(KhuyenMai km) {

        if (km == null) {
            this.codeVoucher = null;
            this.tienGiamGia = BigDecimal.ZERO;
            this.quaTang = null;
            return;
        }

        tinhTongTienHang();

        this.codeVoucher = km.getCodeVoucher();

        this.tienGiamGia = km.tinhGiaTriGiam(
                this.tongTienHang
        );

        if ("Qua_tang".equalsIgnoreCase(km.getLoaiGiamGia())) {
            this.quaTang = km.getTenKhuyenMai();
        } else {
            this.quaTang = null;
        }
    }

    /*
     * Tính tiền VAT theo phần trăm.
     *
     * Ví dụ:
     * Tổng tiền hàng: 10.000.000
     * Giảm giá:       1.000.000
     * VAT:            10%
     * Tiền VAT:       900.000
     */
    public void tinhTienVatTheoPhanTram(BigDecimal vatPhanTram) {

        tinhTongTienHang();

        BigDecimal rate = vatPhanTram != null
                ? vatPhanTram
                : BigDecimal.ZERO;

        if (rate.compareTo(BigDecimal.ZERO) < 0
                || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    "Tỷ lệ VAT phải nằm trong khoảng từ 0 đến 100."
            );
        }

        this.thueVatPhanTram = rate;

        BigDecimal giamGia = this.tienGiamGia != null
                ? this.tienGiamGia
                : BigDecimal.ZERO;

        BigDecimal sauGiam = this.tongTienHang
                .subtract(giamGia)
                .max(BigDecimal.ZERO);

        this.tienThueVat = sauGiam
                .multiply(rate)
                .divide(
                        BigDecimal.valueOf(100),
                        0,
                        RoundingMode.HALF_UP
                );
    }

    /*
     * Tính tổng thanh toán.
     *
     * Tổng thanh toán = Tiền hàng - Giảm giá + VAT
     */
    public BigDecimal tinhTongThanhToan() {

        tinhTongTienHang();

        BigDecimal giamGia = this.tienGiamGia != null
                ? this.tienGiamGia
                : BigDecimal.ZERO;

        BigDecimal vat = this.tienThueVat != null
                ? this.tienThueVat
                : BigDecimal.ZERO;

        BigDecimal sauGiam = this.tongTienHang
                .subtract(giamGia)
                .max(BigDecimal.ZERO);

        this.tongThanhToan = sauGiam.add(vat);

        tinhTienThua();

        return this.tongThanhToan;
    }

    /*
     * Tính tiền thừa trả khách.
     */
    public BigDecimal tinhTienThua() {

        BigDecimal khachDua = this.tienKhachDua != null
                ? this.tienKhachDua
                : BigDecimal.ZERO;

        BigDecimal canTra = this.tongThanhToan != null
                ? this.tongThanhToan
                : BigDecimal.ZERO;

        this.tienThua = khachDua
                .subtract(canTra)
                .max(BigDecimal.ZERO);

        return this.tienThua;
    }

    /*
     * Xác nhận thanh toán.
     */
    public boolean thanhToan() {

        tinhTongThanhToan();

        if ("Tien_mat".equals(this.phuongThucThanhToan)) {

            if (this.tienKhachDua == null
                    || this.tienKhachDua.compareTo(this.tongThanhToan) < 0) {
                return false;
            }

            tinhTienThua();

        } else {

            this.tienKhachDua = this.tongThanhToan;
            this.tienThua = BigDecimal.ZERO;
        }

        this.trangThaiThanhToan = "Da_thanh_toan";

        return true;
    }

    /*
     * Hoàn tiền hóa đơn.
     */
    public boolean hoanTien() {

        if (!"Da_thanh_toan".equals(this.trangThaiThanhToan)) {
            return false;
        }

        this.trangThaiThanhToan = "Da_hoan_tien";

        return true;
    }

    /*
     * In hóa đơn - sẽ triển khai sau.
     */
    public void inHoaDon() {
        // TODO: Triển khai chức năng in hóa đơn.
    }

    // =========================
    // GETTER / SETTER
    // =========================

    public String getMaHoaDon() {
        return maHoaDon;
    }

    public void setMaHoaDon(String maHoaDon) {
        this.maHoaDon = maHoaDon;
    }

    public LocalDateTime getNgayLap() {
        return ngayLap;
    }

    public void setNgayLap(LocalDateTime ngayLap) {
        this.ngayLap = ngayLap;
    }

    public BigDecimal getTongTienHang() {
        return tongTienHang;
    }

    public void setTongTienHang(BigDecimal tongTienHang) {
        this.tongTienHang = tongTienHang != null
                ? tongTienHang
                : BigDecimal.ZERO;
    }

    public BigDecimal getThueVatPhanTram() {
        return thueVatPhanTram;
    }

    public void setThueVatPhanTram(BigDecimal thueVatPhanTram) {
        this.thueVatPhanTram = thueVatPhanTram != null
                ? thueVatPhanTram
                : BigDecimal.ZERO;
    }

    public BigDecimal getTienThueVat() {
        return tienThueVat;
    }

    public void setTienThueVat(BigDecimal tienThueVat) {
        this.tienThueVat = tienThueVat != null
                ? tienThueVat
                : BigDecimal.ZERO;
    }

    public BigDecimal getTienGiamGia() {
        return tienGiamGia;
    }

    public void setTienGiamGia(BigDecimal tienGiamGia) {
        this.tienGiamGia = tienGiamGia != null
                ? tienGiamGia
                : BigDecimal.ZERO;
    }

    public BigDecimal getTongThanhToan() {
        return tongThanhToan;
    }

    public void setTongThanhToan(BigDecimal tongThanhToan) {
        this.tongThanhToan = tongThanhToan != null
                ? tongThanhToan
                : BigDecimal.ZERO;
    }

    public String getPhuongThucThanhToan() {
        return phuongThucThanhToan;
    }

    public void setPhuongThucThanhToan(String phuongThucThanhToan) {
        this.phuongThucThanhToan = phuongThucThanhToan;
    }

    public BigDecimal getTienKhachDua() {
        return tienKhachDua;
    }

    public void setTienKhachDua(BigDecimal tienKhachDua) {
        this.tienKhachDua = tienKhachDua != null
                ? tienKhachDua
                : BigDecimal.ZERO;
    }

    public BigDecimal getTienThua() {
        return tienThua;
    }

    public void setTienThua(BigDecimal tienThua) {
        this.tienThua = tienThua != null
                ? tienThua
                : BigDecimal.ZERO;
    }

    public String getTrangThaiThanhToan() {
        return trangThaiThanhToan;
    }

    public void setTrangThaiThanhToan(String trangThaiThanhToan) {
        this.trangThaiThanhToan = trangThaiThanhToan;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public String getMaKhachHang() {
        return maKhachHang;
    }

    public void setMaKhachHang(String maKhachHang) {
        this.maKhachHang = maKhachHang;
    }

    public String getTenKhachHang() {
        return tenKhachHang;
    }

    public void setTenKhachHang(String tenKhachHang) {
        this.tenKhachHang = tenKhachHang;
    }

    public String getMaNhanVien() {
        return maNhanVien;
    }

    public void setMaNhanVien(String maNhanVien) {
        this.maNhanVien = maNhanVien;
    }

    public String getTenNhanVien() {
        return tenNhanVien;
    }

    public void setTenNhanVien(String tenNhanVien) {
        this.tenNhanVien = tenNhanVien;
    }

    public String getCodeVoucher() {
        return codeVoucher;
    }

    public void setCodeVoucher(String codeVoucher) {
        this.codeVoucher = codeVoucher;
    }

    public String getQuaTang() {
        return quaTang;
    }

    public void setQuaTang(String quaTang) {
        this.quaTang = quaTang;
    }

    public List<ChiTietHoaDon> getItems() {
        return items;
    }

    public void setItems(List<ChiTietHoaDon> items) {
        this.items = items != null
                ? items
                : new ArrayList<>();
    }
}
