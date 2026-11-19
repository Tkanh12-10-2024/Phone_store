package com.phonestore.dao;

import com.phonestore.dto.request.WarrantyCreateRequest;
import com.phonestore.dto.request.WarrantyUpdateRequest;
import com.phonestore.dto.response.WarrantyCheckResult;
import com.phonestore.dto.response.WarrantyTicketDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WarrantyDao {

    private final DataSource dataSource;

    // ========= UC 2.13.1.1 - Kiểm tra bảo hành theo IMEI =========
    public WarrantyCheckResult checkWarranty(String imei) {
        String sql = """
                SELECT im.ma_imei, im.ma_san_pham, im.trang_thai, im.ngay_ban, im.han_bao_hanh,
                       sp.ten_san_pham,
                       hd.ma_khach_hang,
                       kh.ho_ten AS ten_khach_hang, kh.so_dien_thoai
                FROM imei_san_pham im
                JOIN san_pham sp ON sp.ma_san_pham = im.ma_san_pham
                LEFT JOIN hoa_don hd ON hd.ma_hoa_don = im.ma_hoa_don
                LEFT JOIN khach_hang kh ON kh.ma_khach_hang = hd.ma_khach_hang
                WHERE im.ma_imei = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, imei.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return WarrantyCheckResult.builder()
                            .found(false)
                            .maImei(imei)
                            .lyDo("Không tìm thấy IMEI trong hệ thống")
                            .build();
                }

                String trangThai = rs.getString("trang_thai");
                LocalDate hanBaoHanh = DaoJdbcHelper.toLocalDate(rs.getDate("han_bao_hanh"));
                LocalDateTime ngayBan = DaoJdbcHelper.toLocalDateTime(rs.getTimestamp("ngay_ban"));
                LocalDate today = LocalDate.now();

                boolean conBaoHanh = false;
                long soNgayConLai = 0;
                String lyDo;

                if (!"Da_ban".equalsIgnoreCase(trangThai)) {
                    lyDo = "Thiết bị chưa được bán (trạng thái: " + trangThai + ")";
                } else if (ngayBan == null) {
                    lyDo = "Không có thông tin ngày bán";
                } else if (hanBaoHanh == null) {
                    lyDo = "Không có thông tin hạn bảo hành";
                } else if (today.isAfter(hanBaoHanh)) {
                    soNgayConLai = ChronoUnit.DAYS.between(hanBaoHanh, today);
                    lyDo = "Hết bảo hành (đã quá " + soNgayConLai + " ngày)";
                    soNgayConLai = -soNgayConLai;
                } else {
                    conBaoHanh = true;
                    soNgayConLai = ChronoUnit.DAYS.between(today, hanBaoHanh);
                    lyDo = "Còn bảo hành " + soNgayConLai + " ngày";
                }

                return WarrantyCheckResult.builder()
                        .found(true)
                        .maImei(rs.getString("ma_imei"))
                        .maSanPham(rs.getString("ma_san_pham"))
                        .tenSanPham(rs.getString("ten_san_pham"))
                        .maKhachHang(rs.getString("ma_khach_hang"))
                        .tenKhachHang(rs.getString("ten_khach_hang"))
                        .soDienThoai(rs.getString("so_dien_thoai"))
                        .ngayBan(ngayBan)
                        .hanBaoHanh(hanBaoHanh)
                        .conBaoHanh(conBaoHanh)
                        .soNgayConLai(soNgayConLai)
                        .lyDo(lyDo)
                        .trangThaiImei(trangThai)
                        .build();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra bảo hành: " + e.getMessage(), e);
        }
    }

    // ========= UC 2.13.1 - Tiếp nhận phiếu bảo hành =========
    public String createTicket(WarrantyCreateRequest req) {
        String newId = generateNewId();
        String sql = """
                INSERT INTO phieu_bao_hanh
                    (ma_phieu_bao_hanh, ma_khach_hang, ma_nhan_vien_tiep_nhan, ma_imei, ma_san_pham,
                     tinh_trang_tiep_nhan, loi_khach_phan_anh, chan_doan_ky_thuat,
                     ngay_tiep_nhan, trang_thai, ma_ky_thuat_vien)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 'Tiep_nhan', ?)
                """;
        String maSanPham = null;
        if (req.getMaImei() != null && !req.getMaImei().isBlank()) {
            maSanPham = getSanPhamFromImei(req.getMaImei());
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newId);
            ps.setString(2, req.getMaKhachHang());
            ps.setString(3, req.getMaNhanVien());
            ps.setString(4, req.getMaImei() == null || req.getMaImei().isBlank() ? null : req.getMaImei());
            ps.setString(5, maSanPham);
            ps.setString(6, req.getTinhTrangTiepNhan() != null ? req.getTinhTrangTiepNhan() : "Máy tiếp nhận bình thường");
            ps.setString(7, req.getLoiPhanAnh());
            ps.setString(8, req.getYeuCauSuaChua());
            ps.setString(9, req.getMaNhanVien());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tạo phiếu bảo hành: " + e.getMessage(), e);
        }
        return newId;
    }

    // ========= UC 2.13.2 - Danh sách phiếu bảo hành =========
    public List<WarrantyTicketDTO> findAll(String keyword, String trangThai) {
        StringBuilder sql = new StringBuilder("""
                SELECT pbh.*,
                       kh.ho_ten AS ten_khach_hang, kh.so_dien_thoai,
                       sp.ten_san_pham,
                       nv1.ho_ten AS ten_nv_tiep_nhan,
                       nv2.ho_ten AS ten_nv_cap_nhat
                FROM phieu_bao_hanh pbh
                JOIN khach_hang kh ON kh.ma_khach_hang = pbh.ma_khach_hang
                LEFT JOIN san_pham sp ON sp.ma_san_pham = pbh.ma_san_pham
                LEFT JOIN nhan_vien nv1 ON nv1.ma_nhan_vien = pbh.ma_nhan_vien_tiep_nhan
                LEFT JOIN nhan_vien nv2 ON nv2.ma_nhan_vien = pbh.ma_ky_thuat_vien
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();

        if (trangThai != null && !trangThai.isBlank() && !"Tat_ca".equalsIgnoreCase(trangThai)) {
            sql.append(" AND pbh.trang_thai = ?");
            params.add(trangThai);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(kh.ho_ten) LIKE ? OR kh.so_dien_thoai LIKE ? OR pbh.ma_phieu_bao_hanh LIKE ? OR pbh.ma_imei LIKE ?)");
            String p = "%" + keyword.trim().toLowerCase() + "%";
            params.add(p);
            params.add(p);
            params.add(p);
            params.add(p);
        }
        sql.append(" ORDER BY pbh.ngay_tiep_nhan DESC");

        List<WarrantyTicketDTO> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoJdbcHelper.bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapTicket(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách bảo hành: " + e.getMessage(), e);
        }
        return result;
    }

    // ========= UC 2.13.2 - Chi tiết phiếu =========
    public Optional<WarrantyTicketDTO> findById(String maPhieuBh) {
        String sql = """
                SELECT pbh.*,
                       kh.ho_ten AS ten_khach_hang, kh.so_dien_thoai,
                       sp.ten_san_pham,
                       nv1.ho_ten AS ten_nv_tiep_nhan,
                       nv2.ho_ten AS ten_nv_cap_nhat
                FROM phieu_bao_hanh pbh
                JOIN khach_hang kh ON kh.ma_khach_hang = pbh.ma_khach_hang
                LEFT JOIN san_pham sp ON sp.ma_san_pham = pbh.ma_san_pham
                LEFT JOIN nhan_vien nv1 ON nv1.ma_nhan_vien = pbh.ma_nhan_vien_tiep_nhan
                LEFT JOIN nhan_vien nv2 ON nv2.ma_nhan_vien = pbh.ma_ky_thuat_vien
                WHERE pbh.ma_phieu_bao_hanh = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maPhieuBh);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapTicket(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy chi tiết phiếu bảo hành: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ========= UC 2.13.3 - Cập nhật tiến độ =========
    public void updateProgress(String maPhieuBh, WarrantyUpdateRequest req) {
        validateStatusTransition(maPhieuBh, req.getTrangThaiMoi());

        String sql = """
                UPDATE phieu_bao_hanh SET
                    chan_doan_ky_thuat = ?,
                    noi_dung_sua_chua = ?,
                    ghi_chu = ?,
                    trang_thai = ?,
                    ma_ky_thuat_vien = ?
                WHERE ma_phieu_bao_hanh = ? AND trang_thai <> 'Hoan_tat'
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String chanDoan = req.getKetQuaKiemTra() != null ? req.getKetQuaKiemTra() : "";
            if (req.getNguyenNhanLoi() != null && !req.getNguyenNhanLoi().isBlank()) {
                chanDoan = chanDoan + (chanDoan.isBlank() ? "" : " | Nguyên nhân: ") + req.getNguyenNhanLoi();
            }
            ps.setString(1, chanDoan);
            ps.setString(2, req.getNoiDungSuaChua());
            ps.setString(3, req.getGhiChu());
            ps.setString(4, req.getTrangThaiMoi());
            ps.setString(5, req.getMaNhanVien());
            ps.setString(6, maPhieuBh);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new RuntimeException("Phiếu đã hoàn tất hoặc không tồn tại.");
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật tiến độ: " + e.getMessage(), e);
        }
    }

    // ========= UC 2.13.4 - Hoàn tất và đóng phiếu =========
    public void closeTicket(String maPhieuBh, String maNhanVien) {
        String checkSql = "SELECT trang_thai FROM phieu_bao_hanh WHERE ma_phieu_bao_hanh = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, maPhieuBh);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new RuntimeException("Không tìm thấy phiếu bảo hành: " + maPhieuBh);
                String trangThai = rs.getString("trang_thai");
                if (!"Cho_tra_khach".equals(trangThai)) {
                    throw new RuntimeException("Chỉ có thể hoàn tất phiếu khi trạng thái là 'Chờ trả khách'. Trạng thái hiện tại: " + trangThai);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra trạng thái: " + e.getMessage(), e);
        }

        String updateSql = """
                UPDATE phieu_bao_hanh SET
                    trang_thai = 'Hoan_tat',
                    ma_ky_thuat_vien = ?,
                    ngay_tra_thuc_te = CURRENT_TIMESTAMP
                WHERE ma_phieu_bao_hanh = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, maNhanVien);
            ps.setString(2, maPhieuBh);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi hoàn tất phiếu bảo hành: " + e.getMessage(), e);
        }
    }

    // ========= Helpers =========

    private void validateStatusTransition(String maPhieuBh, String newStatus) {
        List<String> validStatuses = List.of("Tiep_nhan", "Dang_kiem_tra", "Dang_sua_chua", "Cho_tra_khach", "Tu_choi", "Tu_choi_bao_hanh");
        if (!validStatuses.contains(newStatus)) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + newStatus);
        }
    }

    private String getSanPhamFromImei(String maImei) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT ma_san_pham FROM imei_san_pham WHERE ma_imei = ?")) {
            ps.setString(1, maImei);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("ma_san_pham");
            }
        } catch (SQLException e) {
            // ignore
        }
        return null;
    }

    private String generateNewId() {
        String prefix = "PBH" + LocalDateTime.now().getYear()
                + String.format("%02d", LocalDateTime.now().getMonthValue())
                + String.format("%02d", LocalDateTime.now().getDayOfMonth());
        String sql = "SELECT ma_phieu_bao_hanh FROM phieu_bao_hanh WHERE ma_phieu_bao_hanh LIKE ? ORDER BY ma_phieu_bao_hanh DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String last = rs.getString(1); // PBH20261118_01
                    String[] parts = last.split("_");
                    int num = Integer.parseInt(parts[parts.length - 1]) + 1;
                    return prefix + "_" + String.format("%02d", num);
                }
            }
        } catch (SQLException e) {
            // ignore
        }
        return prefix + "_01";
    }

    private WarrantyTicketDTO mapTicket(ResultSet rs) throws SQLException {
        String tenThietBi = rs.getString("ten_san_pham");
        if (tenThietBi == null || tenThietBi.isBlank()) {
            tenThietBi = rs.getString("ma_san_pham");
        }
        return WarrantyTicketDTO.builder()
                .maPhieuBh(rs.getString("ma_phieu_bao_hanh"))
                .maKhachHang(rs.getString("ma_khach_hang"))
                .tenKhachHang(rs.getString("ten_khach_hang"))
                .soDienThoai(rs.getString("so_dien_thoai"))
                .maNhanVienTiepNhan(rs.getString("ma_nhan_vien_tiep_nhan"))
                .tenNhanVienTiepNhan(rs.getString("ten_nv_tiep_nhan"))
                .maImei(rs.getString("ma_imei"))
                .maSanPham(rs.getString("ma_san_pham"))
                .tenThietBi(tenThietBi)
                .tinhTrangTiepNhan(rs.getString("tinh_trang_tiep_nhan"))
                .loiPhanAnh(rs.getString("loi_khach_phan_anh"))
                .yeuCauSuaChua(rs.getString("chan_doan_ky_thuat"))
                .ketQuaKiemTra(rs.getString("chan_doan_ky_thuat"))
                .nguyenNhanLoi(rs.getString("chan_doan_ky_thuat"))
                .noiDungSuaChua(rs.getString("noi_dung_sua_chua"))
                .ghiChu(rs.getString("ghi_chu"))
                .ngayTiepNhan(DaoJdbcHelper.toLocalDateTime(rs.getTimestamp("ngay_tiep_nhan")))
                .ngayBanGiao(DaoJdbcHelper.toLocalDateTime(rs.getTimestamp("ngay_tra_thuc_te")))
                .trangThai(rs.getString("trang_thai"))
                .maNhanVienCapNhat(rs.getString("ma_ky_thuat_vien"))
                .tenNhanVienCapNhat(rs.getString("ten_nv_cap_nhat"))
                .build();
    }
}
