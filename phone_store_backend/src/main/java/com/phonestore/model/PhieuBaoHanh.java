package com.phonestore.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PhieuBaoHanh {
    private String maPhieuBaoHanh;
    private LocalDateTime ngayLapPhieu = LocalDateTime.now();
    private String tinhTrangTiepNhan;
    private String loaiKhachPhanAnh;
    private String chanDoanKyThuat;
    private String noiDungSuaChua;
    private BigDecimal chiPhiSuaChua = BigDecimal.ZERO;
    private LocalDate ngayHenTra;
    private LocalDateTime ngayTraThucTe;
    private String trangThai = "Tiep_nhan";
    private String ghiChu;
    private String maNhanVienKyThuat;
    private List<LichSuBaoHanh> lichSuList = new ArrayList<>();

    public void tiepNhanBaoHanh() {
        this.ngayLapPhieu = LocalDateTime.now();
        this.trangThai = "Da_tiep_nhan";
        themLichSu("Tiếp nhận thiết bị bảo hành");
    }

    public void capNhatBaoHanh(String chanDoan, String noiDung, BigDecimal chiPhi) {
        this.chanDoanKyThuat = chanDoan;
        this.noiDungSuaChua = noiDung;
        if (chiPhi != null) {
            this.chiPhiSuaChua = chiPhi;
        }
        themLichSu("Cập nhật chẩn đoán và nội dung sửa chữa");
    }

    public void phanCongKyThuatVien(String maKyThuatVien) {
        this.maNhanVienKyThuat = maKyThuatVien;
        themLichSu("Phân công kỹ thuật viên: " + maKyThuatVien);
    }

    public void capNhatTrangThai(String trangThaiMoi) {
        if (trangThaiMoi != null && !trangThaiMoi.isBlank()) {
            this.trangThai = trangThaiMoi.trim();
            if ("Da_tra_may".equalsIgnoreCase(this.trangThai) && this.ngayTraThucTe == null) {
                this.ngayTraThucTe = LocalDateTime.now();
            }
            themLichSu("Đổi trạng thái phiếu bảo hành sang: " + this.trangThai);
        }
    }

    public void hoanTatBaoHanh() {
        this.ngayTraThucTe = LocalDateTime.now();
        this.trangThai = "Hoan_tat";
        themLichSu("Hoàn tất bảo hành và bàn giao thiết bị");
    }

    public void inPhieuBaoHanh() {
    }

    private void themLichSu(String ghiChuLs) {
        LichSuBaoHanh ls = new LichSuBaoHanh();
        ls.setThoiGianCapNhat(LocalDateTime.now());
        ls.setTrangThaiMoi(this.trangThai);
        ls.setGhiChuTienDo(ghiChuLs);
        this.lichSuList.add(ls);
    }

    public String getMaPhieuBaoHanh() { return maPhieuBaoHanh; }
    public void setMaPhieuBaoHanh(String maPhieuBaoHanh) { this.maPhieuBaoHanh = maPhieuBaoHanh; }
    public LocalDateTime getNgayLapPhieu() { return ngayLapPhieu; }
    public void setNgayLapPhieu(LocalDateTime ngayLapPhieu) { this.ngayLapPhieu = ngayLapPhieu; }
    public String getTinhTrangTiepNhan() { return tinhTrangTiepNhan; }
    public void setTinhTrangTiepNhan(String tinhTrangTiepNhan) { this.tinhTrangTiepNhan = tinhTrangTiepNhan; }
    public String getLoaiKhachPhanAnh() { return loaiKhachPhanAnh; }
    public void setLoaiKhachPhanAnh(String loaiKhachPhanAnh) { this.loaiKhachPhanAnh = loaiKhachPhanAnh; }
    public String getChanDoanKyThuat() { return chanDoanKyThuat; }
    public void setChanDoanKyThuat(String chanDoanKyThuat) { this.chanDoanKyThuat = chanDoanKyThuat; }
    public String getNoiDungSuaChua() { return noiDungSuaChua; }
    public void setNoiDungSuaChua(String noiDungSuaChua) { this.noiDungSuaChua = noiDungSuaChua; }
    public BigDecimal getChiPhiSuaChua() { return chiPhiSuaChua; }
    public void setChiPhiSuaChua(BigDecimal chiPhiSuaChua) { this.chiPhiSuaChua = chiPhiSuaChua; }
    public LocalDate getNgayHenTra() { return ngayHenTra; }
    public void setNgayHenTra(LocalDate ngayHenTra) { this.ngayHenTra = ngayHenTra; }
    public LocalDateTime getNgayTraThucTe() { return ngayTraThucTe; }
    public void setNgayTraThucTe(LocalDateTime ngayTraThucTe) { this.ngayTraThucTe = ngayTraThucTe; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public String getMaNhanVienKyThuat() { return maNhanVienKyThuat; }
    public void setMaNhanVienKyThuat(String maNhanVienKyThuat) { this.maNhanVienKyThuat = maNhanVienKyThuat; }
    public List<LichSuBaoHanh> getLichSuList() { return lichSuList; }
    public void setLichSuList(List<LichSuBaoHanh> lichSuList) { this.lichSuList = lichSuList; }
}