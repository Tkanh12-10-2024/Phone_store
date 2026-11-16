package com.phonestore.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PromotionRequest {
    private String maKhuyenMai;
    private String codeVoucher;
    private String tenKhuyenMai;
    private String loaiGiamGia; // Phan_tram, Tien_mat, Qua_tang
    private BigDecimal giaTriGiam;
    private BigDecimal giaTriDonHangToiThieu;
    private BigDecimal giamToiDa;
    private String quaTang;
    private Integer soLuongPhatHanh;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private Integer maThuongHieuApDung;
    private String dongMayApDung;
    private Integer maDanhMucApDung;
    private String doiTuongKhachHang; // Tat_ca, Khach_moi, Thanh_vien, Khach_VIP
    private String moTa;
    private String trangThai; // Dang_dien_ra, Tam_dung, Het_han
}

