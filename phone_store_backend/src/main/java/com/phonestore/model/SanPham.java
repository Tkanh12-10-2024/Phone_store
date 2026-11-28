package com.phonestore.model;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SanPham {
    private String maSanPham;
    private String tenSanPham;
    private String dongMay;
    private String mauSac;
    private String ram;
    private String rom;
    private String manHinh;
    private String chipXuLy;
    private String pin;
    private String camera;
    private String heDieuHanh;
    private BigDecimal giaNhapThamKhao = BigDecimal.ZERO;
    private BigDecimal giaBan = BigDecimal.ZERO;
    private int thoiGianBaoHanhThang = 12;
    private int soLuongTon;
    private int dinhMucTonToiThieu = 2;
    private String hinhAnh;
    private String trangThai = "Dang_kinh_doanh";
    private LocalDateTime ngayTao = LocalDateTime.now();

    private Integer maThuongHieu;
    private String tenThuongHieu;
    private Integer maDanhMuc;
    private String tenDanhMuc;
    private List<String> galleryUrls = new ArrayList<>();

    public void themSanPham() {
        if (this.tenSanPham == null || this.tenSanPham.isBlank()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }
        if (this.giaBan == null || this.giaBan.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá bán phải lớn hơn 0");
        }
        if (this.maSanPham == null || this.maSanPham.isBlank()) {
            String ma = ("SP_" + chuanHoa(this.dongMay, 8) + "_" + chuanHoa(this.rom, 5) + chuanHoa(this.mauSac, 4)).toUpperCase();
            this.maSanPham = ma.length() > 20 ? ma.substring(0, 20) : ma;
        }
        this.ngayTao = LocalDateTime.now();
    }

    public void capNhatSanPham(SanPham moi) {
        if (moi == null) return;
        this.tenSanPham = moi.tenSanPham;
        this.dongMay = moi.dongMay;
        this.mauSac = moi.mauSac;
        this.ram = moi.ram;
        this.rom = moi.rom;
        this.manHinh = moi.manHinh;
        this.chipXuLy = moi.chipXuLy;
        this.pin = moi.pin;
        this.camera = moi.camera;
        this.heDieuHanh = moi.heDieuHanh;
        this.giaNhapThamKhao = moi.giaNhapThamKhao;
        this.giaBan = moi.giaBan;
        this.thoiGianBaoHanhThang = moi.thoiGianBaoHanhThang;
        this.dinhMucTonToiThieu = moi.dinhMucTonToiThieu;
        this.maThuongHieu = moi.maThuongHieu;
        this.tenThuongHieu = moi.tenThuongHieu;
        this.maDanhMuc = moi.maDanhMuc;
        this.tenDanhMuc = moi.tenDanhMuc;
        if (moi.hinhAnh != null && !moi.hinhAnh.isBlank()) {
            this.hinhAnh = moi.hinhAnh;
        }
        if (moi.galleryUrls != null) {
            this.galleryUrls = new ArrayList<>(moi.galleryUrls);
        }
        if (moi.trangThai != null) {
            this.trangThai = moi.trangThai;
        }
    }

    public void xoaSanPham(boolean daPhatSinhGiaoDich) {
        if (daPhatSinhGiaoDich) {
            this.trangThai = "Ngung_kinh_doanh";
        }
    }

    public SanPham timKiemSanPham(List<SanPham> danhSach, String maCanTim) {
        if (danhSach == null || maCanTim == null) return null;
        return danhSach.stream()
                .filter(sp -> maCanTim.equalsIgnoreCase(sp.maSanPham))
                .findFirst()
                .orElse(null);
    }

    public void capNhatSoLuongTon(int soLuongMoi) {
        if (soLuongMoi < 0) {
            throw new IllegalArgumentException("Số lượng tồn không được âm");
        }
        this.soLuongTon = soLuongMoi;
    }

    public boolean giamTraTonKho(int soLuongBan) {
        if (soLuongBan <= 0 || this.soLuongTon < soLuongBan) {
            return false;
        }
        this.soLuongTon -= soLuongBan;
        return true;
    }

    private String chuanHoa(String s, int max) {
        if (s == null) return "";
        String clean = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .replaceAll("[^a-zA-Z0-9]", "");
        return clean.length() > max ? clean.substring(0, max) : clean;
    }

    public String getMaSanPham() { return maSanPham; }
    public void setMaSanPham(String maSanPham) { this.maSanPham = maSanPham; }
    public String getTenSanPham() { return tenSanPham; }
    public void setTenSanPham(String tenSanPham) { this.tenSanPham = tenSanPham; }
    public String getDongMay() { return dongMay; }
    public void setDongMay(String dongMay) { this.dongMay = dongMay; }
    public String getMauSac() { return mauSac; }
    public void setMauSac(String mauSac) { this.mauSac = mauSac; }
    public String getRam() { return ram; }
    public void setRam(String ram) { this.ram = ram; }
    public String getRom() { return rom; }
    public void setRom(String rom) { this.rom = rom; }
    public String getManHinh() { return manHinh; }
    public void setManHinh(String manHinh) { this.manHinh = manHinh; }
    public String getChipXuLy() { return chipXuLy; }
    public void setChipXuLy(String chipXuLy) { this.chipXuLy = chipXuLy; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    public String getCamera() { return camera; }
    public void setCamera(String camera) { this.camera = camera; }
    public String getHeDieuHanh() { return heDieuHanh; }
    public void setHeDieuHanh(String heDieuHanh) { this.heDieuHanh = heDieuHanh; }
    public BigDecimal getGiaNhapThamKhao() { return giaNhapThamKhao; }
    public void setGiaNhapThamKhao(BigDecimal giaNhapThamKhao) { this.giaNhapThamKhao = giaNhapThamKhao; }
    public BigDecimal getGiaBan() { return giaBan; }
    public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }
    public int getThoiGianBaoHanhThang() { return thoiGianBaoHanhThang; }
    public void setThoiGianBaoHanhThang(int thoiGianBaoHanhThang) { this.thoiGianBaoHanhThang = thoiGianBaoHanhThang; }
    public int getSoLuongTon() { return soLuongTon; }
    public void setSoLuongTon(int soLuongTon) { this.soLuongTon = soLuongTon; }
    public int getDinhMucTonToiThieu() { return dinhMucTonToiThieu; }
    public void setDinhMucTonToiThieu(int dinhMucTonToiThieu) { this.dinhMucTonToiThieu = dinhMucTonToiThieu; }
    public String getHinhAnh() { return hinhAnh; }
    public void setHinhAnh(String hinhAnh) { this.hinhAnh = hinhAnh; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
    public Integer getMaThuongHieu() { return maThuongHieu; }
    public void setMaThuongHieu(Integer maThuongHieu) { this.maThuongHieu = maThuongHieu; }
    public String getTenThuongHieu() { return tenThuongHieu; }
    public void setTenThuongHieu(String tenThuongHieu) { this.tenThuongHieu = tenThuongHieu; }
    public Integer getMaDanhMuc() { return maDanhMuc; }
    public void setMaDanhMuc(Integer maDanhMuc) { this.maDanhMuc = maDanhMuc; }
    public String getTenDanhMuc() { return tenDanhMuc; }
    public void setTenDanhMuc(String tenDanhMuc) { this.tenDanhMuc = tenDanhMuc; }
    public List<String> getGalleryUrls() { return galleryUrls; }
    public void setGalleryUrls(List<String> galleryUrls) { this.galleryUrls = galleryUrls; }
}