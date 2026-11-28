
package com.phonestore.dao;

import com.phonestore.dto.pos.PosCustomerResponse;
import com.phonestore.dto.pos.PosProductResponse;
import com.phonestore.model.SanPham;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PosCatalogDAO {

    private final JdbcTemplate jdbcTemplate;

    public PosCatalogDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
     * Lấy danh sách sản phẩm đang kinh doanh cho POS.
     */
    public List<PosProductResponse> layDanhSachSanPham() {

        String sql = """
                SELECT
                    sp.ma_san_pham,
                    sp.ten_san_pham,
                    sp.ma_thuong_hieu,
                    th.ten_thuong_hieu,
                    sp.dong_may,
                    sp.ram,
                    sp.rom,
                    sp.mau_sac,
                    sp.gia_ban,
                    COALESCE((
                        SELECT COUNT(*)
                        FROM imei_san_pham i
                        WHERE i.ma_san_pham = sp.ma_san_pham
                          AND i.trang_thai = 'Trong_kho'
                    ), 0) AS so_luong_ton,
                    sp.hinh_anh,
                    sp.trang_thai
                FROM san_pham sp
                LEFT JOIN thuong_hieu th
                    ON sp.ma_thuong_hieu = th.ma_thuong_hieu
                WHERE sp.trang_thai = 'Dang_kinh_doanh'
                ORDER BY sp.ten_san_pham ASC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {

            int soLuongTon = rs.getInt("so_luong_ton");
            int maThuongHieu = rs.getInt("ma_thuong_hieu");

            String maThuongHieuStr = rs.wasNull()
                    ? null
                    : String.valueOf(maThuongHieu);

            return new PosProductResponse(
                    rs.getString("ma_san_pham"),
                    rs.getString("ten_san_pham"),
                    maThuongHieuStr,
                    rs.getString("ten_thuong_hieu"),
                    rs.getString("dong_may"),
                    rs.getString("ram"),
                    rs.getString("rom"),
                    rs.getString("mau_sac"),
                    rs.getBigDecimal("gia_ban"),
                    soLuongTon,
                    rs.getString("hinh_anh"),
                    rs.getString("trang_thai")
            );
        });
    }

    /*
     * Lấy danh sách khách hàng đang hoạt động.
     */
    public List<PosCustomerResponse> layDanhSachKhachHang() {

        String sql = """
                SELECT
                    ma_khach_hang,
                    ho_ten,
                    so_dien_thoai,
                    email,
                    diem_tich_luy
                FROM khach_hang
                WHERE trang_thai = 'Hoat_dong'
                ORDER BY ho_ten ASC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {

            int diemTichLuy = rs.getInt("diem_tich_luy");

            String hangKhachHang =
                    xacDinhHangKhachHang(diemTichLuy);

            return new PosCustomerResponse(
                    rs.getString("ma_khach_hang"),
                    rs.getString("ho_ten"),
                    rs.getString("so_dien_thoai"),
                    rs.getString("email"),
                    hangKhachHang,
                    diemTichLuy
            );
        });
    }

    /*
     * Lấy thông tin sản phẩm theo mã.
     * Dùng cho nghiệp vụ kiểm tra voucher và thanh toán.
     */
    public Optional<SanPham> timSanPhamTheoMa(String maSanPham) {

        if (maSanPham == null || maSanPham.isBlank()) {
            return Optional.empty();
        }

        String sql = """
                SELECT
                    sp.ma_san_pham,
                    sp.ten_san_pham,
                    sp.dong_may,
                    sp.mau_sac,
                    sp.ram,
                    sp.rom,
                    sp.gia_ban,
                    sp.thoi_gian_bao_hanh_thang,
                    COALESCE((
                        SELECT COUNT(*)
                        FROM imei_san_pham i
                        WHERE i.ma_san_pham = sp.ma_san_pham
                          AND i.trang_thai = 'Trong_kho'
                    ), 0) AS so_luong_ton,
                    sp.trang_thai,
                    sp.ma_thuong_hieu,
                    th.ten_thuong_hieu,
                    sp.ma_danh_muc,
                    dm.ten_danh_muc
                FROM san_pham sp
                LEFT JOIN thuong_hieu th
                    ON sp.ma_thuong_hieu = th.ma_thuong_hieu
                LEFT JOIN danh_muc dm
                    ON sp.ma_danh_muc = dm.ma_danh_muc
                WHERE sp.ma_san_pham = ?
                LIMIT 1
                """;

        List<SanPham> result = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {

                    SanPham sp = new SanPham();

                    sp.setMaSanPham(
                            rs.getString("ma_san_pham")
                    );

                    sp.setTenSanPham(
                            rs.getString("ten_san_pham")
                    );

                    sp.setDongMay(
                            rs.getString("dong_may")
                    );

                    sp.setMauSac(
                            rs.getString("mau_sac")
                    );

                    sp.setRam(
                            rs.getString("ram")
                    );

                    sp.setRom(
                            rs.getString("rom")
                    );

                    sp.setGiaBan(
                            rs.getBigDecimal("gia_ban")
                    );

                    sp.setThoiGianBaoHanhThang(
                            rs.getInt("thoi_gian_bao_hanh_thang")
                    );

                    sp.setSoLuongTon(
                            rs.getInt("so_luong_ton")
                    );

                    sp.setTrangThai(
                            rs.getString("trang_thai")
                    );

                    int maThuongHieu =
                            rs.getInt("ma_thuong_hieu");

                    sp.setMaThuongHieu(
                            rs.wasNull() ? null : maThuongHieu
                    );

                    sp.setTenThuongHieu(
                            rs.getString("ten_thuong_hieu")
                    );

                    int maDanhMuc =
                            rs.getInt("ma_danh_muc");

                    sp.setMaDanhMuc(
                            rs.wasNull() ? null : maDanhMuc
                    );

                    sp.setTenDanhMuc(
                            rs.getString("ten_danh_muc")
                    );

                    return sp;
                },
                maSanPham
        );

        return result.stream().findFirst();
    }

    /*
     * Lấy hạng khách hàng theo mã khách hàng.
     *
     * Khách không đăng ký hoặc không tìm thấy được xem
     * là khách mới, không đủ điều kiện dùng voucher VIP.
     */
    public String layHangKhachHang(String maKhachHang) {

        if (maKhachHang == null || maKhachHang.isBlank()) {
            return "Khach_moi";
        }

        String sql = """
                SELECT diem_tich_luy
                FROM khach_hang
                WHERE ma_khach_hang = ?
                  AND trang_thai = 'Hoat_dong'
                """;

        List<Integer> result = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getInt("diem_tich_luy"),
                maKhachHang
        );

        if (result.isEmpty()) {
            return "Khach_moi";
        }

        return xacDinhHangKhachHang(result.get(0));
    }

    public String layTenKhachHang(String maKhachHang) {
        if (maKhachHang == null || maKhachHang.isBlank()) {
            return "Khách vãng lai";
        }

        List<String> result = jdbcTemplate.query(
                """
                SELECT ho_ten
                FROM khach_hang
                WHERE ma_khach_hang = ?
                  AND trang_thai = 'Hoat_dong'
                """,
                (rs, rowNum) -> rs.getString("ho_ten"),
                maKhachHang
        );
        return result.isEmpty() ? "Khách vãng lai" : result.get(0);
    }

    /*
     * Quy tắc phân hạng theo logic hiện có của KhachHang.
     */
    private String xacDinhHangKhachHang(int diemTichLuy) {

        if (diemTichLuy >= 1000) {
            return "Khach_VIP";
        }

        if (diemTichLuy >= 200) {
            return "Thanh_vien";
        }

        return "Khach_moi";
    }
}
