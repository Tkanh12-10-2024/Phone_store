package com.phonestore.dao;

import com.phonestore.dto.response.ReportLogDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReportDao {

    private final DataSource dataSource;

    public List<ImportExportRow> findImportExportSummary(LocalDate tuNgay, LocalDate denNgay, Integer maThuongHieu) {
        Timestamp startTs = Timestamp.valueOf(tuNgay.atStartOfDay());
        Timestamp endTs = Timestamp.valueOf(denNgay.atTime(LocalTime.MAX));

        StringBuilder sql = new StringBuilder("""
                SELECT sp.ma_san_pham, sp.ten_san_pham, th.ten_thuong_hieu,
                       sp.so_luong_ton, sp.dinh_muc_ton_toi_thieu, sp.gia_nhap_tham_khao, sp.gia_ban,
                       COALESCE(imp.qty_in, 0) AS nhap_trong_ky,
                       COALESCE(exp.qty_out, 0) AS xuat_trong_ky,
                       COALESCE(exp.rev_out, 0) AS doanh_thu_trong_ky
                FROM san_pham sp
                JOIN thuong_hieu th ON sp.ma_thuong_hieu = th.ma_thuong_hieu
                LEFT JOIN (
                    SELECT ctpn.ma_san_pham, SUM(ctpn.so_luong) AS qty_in
                    FROM chi_tiet_phieu_nhap ctpn
                    JOIN phieu_nhap pn ON ctpn.ma_phieu_nhap = pn.ma_phieu_nhap
                    WHERE pn.trang_thai = 'Hoan_thanh' AND pn.ngay_nhap BETWEEN ? AND ?
                    GROUP BY ctpn.ma_san_pham
                ) imp ON sp.ma_san_pham = imp.ma_san_pham
                LEFT JOIN (
                    SELECT cthd.ma_san_pham, COUNT(cthd.ma_chi_tiet_hd) AS qty_out, COALESCE(SUM(cthd.don_gia), 0) AS rev_out
                    FROM chi_tiet_hoa_don cthd
                    JOIN hoa_don hd ON cthd.ma_hoa_don = hd.ma_hoa_don
                    WHERE hd.trang_thai_thanh_toan = 'Thanh_cong' AND hd.ngay_lap BETWEEN ? AND ?
                    GROUP BY cthd.ma_san_pham
                ) exp ON sp.ma_san_pham = exp.ma_san_pham
                WHERE sp.trang_thai = 'Dang_kinh_doanh'
                """);
        List<Object> params = new ArrayList<>();
        params.add(startTs);
        params.add(endTs);
        params.add(startTs);
        params.add(endTs);
        if (maThuongHieu != null) {
            sql.append(" AND sp.ma_thuong_hieu = ?");
            params.add(maThuongHieu);
        }
        sql.append(" ORDER BY xuat_trong_ky DESC, sp.ten_san_pham ASC");

        List<ImportExportRow> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoJdbcHelper.bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ImportExportRow(
                            rs.getString("ma_san_pham"),
                            rs.getString("ten_san_pham"),
                            rs.getString("ten_thuong_hieu"),
                            rs.getInt("nhap_trong_ky"),
                            rs.getInt("xuat_trong_ky"),
                            rs.getInt("so_luong_ton"),
                            rs.getInt("dinh_muc_ton_toi_thieu"),
                            DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("gia_nhap_tham_khao")),
                            DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("doanh_thu_trong_ky"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tổng hợp xuất - nhập - tồn kho", e);
        }
        return rows;
    }

    public List<WarrantyRow> findWarrantySummary(LocalDate tuNgay, LocalDate denNgay) {
        Timestamp startTs = Timestamp.valueOf(tuNgay.atStartOfDay());
        Timestamp endTs = Timestamp.valueOf(denNgay.atTime(LocalTime.MAX));
        String sql = """
                SELECT pbh.ma_phieu_bao_hanh, sp.ten_san_pham, pbh.ma_imei, kh.ho_ten AS ten_khach_hang,
                       pbh.ngay_tiep_nhan, pbh.loi_khach_phan_anh, pbh.chi_phi_sua_chua, pbh.trang_thai
                FROM phieu_bao_hanh pbh
                JOIN san_pham sp ON pbh.ma_san_pham = sp.ma_san_pham
                JOIN khach_hang kh ON pbh.ma_khach_hang = kh.ma_khach_hang
                WHERE pbh.ngay_tiep_nhan BETWEEN ? AND ?
                ORDER BY pbh.ngay_tiep_nhan DESC
                """;
        List<WarrantyRow> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, startTs);
            ps.setTimestamp(2, endTs);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new WarrantyRow(
                            rs.getString("ma_phieu_bao_hanh"),
                            rs.getString("ten_san_pham"),
                            rs.getString("ma_imei"),
                            rs.getString("ten_khach_hang"),
                            DaoJdbcHelper.toLocalDateTime(rs.getTimestamp("ngay_tiep_nhan")),
                            rs.getString("loi_khach_phan_anh"),
                            DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("chi_phi_sua_chua")),
                            rs.getString("trang_thai")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tổng hợp báo cáo bảo hành", e);
        }
        return rows;
    }

    public String findEmployeeName(String maNhanVien) {
        if (maNhanVien == null || maNhanVien.isBlank()) {
            return "Quản trị viên";
        }
        String sql = "SELECT ho_ten FROM nhan_vien WHERE ma_nhan_vien = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maNhanVien.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException ignored) {
        }
        return maNhanVien;
    }

    public ReportLogDTO insertReportLog(ReportLogDTO log) {
        String sql = """
                INSERT INTO nhat_ky_bao_cao (
                    loai_bao_cao, ma_nhan_vien_lap, ngay_lap, tu_ngay, den_ngay,
                    dinh_dang_xuat, duong_dan_file, tieu_chi_loc, ghi_chu
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime now = LocalDateTime.now();
        String staffId = (log.getMaNhanVienLap() == null || log.getMaNhanVienLap().isBlank()) ? "NV001" : log.getMaNhanVienLap().trim();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, log.getLoaiBaoCao());
            ps.setString(2, staffId);
            ps.setTimestamp(3, Timestamp.valueOf(now));
            ps.setDate(4, log.getTuNgay() != null ? Date.valueOf(log.getTuNgay()) : null);
            ps.setDate(5, log.getDenNgay() != null ? Date.valueOf(log.getDenNgay()) : null);
            ps.setString(6, log.getDinhDangXuat());
            ps.setString(7, log.getDuongDanFile());
            ps.setString(8, log.getTieuChiLoc());
            ps.setString(9, log.getGhiChu());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    log.setMaBaoCao(keys.getInt(1));
                }
            }
            log.setNgayLap(now);
            log.setMaNhanVienLap(staffId);
            log.setTenNhanVienLap(findEmployeeName(staffId));
            return log;
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể ghi nhật ký xuất báo cáo", e);
        }
    }

    public List<ReportLogDTO> findReportLogs(int limit) {
        String sql = """
                SELECT nk.ma_bao_cao, nk.loai_bao_cao, nk.ma_nhan_vien_lap,
                       COALESCE(nv.ho_ten, nk.ma_nhan_vien_lap) AS ten_nhan_vien_lap,
                       nk.ngay_lap, nk.tu_ngay, nk.den_ngay,
                       nk.dinh_dang_xuat, nk.duong_dan_file, nk.tieu_chi_loc, nk.ghi_chu
                FROM nhat_ky_bao_cao nk
                LEFT JOIN nhan_vien nv ON nk.ma_nhan_vien_lap = nv.ma_nhan_vien
                ORDER BY nk.ngay_lap DESC, nk.ma_bao_cao DESC
                LIMIT ?
                """;
        List<ReportLogDTO> logs = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(ReportLogDTO.builder()
                            .maBaoCao(rs.getInt("ma_bao_cao"))
                            .loaiBaoCao(rs.getString("loai_bao_cao"))
                            .maNhanVienLap(rs.getString("ma_nhan_vien_lap"))
                            .tenNhanVienLap(rs.getString("ten_nhan_vien_lap"))
                            .ngayLap(DaoJdbcHelper.toLocalDateTime(rs.getTimestamp("ngay_lap")))
                            .tuNgay(DaoJdbcHelper.toLocalDate(rs.getDate("tu_ngay")))
                            .denNgay(DaoJdbcHelper.toLocalDate(rs.getDate("den_ngay")))
                            .dinhDangXuat(rs.getString("dinh_dang_xuat"))
                            .duongDanFile(rs.getString("duong_dan_file"))
                            .tieuChiLoc(rs.getString("tieu_chi_loc"))
                            .ghiChu(rs.getString("ghi_chu"))
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tải nhật ký báo cáo", e);
        }
        return logs;
    }

    public record ImportExportRow(
            String maSanPham,
            String tenSanPham,
            String tenThuongHieu,
            int nhapTrongKy,
            int xuatTrongKy,
            int tonCuoiKy,
            int dinhMucToiThieu,
            BigDecimal giaNhap,
            BigDecimal doanhThuTrongKy
    ) {
    }

    public record WarrantyRow(
            String maPhieuBaoHanh,
            String tenSanPham,
            String maImei,
            String tenKhachHang,
            LocalDateTime ngayTiepNhan,
            String loiKhachPhanAnh,
            BigDecimal chiPhiSuaChua,
            String trangThai
    ) {
    }
}

