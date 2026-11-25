package com.phonestore.dao;

import com.phonestore.dto.request.PosCheckoutRequest;
import com.phonestore.dto.response.PosCheckoutResponse;
import com.phonestore.dto.response.PosCustomerDTO;
import com.phonestore.dto.response.PosProductDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PosDao {

    private final DataSource dataSource;
    private final PromotionDao promotionDao;

    public List<PosProductDTO> findPosProducts(String keyword, Integer maThuongHieu) {
        StringBuilder sql = new StringBuilder("""
                SELECT sp.ma_san_pham, sp.ten_san_pham, sp.ma_thuong_hieu, th.ten_thuong_hieu,
                       sp.ma_danh_muc, dm.ten_danh_muc, sp.dong_may, sp.mau_sac, sp.ram, sp.rom,
                       sp.gia_ban, sp.so_luong_ton, sp.dinh_muc_ton_toi_thieu, sp.thoi_gian_bao_hanh_thang
                FROM san_pham sp
                JOIN thuong_hieu th ON sp.ma_thuong_hieu = th.ma_thuong_hieu
                JOIN danh_muc dm ON sp.ma_danh_muc = dm.ma_danh_muc
                WHERE sp.trang_thai = 'Dang_kinh_doanh'
                """);
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(sp.ten_san_pham) LIKE ? OR LOWER(sp.ma_san_pham) LIKE ? OR LOWER(sp.dong_may) LIKE ?)");
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (maThuongHieu != null) {
            sql.append(" AND sp.ma_thuong_hieu = ?");
            params.add(maThuongHieu);
        }
        sql.append(" ORDER BY sp.so_luong_ton DESC, sp.ten_san_pham ASC");

        Map<String, List<String>> imeiByProduct = loadAvailableImeis();
        List<PosProductDTO> result = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoJdbcHelper.bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String maSp = rs.getString("ma_san_pham");
                    result.add(PosProductDTO.builder()
                            .maSanPham(maSp)
                            .tenSanPham(rs.getString("ten_san_pham"))
                            .maThuongHieu(rs.getInt("ma_thuong_hieu"))
                            .tenThuongHieu(rs.getString("ten_thuong_hieu"))
                            .maDanhMuc(rs.getInt("ma_danh_muc"))
                            .tenDanhMuc(rs.getString("ten_danh_muc"))
                            .dongMay(rs.getString("dong_may"))
                            .mauSac(rs.getString("mau_sac"))
                            .ram(rs.getString("ram"))
                            .rom(rs.getString("rom"))
                            .giaBan(DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("gia_ban")))
                            .soLuongTon(imeiByProduct.getOrDefault(maSp, List.of()).size())
                            .dinhMucTonToiThieu(rs.getInt("dinh_muc_ton_toi_thieu"))
                            .thoiGianBaoHanhThang(rs.getInt("thoi_gian_bao_hanh_thang"))
                            .imeis(imeiByProduct.getOrDefault(maSp, List.of()))
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tải danh sách sản phẩm POS", e);
        }
        return result;
    }

    public Optional<PosProductDTO> findProductById(String maSanPham) {
        String sql = """
                SELECT sp.ma_san_pham, sp.ten_san_pham, sp.ma_thuong_hieu, th.ten_thuong_hieu,
                       sp.ma_danh_muc, dm.ten_danh_muc, sp.dong_may, sp.mau_sac, sp.ram, sp.rom,
                       sp.gia_ban, sp.so_luong_ton, sp.dinh_muc_ton_toi_thieu, sp.thoi_gian_bao_hanh_thang
                FROM san_pham sp
                JOIN thuong_hieu th ON sp.ma_thuong_hieu = th.ma_thuong_hieu
                JOIN danh_muc dm ON sp.ma_danh_muc = dm.ma_danh_muc
                WHERE sp.ma_san_pham = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSanPham);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(PosProductDTO.builder()
                            .maSanPham(rs.getString("ma_san_pham"))
                            .tenSanPham(rs.getString("ten_san_pham"))
                            .maThuongHieu(rs.getInt("ma_thuong_hieu"))
                            .tenThuongHieu(rs.getString("ten_thuong_hieu"))
                            .maDanhMuc(rs.getInt("ma_danh_muc"))
                            .tenDanhMuc(rs.getString("ten_danh_muc"))
                            .dongMay(rs.getString("dong_may"))
                            .mauSac(rs.getString("mau_sac"))
                            .ram(rs.getString("ram"))
                            .rom(rs.getString("rom"))
                            .giaBan(DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("gia_ban")))
                            .soLuongTon(rs.getInt("so_luong_ton"))
                            .dinhMucTonToiThieu(rs.getInt("dinh_muc_ton_toi_thieu"))
                            .thoiGianBaoHanhThang(rs.getInt("thoi_gian_bao_hanh_thang"))
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tra cứu thông tin sản phẩm", e);
        }
        return Optional.empty();
    }

    public List<PosCustomerDTO> findAllCustomers() {
        String sql = """
                SELECT ma_khach_hang, ho_ten, so_dien_thoai, email, diem_tich_luy
                FROM khach_hang
                WHERE trang_thai = 'Hoat_dong'
                ORDER BY diem_tich_luy DESC, ho_ten ASC
                """;
        List<PosCustomerDTO> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapCustomer(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tải danh sách khách hàng", e);
        }
        return list;
    }

    public Optional<PosCustomerDTO> findCustomerById(String maKhachHang) {
        if (maKhachHang == null || maKhachHang.isBlank() || "KH_LE".equalsIgnoreCase(maKhachHang)) {
            return Optional.empty();
        }
        String sql = "SELECT ma_khach_hang, ho_ten, so_dien_thoai, email, diem_tich_luy FROM khach_hang WHERE ma_khach_hang = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maKhachHang.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapCustomer(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tra cứu khách hàng", e);
        }
        return Optional.empty();
    }

    public PosCheckoutResponse executeCheckout(
            String maDonHang,
            String maHoaDon,
            PosCheckoutRequest req,
            String maKhuyenMai,
            String codeVoucher,
            String quaTang,
            BigDecimal tongTienHang,
            BigDecimal tienGiamGia,
            BigDecimal vatPhanTram,
            BigDecimal tienThueVat,
            BigDecimal tongThanhToan,
            BigDecimal tienKhachDua,
            BigDecimal tienThua
    ) {
        LocalDateTime now = LocalDateTime.now();
        String customerId = (req.getMaKhachHang() == null || req.getMaKhachHang().isBlank() || "KH_LE".equalsIgnoreCase(req.getMaKhachHang()))
                ? null : req.getMaKhachHang().trim();
        String staffId = (req.getMaNhanVien() == null || req.getMaNhanVien().isBlank()) ? "NV003" : req.getMaNhanVien().trim();
        String paymentMethod = (req.getPhuongThucThanhToan() == null || req.getPhuongThucThanhToan().isBlank())
                ? "Tien_mat" : req.getPhuongThucThanhToan().trim();

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            List<PosCheckoutResponse.InvoiceLineDTO> invoiceLines = new ArrayList<>();
            Map<String, Integer> preCheckoutStock = new HashMap<>();
            Map<String, Integer> qtySoldByProduct = new HashMap<>();

            // Validate stock and lock/resolve concrete IMEIs before writing invoice
            for (PosCheckoutRequest.CheckoutItem item : req.getItems()) {
                String maSp = item.getMaSanPham();
                int qty = item.getSoLuong() != null && item.getSoLuong() > 0 ? item.getSoLuong() : 1;
                ProductLockInfo prodInfo = loadProductForCheckout(conn, maSp);
                preCheckoutStock.putIfAbsent(maSp, prodInfo.stock);
                qtySoldByProduct.merge(maSp, qty, Integer::sum);

                if (prodInfo.stock < qtySoldByProduct.get(maSp)) {
                    throw new IllegalArgumentException("Sản phẩm " + prodInfo.name + " không đủ tồn kho để xuất bán");
                }

                List<String> resolvedImeis = resolveImeisForItem(conn, maSp, item.getMaImei(), qty, prodInfo.name);
                BigDecimal unitPrice = item.getDonGia() != null ? item.getDonGia() : prodInfo.salePrice;
                for (String imei : resolvedImeis) {
                    invoiceLines.add(PosCheckoutResponse.InvoiceLineDTO.builder()
                            .maSanPham(maSp)
                            .tenSanPham(prodInfo.name)
                            .maImei(imei)
                            .donGia(unitPrice)
                            .thoiGianBaoHanhThang(prodInfo.warrantyMonths)
                            .build());
                }
            }

            if (maKhuyenMai != null) {
                boolean reserved = promotionDao.incrementUsedCountIfAvailable(conn, maKhuyenMai);
                if (!reserved) {
                    throw new IllegalStateException("Mã khuyến mãi " + codeVoucher + " đã hết lượt sử dụng");
                }
            }

            insertOrder(conn, maDonHang, customerId, staffId, maKhuyenMai, now, tongTienHang, tienGiamGia, tienThueVat, tongThanhToan, req.getGhiChu());
            insertOrderDetails(conn, maDonHang, req.getItems());
            insertInvoice(conn, maHoaDon, maDonHang, customerId, staffId, now, tongTienHang, vatPhanTram, tienThueVat, tienGiamGia, tongThanhToan, paymentMethod, tienKhachDua, tienThua, req.getGhiChu());

            LocalDate today = now.toLocalDate();
            for (PosCheckoutResponse.InvoiceLineDTO line : invoiceLines) {
                insertInvoiceDetail(conn, maHoaDon, line);
                markImeiAsSold(conn, line.getMaImei(), maHoaDon, now, today.plusMonths(line.getThoiGianBaoHanhThang()));
            }

            // Reconcile stock safely: only decrement if DB trigger did not already decrement
            for (Map.Entry<String, Integer> entry : qtySoldByProduct.entrySet()) {
                reconcileProductStock(conn, entry.getKey(), preCheckoutStock.get(entry.getKey()), entry.getValue());
            }

            if (customerId != null) {
                int earnedPoints = tongThanhToan.divide(BigDecimal.valueOf(1_000_000), 0, RoundingMode.DOWN).intValue();
                if (earnedPoints > 0) {
                    addCustomerPoints(conn, customerId, earnedPoints);
                }
            }

            conn.commit();

            String customerName = customerId == null ? "Khách vãng lai" : findCustomerName(conn, customerId);
            String staffName = findEmployeeName(conn, staffId);

            return PosCheckoutResponse.builder()
                    .maHoaDon(maHoaDon)
                    .maDonHang(maDonHang)
                    .ngayLap(now)
                    .maKhachHang(customerId)
                    .tenKhachHang(customerName)
                    .maNhanVien(staffId)
                    .tenNhanVien(staffName)
                    .phuongThucThanhToan(paymentMethod)
                    .codeVoucher(codeVoucher)
                    .quaTang(quaTang)
                    .tongTienHang(tongTienHang)
                    .tienGiamGia(tienGiamGia)
                    .thueVatPhanTram(vatPhanTram)
                    .tienThueVat(tienThueVat)
                    .tongThanhToan(tongThanhToan)
                    .tienKhachDua(tienKhachDua)
                    .tienThua(tienThua)
                    .trangThaiThanhToan("Thanh_cong")
                    .items(invoiceLines)
                    .build();
        } catch (RuntimeException ex) {
            rollbackQuietly(conn);
            throw ex;
        } catch (SQLException ex) {
            rollbackQuietly(conn);
            throw new IllegalStateException("Lỗi giao dịch thanh toán POS: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(conn);
        }
    }

    public List<PosCheckoutResponse> findRecentInvoices(int limit) {
        String sql = """
                SELECT hd.ma_hoa_don, hd.ma_don_hang, hd.ngay_lap,
                       hd.ma_khach_hang, COALESCE(kh.ho_ten, 'Khách vãng lai') AS ten_khach_hang,
                       hd.ma_nhan_vien, COALESCE(nv.ho_ten, hd.ma_nhan_vien) AS ten_nhan_vien,
                       hd.phuong_thuc_thanh_toan, km.code_voucher,
                       hd.tong_tien_hang, hd.tien_giam_gia, hd.thue_vat_phan_tram,
                       hd.tien_thue_vat, hd.tong_thanh_toan, hd.tien_khach_dua,
                       hd.tien_thua, hd.trang_thai_thanh_toan
                FROM hoa_don hd
                LEFT JOIN khach_hang kh ON hd.ma_khach_hang = kh.ma_khach_hang
                LEFT JOIN nhan_vien nv ON hd.ma_nhan_vien = nv.ma_nhan_vien
                LEFT JOIN don_hang dh ON hd.ma_don_hang = dh.ma_don_hang
                LEFT JOIN khuyen_mai km ON dh.ma_khuyen_mai = km.ma_khuyen_mai
                ORDER BY hd.ngay_lap DESC
                LIMIT ?
                """;
        List<PosCheckoutResponse> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(PosCheckoutResponse.builder()
                            .maHoaDon(rs.getString("ma_hoa_don"))
                            .maDonHang(rs.getString("ma_don_hang"))
                            .ngayLap(DaoJdbcHelper.toLocalDateTime(rs.getTimestamp("ngay_lap")))
                            .maKhachHang(rs.getString("ma_khach_hang"))
                            .tenKhachHang(rs.getString("ten_khach_hang"))
                            .maNhanVien(rs.getString("ma_nhan_vien"))
                            .tenNhanVien(rs.getString("ten_nhan_vien"))
                            .phuongThucThanhToan(rs.getString("phuong_thuc_thanh_toan"))
                            .codeVoucher(rs.getString("code_voucher"))
                            .tongTienHang(DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("tong_tien_hang")))
                            .tienGiamGia(DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("tien_giam_gia")))
                            .thueVatPhanTram(DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("thue_vat_phan_tram")))
                            .tienThueVat(DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("tien_thue_vat")))
                            .tongThanhToan(DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("tong_thanh_toan")))
                            .tienKhachDua(DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("tien_khach_dua")))
                            .tienThua(DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("tien_thua")))
                            .trangThaiThanhToan(rs.getString("trang_thai_thanh_toan"))
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tải lịch sử hóa đơn gần đây", e);
        }
        return list;
    }

    private Map<String, List<String>> loadAvailableImeis() {
        String sql = "SELECT ma_san_pham, ma_imei FROM imei_san_pham WHERE trang_thai = 'Trong_kho' ORDER BY ngay_nhap ASC";
        Map<String, List<String>> map = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.computeIfAbsent(rs.getString("ma_san_pham"), k -> new ArrayList<>())
                        .add(rs.getString("ma_imei"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lỗi tải danh sách IMEI khả dụng", e);
        }
        return map;
    }

    private PosCustomerDTO mapCustomer(ResultSet rs) throws SQLException {
        int points = rs.getInt("diem_tich_luy");
        String tier = points >= 200 ? "Khach_VIP" : (points >= 50 ? "Thanh_vien" : "Khach_moi");
        return PosCustomerDTO.builder()
                .maKhachHang(rs.getString("ma_khach_hang"))
                .hoTen(rs.getString("ho_ten"))
                .soDienThoai(rs.getString("so_dien_thoai"))
                .email(rs.getString("email"))
                .diemTichLuy(points)
                .hangKhachHang(tier)
                .build();
    }

    private ProductLockInfo loadProductForCheckout(Connection conn, String maSanPham) throws SQLException {
        String sql = """
                SELECT sp.ten_san_pham, sp.gia_ban, sp.thoi_gian_bao_hanh_thang,
                       (
                           SELECT COUNT(*)
                           FROM imei_san_pham i
                           WHERE i.ma_san_pham = sp.ma_san_pham
                             AND i.trang_thai = 'Trong_kho'
                       ) AS so_luong_ton
                FROM san_pham sp
                WHERE sp.ma_san_pham = ?
                FOR UPDATE
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSanPham);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Không tìm thấy sản phẩm: " + maSanPham);
                }

                return new ProductLockInfo(
                        rs.getString("ten_san_pham"),
                        DaoJdbcHelper.zeroIfNull(rs.getBigDecimal("gia_ban")),
                        rs.getInt("so_luong_ton"),
                        rs.getInt("thoi_gian_bao_hanh_thang")
                );
            }
        }
    }

    private List<String> resolveImeisForItem(Connection conn, String maSanPham, String requestedImei, int qty, String productName) throws SQLException {
        List<String> selected = new ArrayList<>();
        if (requestedImei != null && !requestedImei.isBlank() && qty == 1) {
            String checkSql = "SELECT ma_imei FROM imei_san_pham WHERE ma_imei = ? AND ma_san_pham = ? AND trang_thai = 'Trong_kho'";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, requestedImei.trim());
                ps.setString(2, maSanPham);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("IMEI " + requestedImei + " không hợp lệ hoặc không còn trong kho");
                    }
                    selected.add(rs.getString(1));
                    return selected;
                }
            }
        }

        String pickSql = "SELECT ma_imei FROM imei_san_pham WHERE ma_san_pham = ? AND trang_thai = 'Trong_kho' ORDER BY ngay_nhap ASC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(pickSql)) {
            ps.setString(1, maSanPham);
            ps.setInt(2, qty);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    selected.add(rs.getString(1));
                }
            }
        }
        if (selected.size() < qty) {
            throw new IllegalArgumentException("Không đủ IMEI khả dụng trong kho cho sản phẩm " + productName);
        }
        return selected;
    }

    private void insertOrder(Connection conn, String maDonHang, String customerId, String staffId, String maKhuyenMai,
                             LocalDateTime now, BigDecimal tongTienHang, BigDecimal tienGiamGia, BigDecimal tienThueVat,
                             BigDecimal tongThanhToan, String ghiChu) throws SQLException {
        String sql = """
                INSERT INTO don_hang (ma_don_hang, ma_khach_hang, ma_nhan_vien, ma_khuyen_mai, ngay_tao,
                                      tong_tien_hang, tien_giam_gia, thue_vat, tong_tien_can_tra, trang_thai, ghi_chu)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'Da_thanh_toan', ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maDonHang);
            if (customerId != null) ps.setString(2, customerId); else ps.setNull(2, Types.VARCHAR);
            ps.setString(3, staffId);
            if (maKhuyenMai != null) ps.setString(4, maKhuyenMai); else ps.setNull(4, Types.VARCHAR);
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.setBigDecimal(6, tongTienHang);
            ps.setBigDecimal(7, tienGiamGia);
            ps.setBigDecimal(8, tienThueVat);
            ps.setBigDecimal(9, tongThanhToan);
            ps.setString(10, ghiChu);
            ps.executeUpdate();
        }
    }

    private void insertOrderDetails(Connection conn, String maDonHang, List<PosCheckoutRequest.CheckoutItem> items) throws SQLException {
        String sql = "INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_san_pham, so_luong, don_gia) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (PosCheckoutRequest.CheckoutItem item : items) {
                ps.setString(1, maDonHang);
                ps.setString(2, item.getMaSanPham());
                ps.setInt(3, item.getSoLuong() != null ? item.getSoLuong() : 1);
                ps.setBigDecimal(4, DaoJdbcHelper.zeroIfNull(item.getDonGia()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertInvoice(Connection conn, String maHoaDon, String maDonHang, String customerId, String staffId,
                               LocalDateTime now, BigDecimal tongTienHang, BigDecimal vatPhanTram, BigDecimal tienThueVat,
                               BigDecimal tienGiamGia, BigDecimal tongThanhToan, String paymentMethod,
                               BigDecimal tienKhachDua, BigDecimal tienThua, String ghiChu) throws SQLException {
        String sql = """
                INSERT INTO hoa_don (ma_hoa_don, ma_don_hang, ma_khach_hang, ma_nhan_vien, ngay_lap,
                                     tong_tien_hang, thue_vat_phan_tram, tien_thue_vat, tien_giam_gia, tong_thanh_toan,
                                     phuong_thuc_thanh_toan, tien_khach_dua, tien_thua, trang_thai_thanh_toan, ghi_chu)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Thanh_cong', ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maHoaDon);
            ps.setString(2, maDonHang);
            if (customerId != null) ps.setString(3, customerId); else ps.setNull(3, Types.VARCHAR);
            ps.setString(4, staffId);
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.setBigDecimal(6, tongTienHang);
            ps.setBigDecimal(7, DaoJdbcHelper.zeroIfNull(vatPhanTram));
            ps.setBigDecimal(8, tienThueVat);
            ps.setBigDecimal(9, tienGiamGia);
            ps.setBigDecimal(10, tongThanhToan);
            ps.setString(11, paymentMethod);
            ps.setBigDecimal(12, tienKhachDua);
            ps.setBigDecimal(13, tienThua);
            ps.setString(14, ghiChu);
            ps.executeUpdate();
        }
    }

    private void insertInvoiceDetail(Connection conn, String maHoaDon, PosCheckoutResponse.InvoiceLineDTO line) throws SQLException {
        String sql = "INSERT INTO chi_tiet_hoa_don (ma_hoa_don, ma_san_pham, ma_imei, don_gia, thoi_gian_bao_hanh_thang) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maHoaDon);
            ps.setString(2, line.getMaSanPham());
            ps.setString(3, line.getMaImei());
            ps.setBigDecimal(4, line.getDonGia());
            ps.setInt(5, line.getThoiGianBaoHanhThang());
            ps.executeUpdate();
        }
    }

    private void markImeiAsSold(Connection conn, String maImei, String maHoaDon, LocalDateTime ngayBan, LocalDate hanBaoHanh) throws SQLException {
        String sql = "UPDATE imei_san_pham SET trang_thai = 'Da_ban', ma_hoa_don = ?, ngay_ban = ?, han_bao_hanh = ? WHERE ma_imei = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maHoaDon);
            ps.setTimestamp(2, Timestamp.valueOf(ngayBan));
            ps.setDate(3, Date.valueOf(hanBaoHanh));
            ps.setString(4, maImei);
            ps.executeUpdate();
        }
    }

    private void reconcileProductStock(Connection conn, String maSanPham, int stockBefore, int qtySold)
            throws SQLException {
        String sql = """
                UPDATE san_pham
                SET so_luong_ton = (
                    SELECT COUNT(*)
                    FROM imei_san_pham
                    WHERE ma_san_pham = ?
                      AND trang_thai = 'Trong_kho'
                )
                WHERE ma_san_pham = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSanPham);
            ps.setString(2, maSanPham);
            ps.executeUpdate();
        }
    }

    private void addCustomerPoints(Connection conn, String maKhachHang, int points) throws SQLException {
        String sql = "UPDATE khach_hang SET diem_tich_luy = COALESCE(diem_tich_luy, 0) + ? WHERE ma_khach_hang = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, points);
            ps.setString(2, maKhachHang);
            ps.executeUpdate();
        }
    }

    private String findCustomerName(Connection conn, String maKhachHang) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT ho_ten FROM khach_hang WHERE ma_khach_hang = ?")) {
            ps.setString(1, maKhachHang);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "Khách hàng";
            }
        }
    }

    private String findEmployeeName(Connection conn, String maNhanVien) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT ho_ten FROM nhan_vien WHERE ma_nhan_vien = ?")) {
            ps.setString(1, maNhanVien);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : maNhanVien;
            }
        }
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private record ProductLockInfo(String name, BigDecimal salePrice, int stock, int warrantyMonths) {
    }
}

