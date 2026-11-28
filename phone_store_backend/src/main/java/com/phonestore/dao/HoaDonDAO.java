
package com.phonestore.dao;

import com.phonestore.model.ChiTietHoaDon;
import com.phonestore.model.HoaDon;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Repository
public class HoaDonDAO {

    private final JdbcTemplate jdbcTemplate;
    private final KhuyenMaiDAO khuyenMaiDAO;

    public HoaDonDAO(
            JdbcTemplate jdbcTemplate,
            KhuyenMaiDAO khuyenMaiDAO
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.khuyenMaiDAO = khuyenMaiDAO;
    }

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

    private String sinhMaDonHang(String maHoaDon) {
        return "DH" + maHoaDon.substring(2);
    }

    private String xuLyMaKhachHang(String maKhachHang) {
        if (maKhachHang == null
                || maKhachHang.isBlank()
                || "KH_LE".equalsIgnoreCase(maKhachHang.trim())) {
            return null;
        }

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM khach_hang
                WHERE ma_khach_hang = ?
                  AND trang_thai = 'Hoat_dong'
                """,
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

    private void kiemTraNhanVien(String maNhanVien) {
        if (maNhanVien == null || maNhanVien.isBlank()) {
            throw new IllegalArgumentException(
                    "Mã nhân viên không được để trống."
            );
        }

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM nhan_vien
                WHERE ma_nhan_vien = ?
                """,
                Integer.class,
                maNhanVien.trim()
        );

        if (count == null || count == 0) {
            throw new IllegalArgumentException(
                    "Không tìm thấy nhân viên: " + maNhanVien
            );
        }
    }

    private String timMaKhuyenMai(String codeVoucher) {
        if (codeVoucher == null || codeVoucher.isBlank()) {
            return null;
        }

        List<String> result = jdbcTemplate.query(
                """
                SELECT ma_khuyen_mai
                FROM khuyen_mai
                WHERE LOWER(code_voucher) = LOWER(?)
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getString("ma_khuyen_mai"),
                codeVoucher.trim()
        );

        if (result.isEmpty()) {
            throw new IllegalArgumentException(
                    "Không tìm thấy voucher: " + codeVoucher
            );
        }

        return result.get(0);
    }

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
     * Khóa và lấy chính xác IMEI cần bán.
     * Phải gọi trong transaction.
     */
    private List<String> layVaKhoaImei(
            String maSanPham,
            int soLuong
    ) {
        if (maSanPham == null || maSanPham.isBlank() || soLuong <= 0) {
            throw new IllegalArgumentException(
                    "Thông tin sản phẩm hoặc số lượng không hợp lệ."
            );
        }

        List<String> imeiList = jdbcTemplate.query(
                """
                SELECT ma_imei
                FROM imei_san_pham
                WHERE ma_san_pham = ?
                  AND trang_thai = 'Trong_kho'
                ORDER BY ngay_nhap ASC, ma_imei ASC
                LIMIT ?
                FOR UPDATE
                """,
                (rs, rowNum) -> rs.getString("ma_imei"),
                maSanPham,
                soLuong
        );

        if (imeiList.size() != soLuong) {
            throw new IllegalStateException(
                    "Sản phẩm " + maSanPham
                            + " không đủ IMEI trong kho. Cần: "
                            + soLuong + ", còn: " + imeiList.size()
            );
        }

        return imeiList;
    }

    private void luuDonHang(
            String maDonHang,
            HoaDon hoaDon,
            String maKhachHang,
            String maKhuyenMai
    ) {
        jdbcTemplate.update(
                """
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
                """,
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

        for (ChiTietHoaDon item : hoaDon.getItems()) {
            jdbcTemplate.update(
                    """
                    INSERT INTO chi_tiet_don_hang (
                        ma_don_hang,
                        ma_san_pham,
                        so_luong,
                        don_gia
                    )
                    VALUES (?, ?, ?, ?)
                    """,
                    maDonHang,
                    item.getMaSanPham(),
                    item.getSoLuong(),
                    item.getDonGia()
            );
        }
    }

    private void luuThongTinHoaDon(
            String maDonHang,
            HoaDon hoaDon,
            String maKhachHang
    ) {
        jdbcTemplate.update(
                """
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
                """,
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
     * Lưu chi tiết hóa đơn và ghi nhớ IMEI đã chọn.
     * Trigger MySQL có thể cập nhật trạng thái IMEI ngay khi INSERT.
     */
    private Map<String, List<String>> luuChiTietHoaDon(
            String maHoaDon,
            List<ChiTietHoaDon> items
    ) {
        Map<String, List<String>> imeiDaChon = new LinkedHashMap<>();
        Set<String> imeiTrungLap = new HashSet<>();

        for (ChiTietHoaDon item : items) {
            List<String> imeiList = layVaKhoaImei(
                    item.getMaSanPham(),
                    item.getSoLuong()
            );

            for (String imei : imeiList) {
                if (!imeiTrungLap.add(imei)) {
                    throw new IllegalStateException(
                            "IMEI bị trùng trong hóa đơn: " + imei
                    );
                }

                jdbcTemplate.update(
                        """
                        INSERT INTO chi_tiet_hoa_don (
                            ma_hoa_don,
                            ma_san_pham,
                            ma_imei,
                            don_gia,
                            thoi_gian_bao_hanh_thang
                        )
                        VALUES (?, ?, ?, ?, ?)
                        """,
                        maHoaDon,
                        item.getMaSanPham(),
                        imei,
                        item.getDonGia(),
                        item.getThoiGianBaoHanhThang() > 0
                                ? item.getThoiGianBaoHanhThang()
                                : 12
                );
            }

            imeiDaChon.put(item.getMaSanPham(), imeiList);
        }

        return imeiDaChon;
    }

    /*
     * Đồng bộ trạng thái IMEI cho cả MySQL và H2:
     *
     * - MySQL: trigger đã cập nhật IMEI và tồn kho khi INSERT.
     * - H2: nếu chưa có trigger, DAO cập nhật đúng các IMEI đã chọn
     *   và tự trừ tồn kho.
     *
     * Không cập nhật một nhóm IMEI khác.
     */
    private void dongBoTrangThaiImei(
            String maHoaDon,
            List<ChiTietHoaDon> items,
            Map<String, List<String>> imeiDaChon
    ) {
        for (ChiTietHoaDon item : items) {
            List<String> imeiList = imeiDaChon.get(item.getMaSanPham());

            if (imeiList == null || imeiList.size() != item.getSoLuong()) {
                throw new IllegalStateException(
                        "Danh sách IMEI không khớp sản phẩm: "
                                + item.getMaSanPham()
                );
            }

            String placeholders = String.join(
                    ",",
                    java.util.Collections.nCopies(imeiList.size(), "?")
            );

            List<Object> args = new ArrayList<>();
            args.add(maHoaDon);
            int warrantyMonths = item.getThoiGianBaoHanhThang() > 0
                    ? item.getThoiGianBaoHanhThang()
                    : 12;
            args.add(java.sql.Date.valueOf(LocalDate.now().plusMonths(warrantyMonths)));
            args.addAll(imeiList);

            String sql = """
                    UPDATE imei_san_pham
                    SET trang_thai = 'Da_ban',
                        ma_hoa_don = ?,
                        ngay_ban = CURRENT_TIMESTAMP,
                        han_bao_hanh = ?
                    WHERE ma_imei IN (%s)
                      AND trang_thai = 'Trong_kho'
                    """.formatted(placeholders);

            int updated = jdbcTemplate.update(sql, args.toArray());

            if (updated == imeiList.size()) {
                // Không có trigger: DAO tự trừ tồn kho.
                int stockUpdated = jdbcTemplate.update(
                        """
                        UPDATE san_pham
                        SET so_luong_ton = so_luong_ton - ?
                        WHERE ma_san_pham = ?
                          AND so_luong_ton >= ?
                        """,
                        item.getSoLuong(),
                        item.getMaSanPham(),
                        item.getSoLuong()
                );

                if (stockUpdated != 1) {
                    throw new IllegalStateException(
                            "Không thể cập nhật tồn kho sản phẩm: "
                                    + item.getMaSanPham()
                    );
                }

            } else if (updated == 0) {
                // Có thể trigger MySQL đã cập nhật IMEI.
                Integer daBan = jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM imei_san_pham
                        WHERE ma_hoa_don = ?
                          AND ma_san_pham = ?
                          AND trang_thai = 'Da_ban'
                          AND ma_imei IN (%s)
                        """.formatted(placeholders),
                        Integer.class,
                        buildArgs(maHoaDon, item.getMaSanPham(), imeiList)
                );

                if (daBan == null || daBan != imeiList.size()) {
                    throw new IllegalStateException(
                            "Không thể xác nhận trạng thái IMEI của hóa đơn "
                                    + maHoaDon
                    );
                }

            } else {
                throw new IllegalStateException(
                        "Chỉ cập nhật được một phần IMEI của sản phẩm: "
                                + item.getMaSanPham()
                );
            }
        }
    }

    private Object[] buildArgs(
            String maHoaDon,
            String maSanPham,
            List<String> imeiList
    ) {
        List<Object> args = new ArrayList<>();
        args.add(maHoaDon);
        args.add(maSanPham);
        args.addAll(imeiList);
        return args.toArray();
    }

    /*
     * Toàn bộ thao tác bán hàng nằm trong một transaction.
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

        for (ChiTietHoaDon item : hoaDon.getItems()) {
            if (item == null
                    || item.getMaSanPham() == null
                    || item.getMaSanPham().isBlank()
                    || item.getSoLuong() <= 0
                    || item.getDonGia() == null
                    || item.getDonGia().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Chi tiết hóa đơn không hợp lệ."
                );
            }
        }

        String maDonHang = sinhMaDonHang(hoaDon.getMaHoaDon());

        String maKhuyenMai = timMaKhuyenMai(
                hoaDon.getCodeVoucher()
        );

        // Tăng lượt sử dụng voucher bằng UPDATE có điều kiện.
        // Nếu thất bại, transaction sẽ rollback.
        if (maKhuyenMai != null
                && !khuyenMaiDAO.tangSoLuongDaDung(maKhuyenMai)) {
            throw new IllegalStateException(
                    "Voucher đã hết lượt, hết hạn hoặc không còn hiệu lực."
            );
        }

        luuDonHang(
                maDonHang,
                hoaDon,
                maKhachHang,
                maKhuyenMai
        );

        luuThongTinHoaDon(
                maDonHang,
                hoaDon,
                maKhachHang
        );

        Map<String, List<String>> imeiDaChon = luuChiTietHoaDon(
                hoaDon.getMaHoaDon(),
                hoaDon.getItems()
        );

        dongBoTrangThaiImei(
                hoaDon.getMaHoaDon(),
                hoaDon.getItems(),
                imeiDaChon
        );

        return hoaDon;
    }

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

    public List<HoaDon> layHoaDonGanNhat(int limit) {
        int soLuong = Math.max(1, Math.min(limit, 100));

        return jdbcTemplate.query(
                """
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
                """,
                hoaDonRowMapper,
                soLuong
        );
    }

    public List<ChiTietHoaDon> layChiTietHoaDon(String maHoaDon) {
        return jdbcTemplate.query(
                """
                SELECT cthd.ma_san_pham,
                       sp.ten_san_pham,
                       cthd.ma_imei,
                       cthd.don_gia,
                       cthd.thoi_gian_bao_hanh_thang
                FROM chi_tiet_hoa_don cthd
                JOIN san_pham sp ON sp.ma_san_pham = cthd.ma_san_pham
                WHERE cthd.ma_hoa_don = ?
                ORDER BY cthd.ma_chi_tiet_hd
                """,
                (rs, rowNum) -> {
                    ChiTietHoaDon item = new ChiTietHoaDon();
                    item.setMaSanPham(rs.getString("ma_san_pham"));
                    item.setTenSanPham(rs.getString("ten_san_pham"));
                    item.setMaImei(rs.getString("ma_imei"));
                    item.setDonGia(rs.getBigDecimal("don_gia"));
                    item.setThoiGianBaoHanhThang(
                            rs.getInt("thoi_gian_bao_hanh_thang")
                    );
                    return item;
                },
                maHoaDon
        );
    }
}
