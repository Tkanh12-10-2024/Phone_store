
package com.phonestore.dao;

import com.phonestore.dto.request.PromotionRequest;
import com.phonestore.dto.response.OptionItemDTO;
import com.phonestore.dto.response.PromotionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PromotionDao {

    private static final String BASE_SELECT = """
            SELECT km.ma_khuyen_mai,
                   km.code_voucher,
                   km.ten_khuyen_mai,
                   km.loai_giam_gia,
                   km.gia_tri_giam,
                   km.gia_tri_don_hang_toi_thieu,
                   km.giam_toi_da,
                   km.so_luong_phat_hanh,
                   km.so_luong_da_dung,
                   km.ngay_bat_dau,
                   km.ngay_ket_thuc,
                   km.ma_thuong_hieu_ap_dung,
                   th.ten_thuong_hieu,
                   km.trang_thai
            FROM khuyen_mai km
            LEFT JOIN thuong_hieu th
                ON km.ma_thuong_hieu_ap_dung = th.ma_thuong_hieu
            """;

    private final DataSource dataSource;

    // Lấy danh sách khuyến mãi
    public List<PromotionResponse> findAll(
            String keyword,
            String trangThai,
            String loaiGiamGia) {

        StringBuilder sql = new StringBuilder(BASE_SELECT)
                .append(" WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                     AND (
                         LOWER(km.code_voucher) LIKE ?
                         OR LOWER(km.ten_khuyen_mai) LIKE ?
                     )
                    """);

            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
        }

        if (trangThai != null
                && !trangThai.isBlank()
                && !"ALL".equalsIgnoreCase(trangThai)) {

            sql.append(" AND km.trang_thai = ?");
            params.add(trangThai.trim());
        }

        if (loaiGiamGia != null
                && !loaiGiamGia.isBlank()
                && !"ALL".equalsIgnoreCase(loaiGiamGia)) {

            sql.append(" AND km.loai_giam_gia = ?");
            params.add(loaiGiamGia.trim());
        }

        sql.append(" ORDER BY km.ngay_ket_thuc ASC");

        List<PromotionResponse> list = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            DaoJdbcHelper.bindParams(ps, params);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Lỗi truy vấn danh sách khuyến mãi: " + e.getMessage(), e);
        }

        return list;
    }

    // Tìm khuyến mãi theo mã
    public Optional<PromotionResponse> findById(String maKhuyenMai) {
        String sql = BASE_SELECT + " WHERE km.ma_khuyen_mai = ?";
        return findSingle(sql, maKhuyenMai);
    }

    // Tìm khuyến mãi theo mã voucher
    public Optional<PromotionResponse> findByCodeVoucher(String codeVoucher) {
        String sql = BASE_SELECT
                + " WHERE UPPER(km.code_voucher) = UPPER(?)";

        return findSingle(sql, codeVoucher);
    }

    private Optional<PromotionResponse> findSingle(
            String sql,
            String param) {

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, param);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Lỗi truy vấn khuyến mãi theo mã: " + e.getMessage(), e);
        }

        return Optional.empty();
    }

    // Kiểm tra mã voucher đã tồn tại
    public boolean existsByCodeVoucher(
            String codeVoucher,
            String excludeMaKhuyenMai) {

        String sql = excludeMaKhuyenMai == null
                || excludeMaKhuyenMai.isBlank()
                ? """
                    SELECT COUNT(*)
                    FROM khuyen_mai
                    WHERE UPPER(code_voucher) = UPPER(?)
                    """
                : """
                    SELECT COUNT(*)
                    FROM khuyen_mai
                    WHERE UPPER(code_voucher) = UPPER(?)
                    AND ma_khuyen_mai <> ?
                    """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, codeVoucher.trim());

            if (excludeMaKhuyenMai != null
                    && !excludeMaKhuyenMai.isBlank()) {
                ps.setString(2, excludeMaKhuyenMai.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Lỗi kiểm tra trùng mã voucher: " + e.getMessage(), e);
        }
    }

    // Thêm khuyến mãi
    public PromotionResponse insert(
            PromotionRequest req,
            String maKhuyenMai) {

        String sql = """
                INSERT INTO khuyen_mai (
                    ma_khuyen_mai,
                    code_voucher,
                    ten_khuyen_mai,
                    loai_giam_gia,
                    gia_tri_giam,
                    gia_tri_don_hang_toi_thieu,
                    giam_toi_da,
                    so_luong_phat_hanh,
                    so_luong_da_dung,
                    ngay_bat_dau,
                    ngay_ket_thuc,
                    ma_thuong_hieu_ap_dung,
                    trang_thai
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, maKhuyenMai);
            bindPromotionFields(ps, 2, req);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Không thể tạo khuyến mãi mới: " + e.getMessage(), e);
        }

        return findById(maKhuyenMai).orElseThrow();
    }

    // Cập nhật khuyến mãi
    public PromotionResponse update(
            String maKhuyenMai,
            PromotionRequest req) {

        String sql = """
                UPDATE khuyen_mai SET
                    code_voucher = ?,
                    ten_khuyen_mai = ?,
                    loai_giam_gia = ?,
                    gia_tri_giam = ?,
                    gia_tri_don_hang_toi_thieu = ?,
                    giam_toi_da = ?,
                    so_luong_phat_hanh = ?,
                    ngay_bat_dau = ?,
                    ngay_ket_thuc = ?,
                    ma_thuong_hieu_ap_dung = ?,
                    trang_thai = ?
                WHERE ma_khuyen_mai = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int nextIdx = bindPromotionFields(ps, 1, req);
            ps.setString(nextIdx, maKhuyenMai);

            int affected = ps.executeUpdate();

            if (affected == 0) {
                throw new IllegalArgumentException(
                        "Không tìm thấy chương trình khuyến mãi: "
                                + maKhuyenMai);
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Không thể cập nhật khuyến mãi: " + e.getMessage(), e);
        }

        return findById(maKhuyenMai).orElseThrow();
    }

    // Cập nhật trạng thái
    public boolean updateStatus(
            String maKhuyenMai,
            String trangThai) {

        String sql = """
                UPDATE khuyen_mai
                SET trang_thai = ?
                WHERE ma_khuyen_mai = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, trangThai);
            ps.setString(2, maKhuyenMai);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Không thể cập nhật trạng thái khuyến mãi: "
                            + e.getMessage(), e);
        }
    }

    // Kiểm tra khuyến mãi đã được sử dụng trong đơn hàng
    public boolean isUsedInOrders(String maKhuyenMai) {

        String sql = """
                SELECT COUNT(*)
                FROM don_hang
                WHERE ma_khuyen_mai = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, maKhuyenMai);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Lỗi kiểm tra lịch sử sử dụng khuyến mãi: "
                            + e.getMessage(), e);
        }
    }

    // Xóa khuyến mãi
    public boolean deleteById(String maKhuyenMai) {

        String sql = """
                DELETE FROM khuyen_mai
                WHERE ma_khuyen_mai = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, maKhuyenMai);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Không thể xóa khuyến mãi đang phát sinh giao dịch: "
                            + e.getMessage(), e);
        }
    }

    // Tăng số lượng voucher đã sử dụng
    public boolean incrementUsedCountIfAvailable(
            Connection conn,
            String maKhuyenMai) throws SQLException {

        String sql = """
                UPDATE khuyen_mai
                SET so_luong_da_dung = so_luong_da_dung + 1
                WHERE ma_khuyen_mai = ?
                AND so_luong_da_dung < so_luong_phat_hanh
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maKhuyenMai);
            return ps.executeUpdate() > 0;
        }
    }

    // Lấy danh sách thương hiệu
    public List<OptionItemDTO> findBrandOptions() {
        String sql = """
                SELECT ma_thuong_hieu, ten_thuong_hieu
                FROM thuong_hieu
                ORDER BY ten_thuong_hieu
                """;

        return queryOptions(sql);
    }

    // Lấy danh sách danh mục
    public List<OptionItemDTO> findCategoryOptions() {
        String sql = """
                SELECT ma_danh_muc, ten_danh_muc
                FROM danh_muc
                ORDER BY ten_danh_muc
                """;

        return queryOptions(sql);
    }

    // Lấy danh sách dòng máy
    public List<String> findProductLineOptions() {

        String sql = """
                SELECT DISTINCT dong_may
                FROM san_pham
                WHERE dong_may IS NOT NULL
                ORDER BY dong_may
                """;

        List<String> lines = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lines.add(rs.getString(1));
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Lỗi lấy danh sách dòng máy: " + e.getMessage(), e);
        }

        return lines;
    }

    private List<OptionItemDTO> queryOptions(String sql) {

        List<OptionItemDTO> options = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                options.add(new OptionItemDTO(
                        String.valueOf(rs.getInt(1)),
                        rs.getString(2)));
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Lỗi truy vấn danh mục lựa chọn: " + e.getMessage(), e);
        }

        return options;
    }

    // Gán dữ liệu cho câu lệnh thêm và cập nhật
    private int bindPromotionFields(
            PreparedStatement ps,
            int startIdx,
            PromotionRequest req) throws SQLException {

        int i = startIdx;

        ps.setString(i++, req.getCodeVoucher().trim().toUpperCase());
        ps.setString(i++, req.getTenKhuyenMai().trim());
        ps.setString(i++, req.getLoaiGiamGia());

        ps.setBigDecimal(
                i++,
                DaoJdbcHelper.zeroIfNull(req.getGiaTriGiam()));

        ps.setBigDecimal(
                i++,
                DaoJdbcHelper.zeroIfNull(req.getGiaTriDonHangToiThieu()));

        if (req.getGiamToiDa() != null) {
            ps.setBigDecimal(i++, req.getGiamToiDa());
        } else {
            ps.setNull(i++, Types.DECIMAL);
        }

        ps.setInt(
                i++,
                req.getSoLuongPhatHanh() == null
                        ? 100
                        : req.getSoLuongPhatHanh());

        ps.setTimestamp(i++, Timestamp.valueOf(req.getNgayBatDau()));
        ps.setTimestamp(i++, Timestamp.valueOf(req.getNgayKetThuc()));

        if (req.getMaThuongHieuApDung() != null) {
            ps.setInt(i++, req.getMaThuongHieuApDung());
        } else {
            ps.setNull(i++, Types.INTEGER);
        }

        ps.setString(
                i++,
                blankToDefault(req.getTrangThai(), "Dang_dien_ra"));

        return i;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank()
                ? defaultValue
                : value.trim();
    }

    // Chuyển dữ liệu từ ResultSet thành PromotionResponse
    private PromotionResponse mapRow(ResultSet rs) throws SQLException {

        int phatHanh = rs.getInt("so_luong_phat_hanh");
        int daDung = rs.getInt("so_luong_da_dung");
        int conLai = Math.max(0, phatHanh - daDung);

        BigDecimal tiLeConLai = phatHanh > 0
                ? BigDecimal.valueOf(conLai)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(
                            BigDecimal.valueOf(phatHanh),
                            2,
                            RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime ngayBatDau =
                DaoJdbcHelper.toLocalDateTime(
                        rs.getTimestamp("ngay_bat_dau"));

        LocalDateTime ngayKetThuc =
                DaoJdbcHelper.toLocalDateTime(
                        rs.getTimestamp("ngay_ket_thuc"));

        long gioConLai = ngayKetThuc != null
                ? Duration.between(now, ngayKetThuc).toHours()
                : 0L;

        String trangThai = rs.getString("trang_thai");

        boolean active = "Dang_dien_ra".equalsIgnoreCase(trangThai);

        boolean sapHetHan = active
                && ngayKetThuc != null
                && ngayKetThuc.isAfter(now)
                && Duration.between(now, ngayKetThuc).toMinutes()
                        < 24 * 60L;

        boolean sapHetSoLuong = active
                && phatHanh > 0
                && tiLeConLai.compareTo(BigDecimal.valueOf(5)) < 0;

        List<String> reasons = new ArrayList<>();

        if (sapHetHan) {
            long minsLeft = Math.max(
                    1,
                    Duration.between(now, ngayKetThuc).toMinutes());

            long hrsLeft = minsLeft / 60;

            reasons.add(
                    hrsLeft > 0
                            ? "Sắp hết hạn (" + hrsLeft + " giờ còn lại)"
                            : "Sắp hết hạn (" + minsLeft + " phút còn lại)");
        }

        if (sapHetSoLuong) {
            reasons.add(
                    "Sắp hết số lượng (còn "
                            + conLai + "/" + phatHanh
                            + " = " + tiLeConLai + "%)");
        }

        Integer brandId =
                rs.getObject("ma_thuong_hieu_ap_dung") != null
                        ? rs.getInt("ma_thuong_hieu_ap_dung")
                        : null;

        return PromotionResponse.builder()
                .maKhuyenMai(rs.getString("ma_khuyen_mai"))
                .codeVoucher(rs.getString("code_voucher"))
                .tenKhuyenMai(rs.getString("ten_khuyen_mai"))
                .loaiGiamGia(rs.getString("loai_giam_gia"))
                .giaTriGiam(
                        DaoJdbcHelper.zeroIfNull(
                                rs.getBigDecimal("gia_tri_giam")))
                .giaTriDonHangToiThieu(
                        DaoJdbcHelper.zeroIfNull(
                                rs.getBigDecimal("gia_tri_don_hang_toi_thieu")))
                .giamToiDa(rs.getBigDecimal("giam_toi_da"))
                .quaTang(null)
                .soLuongPhatHanh(phatHanh)
                .soLuongDaDung(daDung)
                .soLuongConLai(conLai)
                .tiLeConLai(tiLeConLai)
                .ngayBatDau(ngayBatDau)
                .ngayKetThuc(ngayKetThuc)
                .gioConLai(gioConLai)
                .maThuongHieuApDung(brandId)
                .tenThuongHieu(rs.getString("ten_thuong_hieu"))
                .dongMayApDung(null)
                .maDanhMucApDung(null)
                .tenDanhMuc(null)
                .doiTuongKhachHang(null)
                .moTa(null)
                .trangThai(trangThai)
                .ngayTao(null)
                .sapHetHan(sapHetHan)
                .sapHetSoLuong(sapHetSoLuong)
                .coCanhBao(sapHetHan || sapHetSoLuong)
                .lyDoCanhBao(String.join(" | ", reasons))
                .build();
    }
}
