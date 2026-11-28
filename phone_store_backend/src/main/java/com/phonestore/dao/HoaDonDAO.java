
package com.phonestore.dao;

import com.phonestore.model.ChiTietHoaDon;
import com.phonestore.model.HoaDon;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class HoaDonDAO {

    private final JdbcTemplate jdbcTemplate;

    public HoaDonDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
     * Sinh mã hóa đơn duy nhất.
     * Ví dụ: HD20261128103315A1B2C3
     */
    public String sinhMaHoaDonMoi() {
        String thoiGian = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        String ngauNhien = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();

        return "HD" + thoiGian + ngauNhien;
    }

    /*
     * Sinh mã đơn hàng từ mã hóa đơn.
     */
    private String sinhMaDonHang(String maHoaDon) {
        return "DH" + maHoaDon.substring(2);
    }

    /*
     * Kiểm tra khách hàng có tồn tại hay không.
     * Khách vãng lai được lưu ma_khach_hang = NULL.
     */
    private String xuLyMaKhachHang(String maKhachHang) {

        if (maKhachHang == null
                || maKhachHang.isBlank()
                || "KH_LE".equalsIgnoreCase(maKhachHang.trim())) {
            return null;
        }

        String sql = """
            SELECT COUNT(*)
            FROM khach_hang
            WHERE ma_khach_hang = ?
        """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                maKhachHang.trim()
        );

        if (count == null || count == 0) {
            throw new IllegalArgumentException(
                    "Không tìm thấy khách hàng: " + maKhachHang
            );
        }

        return maKhachHang.trim();
    }

    /*
     * Kiểm tra nhân viên tồn tại.
     * Bảng hoa_don và don_hang đều yêu cầu ma_nhan_vien.
     */
    private void kiemTraNhanVien(String maNhanVien) {

        if (maNhanVien == null || maNhanVien.isBlank()) {
            throw new IllegalArgumentException(
                    "Mã nhân viên không được để trống."
            );
        }

        String sql = """
            SELECT COUNT(*)
            FROM nhan_vien
            WHERE ma_nhan_vien = ?
        """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                maNhanVien.trim()
        );

        if (count == null || count == 0) {
            throw new IllegalArgumentException(
                    "Không tìm thấy nhân viên: " + maNhanVien
            );
        }
    }

    /*
     * Tìm mã khuyến mãi từ code voucher.
     * Không sử dụng voucher thì trả về NULL.
     */
    private String timMaKhuyenMai(String codeVoucher) {

        if (codeVoucher == null || codeVoucher.isBlank()) {
            return null;
        }

        String sql = """
            SELECT ma_khuyen_mai
            FROM khuyen_mai
            WHERE LOWER(code_voucher) = LOWER(?)
            LIMIT 1
        """;

        List<String> result = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getString("ma_khuyen_mai"),
                codeVoucher.trim()
        );

        return result.isEmpty() ? null : result.get(0);
    }

    /*
     * Kiểm tra phương thức thanh toán có hợp lệ theo ENUM SQL.
     */
    private void kiemTraPhuongThucThanhToan(String phuongThuc) {

        Set<String> hopLe = Set.of(
                "Tien_mat",
                "Chuyen_khoan_QR",
                "The"
        );

        if (phuongThuc == null || !hopLe.contains(phuongThuc)) {
            throw new IllegalArgumentException(
                    "Phương thức thanh toán không hợp lệ: " + phuongThuc
            );
        }
    }

    /*
     * Lấy và khóa IMEI còn trong kho theo mã sản phẩm.
     *
     * FOR UPDATE giúp tránh hai giao dịch cùng lấy một IMEI.
     * Phải gọi bên trong transaction.
     */
    private List<String> layVaKhoaImei(
            String maSanPham,
            int soLuong
    ) {

        if (maSanPham == null || maSanPham.isBlank()) {
            throw new IllegalArgumentException(
                    "Mã sản phẩm không được để trống."
            );
        }

        if (soLuong <= 0) {
            throw new IllegalArgumentException(
                    "Số lượng sản phẩm phải lớn hơn 0."
            );
        }

        String sql = """
            SELECT ma_imei
            FROM imei_san_pham
            WHERE ma_san_pham = ?
              AND trang_thai = 'Trong_kho'
            ORDER BY ngay_nhap ASC, ma_imei ASC
            LIMIT ?
            FOR UPDATE
        """;

        List<String> imeiList = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getString("ma_imei"),
                maSanPham,
                soLuong
        );

        if (imeiList.size() < soLuong) {
            throw new IllegalStateException(
                    "Sản phẩm " + maSanPham
                            + " không đủ IMEI trong kho. "
                            + "Cần: " + soLuong
                            + ", còn: " + imeiList.size()
            );
        }

        return imeiList;
    }

    /*
     * Lưu đơn hàng và toàn bộ chi tiết đơn hàng.
     */
    private void luuDonHang(
            String maDonHang,
            HoaDon hoaDon,
            String maKhachHang,
            String maKhuyenMai
    ) {

        String sqlDonHang = """
            INSERT INTO don_hang (
                ma_don_hang,
                ma_khach_hang,
                ma_nhan_vien,
                ma_khuyen_mai,
                ngay_tao,
                tong_tien_hang,
                tien_giam_gia,
                thue_vat,
                tong_tien_can_tra,
                trang_thai,
                ghi_chu
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'Da_thanh_toan', ?)
        """;

        jdbcTemplate.update(
                sqlDonHang,
                maDonHang,
                maKhachHang,
                hoaDon.getMaNhanVien(),
                maKhuyenMai,
                Timestamp.valueOf(hoaDon.getNgayLap()),
                hoaDon.getTongTienHang(),
                hoaDon.getTienGiamGia(),
                hoaDon.getTienThueVat(),
                hoaDon.getTongThanhToan(),
                hoaDon.getGhiChu()
        );

        String sqlChiTietDon = """
            INSERT INTO chi_tiet_don_hang (
                ma_don_hang,
                ma_san_pham,
                so_luong,
                don_gia
            )
            VALUES (?, ?, ?, ?)
        """;

        for (ChiTietHoaDon item : hoaDon.getItems()) {

            jdbcTemplate.update(
                    sqlChiTietDon,
                    maDonHang,
                    item.getMaSanPham(),
                    item.getSoLuong(),
                    item.getDonGia()
            );
        }
    }

    /*
     * Lưu thông tin hóa đơn.
     */
    private void luuThongTinHoaDon(
            String maDonHang,
            HoaDon hoaDon,
            String maKhachHang
    ) {

        String sql = """
            INSERT INTO hoa_don (
                ma_hoa_don,
                ma_don_hang,
                ma_khach_hang,
                ma_nhan_vien,
                ngay_lap,
                tong_tien_hang,
                thue_vat_phan_tram,
                tien_thue_vat,
                tien_giam_gia,
                tong_thanh_toan,
                phuong_thuc_thanh_toan,
                tien_khach_dua,
                tien_thua,
                trang_thai_thanh_toan,
                ghi_chu
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Thanh_cong', ?)
        """;

        jdbcTemplate.update(
                sql,
                hoaDon.getMaHoaDon(),
                maDonHang,
                maKhachHang,
                hoaDon.getMaNhanVien(),
                Timestamp.valueOf(hoaDon.getNgayLap()),
                hoaDon.getTongTienHang(),
                hoaDon.getThueVatPhanTram(),
                hoaDon.getTienThueVat(),
                hoaDon.getTienGiamGia(),
                hoaDon.getTongThanhToan(),
                hoaDon.getPhuongThucThanhToan(),
                hoaDon.getTienKhachDua(),
                hoaDon.getTienThua(),
                hoaDon.getGhiChu()
        );
    }

    /*
     * Lưu chi tiết hóa đơn.
     *
     * Một sản phẩm có số lượng N sẽ được tách thành N dòng,
     * mỗi dòng gắn với một IMEI riêng.
     */
    private void luuChiTietHoaDon(
            String maHoaDon,
            List<ChiTietHoaDon> items
    ) {

        String sql = """
            INSERT INTO chi_tiet_hoa_don (
                ma_hoa_don,
                ma_san_pham,
                ma_imei,
                don_gia,
                thoi_gian_bao_hanh_thang
            )
            VALUES (?, ?, ?, ?, ?)
        """;

        // Tránh một IMEI bị sử dụng hai lần trong cùng hóa đơn.
        Set<String> imeiDaChon = new HashSet<>();

        for (ChiTietHoaDon item : items) {

            List<String> imeiList = layVaKhoaImei(
                    item.getMaSanPham(),
                    item.getSoLuong()
            );

            for (String imei : imeiList) {

                if (!imeiDaChon.add(imei)) {
                    throw new IllegalStateException(
                            "IMEI bị trùng trong hóa đơn: " + imei
                    );
                }

                jdbcTemplate.update(
                        sql,
                        maHoaDon,
                        item.getMaSanPham(),
                        imei,
                        item.getDonGia(),
                        item.getThoiGianBaoHanhThang() > 0
                                ? item.getThoiGianBaoHanhThang()
                                : 12
                );
            }
        }
    }

    /*
     * Cập nhật trạng thái IMEI sau khi đã lưu chi tiết hóa đơn.
     */
    private void capNhatTrangThaiImei(
            String maHoaDon,
            List<ChiTietHoaDon> items
    ) {

        for (ChiTietHoaDon item : items) {

            String sql = """
                UPDATE imei_san_pham
                SET trang_thai = 'Da_ban',
                    ma_hoa_don = ?,
                    ngay_ban = CURRENT_TIMESTAMP,
                    han_bao_hanh = DATE_ADD(
                        CURRENT_DATE,
                        INTERVAL ? MONTH
                    )
                WHERE ma_san_pham = ?
                  AND trang_thai = 'Trong_kho'
                ORDER BY ngay_nhap ASC, ma_imei ASC
                LIMIT ?
            """;

            int updated = jdbcTemplate.update(
                    sql,
                    maHoaDon,
                    item.getThoiGianBaoHanhThang() > 0
                            ? item.getThoiGianBaoHanhThang()
                            : 12,
                    item.getMaSanPham(),
                    item.getSoLuong()
            );

            if (updated != item.getSoLuong()) {
                throw new IllegalStateException(
                        "Không thể cập nhật trạng thái IMEI cho sản phẩm: "
                                + item.getMaSanPham()
                );
            }
        }
    }

    /*
     * Lưu hóa đơn và chi tiết hóa đơn trong một transaction.
     *
     * Nếu bất kỳ bước nào lỗi, Spring sẽ rollback toàn bộ:
     * - Đơn hàng
     * - Chi tiết đơn hàng
     * - Hóa đơn
     * - Chi tiết hóa đơn
     * - Trạng thái IMEI
     */
    @Transactional(rollbackFor = Exception.class)
    public HoaDon luuHoaDonVaChiTiet(HoaDon hoaDon) {

        if (hoaDon == null) {
            throw new IllegalArgumentException("Hóa đơn không được null.");
        }

        if (hoaDon.getItems() == null || hoaDon.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "Hóa đơn phải có ít nhất một sản phẩm."
            );
        }

        if (hoaDon.getMaHoaDon() == null
                || hoaDon.getMaHoaDon().isBlank()) {
            hoaDon.setMaHoaDon(sinhMaHoaDonMoi());
        }

        if (hoaDon.getNgayLap() == null) {
            hoaDon.setNgayLap(LocalDateTime.now());
        }

        kiemTraNhanVien(hoaDon.getMaNhanVien());

        String maKhachHang = xuLyMaKhachHang(
                hoaDon.getMaKhachHang()
        );

        kiemTraPhuongThucThanhToan(
                hoaDon.getPhuongThucThanhToan()
        );

        // Kiểm tra dữ liệu sản phẩm trước khi ghi database.
        for (ChiTietHoaDon item : hoaDon.getItems()) {

            if (item == null) {
                throw new IllegalArgumentException(
                        "Chi tiết hóa đơn không được null."
                );
            }

            if (item.getMaSanPham() == null
                    || item.getMaSanPham().isBlank()) {
                throw new IllegalArgumentException(
                        "Chi tiết hóa đơn thiếu mã sản phẩm."
                );
            }

            if (item.getSoLuong() <= 0) {
                throw new IllegalArgumentException(
                        "Số lượng sản phẩm phải lớn hơn 0."
                );
            }

            if (item.getDonGia() == null
                    || item.getDonGia().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Đơn giá sản phẩm không hợp lệ."
                );
            }
        }

        String maDonHang = sinhMaDonHang(hoaDon.getMaHoaDon());

        String maKhuyenMai = timMaKhuyenMai(
                hoaDon.getCodeVoucher()
        );

        // 1. Tạo đơn hàng và chi tiết đơn hàng.
        luuDonHang(
                maDonHang,
                hoaDon,
                maKhachHang,
                maKhuyenMai
        );

        // 2. Lưu hóa đơn.
        luuThongTinHoaDon(
                maDonHang,
                hoaDon,
                maKhachHang
        );

        // 3. Tự chọn IMEI và lưu chi tiết hóa đơn.
        luuChiTietHoaDon(
                hoaDon.getMaHoaDon(),
                hoaDon.getItems()
        );

        // 4. Cập nhật trạng thái IMEI.
        capNhatTrangThaiImei(
                hoaDon.getMaHoaDon(),
                hoaDon.getItems()
        );

        return hoaDon;
    }

    /*
     * RowMapper cho danh sách hóa đơn gần nhất.
     */
    private final RowMapper<HoaDon> hoaDonRowMapper = (rs, rowNum) -> {

        HoaDon hd = new HoaDon();

        hd.setMaHoaDon(rs.getString("ma_hoa_don"));

        Timestamp ngayLap = rs.getTimestamp("ngay_lap");
        if (ngayLap != null) {
            hd.setNgayLap(ngayLap.toLocalDateTime());
        }

        hd.setTongTienHang(rs.getBigDecimal("tong_tien_hang"));
        hd.setThueVatPhanTram(rs.getBigDecimal("thue_vat_phan_tram"));
        hd.setTienThueVat(rs.getBigDecimal("tien_thue_vat"));
        hd.setTienGiamGia(rs.getBigDecimal("tien_giam_gia"));
        hd.setTongThanhToan(rs.getBigDecimal("tong_thanh_toan"));
        hd.setPhuongThucThanhToan(rs.getString("phuong_thuc_thanh_toan"));
        hd.setTienKhachDua(rs.getBigDecimal("tien_khach_dua"));
        hd.setTienThua(rs.getBigDecimal("tien_thua"));
        hd.setTrangThaiThanhToan(rs.getString("trang_thai_thanh_toan"));
        hd.setGhiChu(rs.getString("ghi_chu"));

        hd.setMaKhachHang(rs.getString("ma_khach_hang"));
        hd.setTenKhachHang(rs.getString("ten_khach_hang"));

        hd.setMaNhanVien(rs.getString("ma_nhan_vien"));
        hd.setTenNhanVien(rs.getString("ten_nhan_vien"));

        hd.setCodeVoucher(rs.getString("code_voucher"));

        return hd;
    };

    /*
     * Lấy danh sách hóa đơn mới nhất.
     */
    public List<HoaDon> layHoaDonGanNhat(int limit) {

        int soLuong = Math.max(1, Math.min(limit, 100));

        String sql = """
            SELECT
                hd.*,
                kh.ho_ten AS ten_khach_hang,
                nv.ho_ten AS ten_nhan_vien,
                km.code_voucher AS code_voucher
            FROM hoa_don hd
            LEFT JOIN khach_hang kh
                ON hd.ma_khach_hang = kh.ma_khach_hang
            LEFT JOIN nhan_vien nv
                ON hd.ma_nhan_vien = nv.ma_nhan_vien
            LEFT JOIN don_hang dh
                ON hd.ma_don_hang = dh.ma_don_hang
            LEFT JOIN khuyen_mai km
                ON dh.ma_khuyen_mai = km.ma_khuyen_mai
            ORDER BY hd.ngay_lap DESC
            LIMIT ?
        """;

        return jdbcTemplate.query(
                sql,
                hoaDonRowMapper,
                soLuong
        );
    }
}
