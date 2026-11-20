package com.phonestore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResponse {
    private String maKhuyenMai;
    private String codeVoucher;
    private String tenKhuyenMai;
    private String loaiGiamGia;
    private BigDecimal giaTriGiam;
    private BigDecimal giaTriDonHangToiThieu;
    private BigDecimal giamToiDa;
    private String quaTang;
    private Integer soLuongPhatHanh;
    private Integer soLuongDaDung;
    private Integer soLuongConLai;
    private BigDecimal tiLeConLai;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private Long gioConLai;
    private Integer maThuongHieuApDung;
    private String tenThuongHieu;
    private String dongMayApDung;
    private Integer maDanhMucApDung;
    private String tenDanhMuc;
    private String doiTuongKhachHang;
    private String moTa;
    private String trangThai;
    private LocalDateTime ngayTao;
    private boolean sapHetHan;
    private boolean sapHetSoLuong;
    private boolean coCanhBao;
    private String lyDoCanhBao;
}

