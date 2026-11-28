package com.phonestore.dao;

import com.phonestore.dto.warranty.WarrantyCloseRequest;
import com.phonestore.dto.warranty.WarrantyProgressRequest;
import com.phonestore.dto.warranty.WarrantyReceiveRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

@Repository
public class WarrantyManagementDAO {

    private static final String CHECK_RESULT_MARKER = "[[KET_QUA]]";
    private static final String CAUSE_MARKER = "[[NGUYEN_NHAN]]";
    private static final String REQUEST_MARKER = "[[YEU_CAU]]";
    private static final String NOTE_MARKER = "[[GHI_CHU]]";

    private final JdbcTemplate jdbcTemplate;

    public WarrantyManagementDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> layDanhSach(String keyword, String trangThai) {
        List<Map<String, Object>> tickets = jdbcTemplate.query(
                """
                SELECT pb.ma_phieu_bao_hanh, pb.ma_imei, pb.ma_khach_hang,
                       pb.loi_khach_phan_anh, pb.ngay_tiep_nhan, pb.trang_thai,
                       sp.ten_san_pham,
                       kh.ho_ten AS ten_khach_hang, kh.so_dien_thoai
                FROM phieu_bao_hanh pb
                LEFT JOIN san_pham sp ON sp.ma_san_pham = pb.ma_san_pham
                LEFT JOIN khach_hang kh ON kh.ma_khach_hang = pb.ma_khach_hang
                ORDER BY pb.ngay_tiep_nhan DESC, pb.ma_phieu_bao_hanh DESC
                """,
                this::mapListItem
        );
        String normalizedKeyword = normalize(keyword);
        String normalizedStatus = normalize(trangThai);
        return tickets.stream()
                .filter(ticket -> normalizedStatus == null
                        || normalizedStatus.equals(normalize((String) ticket.get("trangThai"))))
                .filter(ticket -> normalizedKeyword == null
                        || containsAny(ticket, normalizedKeyword))
                .toList();
    }

    public Map<String, Object> kiemTraImei(String imei) {
        if (imei == null || imei.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập số IMEI.");
        }
        ImeiInfo info = findImei(imei.trim());
        if (info == null) {
            return checkResult(false, "IMEI không tồn tại trong hệ thống.", null);
        }

        String reason;
        if (!"Da_ban".equals(info.status()) && !"Trong_kho".equals(info.status())) {
            reason = "IMEI hiện không đủ điều kiện tiếp nhận bảo hành.";
            return checkResult(false, reason, info);
        }
        if (hasActiveTicket(imei.trim())) {
            reason = "IMEI đang có phiếu bảo hành chưa hoàn tất.";
            return checkResult(false, reason, info);
        }

        boolean inStock = "Trong_kho".equals(info.status());
        boolean underWarranty = !inStock && info.warrantyEnd() != null
                && !info.warrantyEnd().isBefore(LocalDate.now());
        reason = inStock
                ? "Thiết bị đang trong kho, có thể tiếp nhận sửa chữa nội bộ."
                : underWarranty
                    ? "Thiết bị còn hạn bảo hành."
                    : "Thiết bị đã hết hạn bảo hành, có thể tiếp nhận sửa chữa dịch vụ.";
        Map<String, Object> result = checkResult(true, reason, info);
        result.put("conBaoHanh", underWarranty);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> tiepNhan(WarrantyReceiveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin tiếp nhận bảo hành.");
        }
        String imei = required(request.maImei(), "IMEI");
        String employeeId = required(request.maNhanVien(), "Mã nhân viên tiếp nhận");
        String issue = required(request.loiPhanAnh(), "Lỗi khách phản ánh");
        String condition = emptyToNull(request.tinhTrangTiepNhan());
        String requestText = emptyToNull(request.yeuCauSuaChua());
        requireEmployee(employeeId);

        Map<String, Object> check = kiemTraImei(imei);
        if (!Boolean.TRUE.equals(check.get("found"))) {
            throw new IllegalArgumentException(String.valueOf(check.get("lyDo")));
        }
        String customerId = (String) check.get("maKhachHang");
        if (hasText(request.maKhachHang()) && !request.maKhachHang().trim().equals(customerId)) {
            throw new IllegalArgumentException("Khách hàng không khớp với thông tin bán hàng của IMEI.");
        }
        ImeiInfo info = findImei(imei);
        String ticketId = createTicketId();
        String insertSql = """
                INSERT INTO phieu_bao_hanh (
                    ma_phieu_bao_hanh, ma_imei, ma_san_pham, ma_khach_hang, ma_hoa_don,
                    ma_nhan_vien_tiep_nhan, ngay_tiep_nhan, tinh_trang_tiep_nhan,
                    loi_khach_phan_anh, trang_thai, ghi_chu
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'Tiep_nhan', ?)
                """;
        jdbcTemplate.update(insertSql, ticketId, imei, info.productId(), customerId,
                info.invoiceId(), employeeId, Timestamp.valueOf(LocalDateTime.now()),
                condition == null ? "Chưa ghi nhận" : condition, issue, packNotes(requestText, null));
        jdbcTemplate.update("UPDATE imei_san_pham SET trang_thai = 'Dang_bao_hanh' WHERE ma_imei = ?", imei);

        if (countHistory(ticketId) == 0) {
            insertHistory(ticketId, employeeId, "Tiep_nhan", "Tiếp nhận thiết bị bảo hành từ khách hàng");
        }
        return timChiTiet(ticketId);
    }

    public Map<String, Object> timChiTiet(String ticketId) {
        List<Map<String, Object>> result = jdbcTemplate.query(
                """
                SELECT pb.*, sp.ten_san_pham,
                       kh.ho_ten AS ten_khach_hang, kh.so_dien_thoai,
                       nv.ho_ten AS ten_nhan_vien_tiep_nhan,
                       (SELECT ls.ma_nhan_vien_cap_nhat FROM lich_su_bao_hanh ls
                        WHERE ls.ma_phieu_bao_hanh = pb.ma_phieu_bao_hanh
                        ORDER BY ls.thoi_gian_cap_nhat DESC, ls.ma_lich_su DESC LIMIT 1)
                        AS ma_nhan_vien_cap_nhat,
                       (SELECT nv2.ho_ten FROM lich_su_bao_hanh ls
                        LEFT JOIN nhan_vien nv2 ON nv2.ma_nhan_vien = ls.ma_nhan_vien_cap_nhat
                        WHERE ls.ma_phieu_bao_hanh = pb.ma_phieu_bao_hanh
                        ORDER BY ls.thoi_gian_cap_nhat DESC, ls.ma_lich_su DESC LIMIT 1)
                        AS ten_nhan_vien_cap_nhat,
                       (SELECT ls.thoi_gian_cap_nhat FROM lich_su_bao_hanh ls
                        WHERE ls.ma_phieu_bao_hanh = pb.ma_phieu_bao_hanh
                        ORDER BY ls.thoi_gian_cap_nhat DESC, ls.ma_lich_su DESC LIMIT 1)
                        AS ngay_cap_nhat
                FROM phieu_bao_hanh pb
                LEFT JOIN san_pham sp ON sp.ma_san_pham = pb.ma_san_pham
                LEFT JOIN khach_hang kh ON kh.ma_khach_hang = pb.ma_khach_hang
                LEFT JOIN nhan_vien nv ON nv.ma_nhan_vien = pb.ma_nhan_vien_tiep_nhan
                WHERE pb.ma_phieu_bao_hanh = ?
                """,
                (rs, rowNum) -> mapDetail(rs),
                ticketId
        );
        if (result.isEmpty()) {
            throw new NoSuchElementException("Không tìm thấy phiếu bảo hành.");
        }
        Map<String, Object> detail = result.get(0);
        detail.put("lichSu", layLichSu(ticketId));
        return detail;
    }

    public List<Map<String, Object>> layLichSu(String ticketId) {
        requireTicket(ticketId);
        return jdbcTemplate.query(
                """
                SELECT ls.ma_lich_su, ls.trang_thai_moi, ls.ghi_chu_tien_do,
                       ls.thoi_gian_cap_nhat, ls.ma_nhan_vien_cap_nhat, nv.ho_ten
                FROM lich_su_bao_hanh ls
                LEFT JOIN nhan_vien nv ON nv.ma_nhan_vien = ls.ma_nhan_vien_cap_nhat
                WHERE ls.ma_phieu_bao_hanh = ?
                ORDER BY ls.thoi_gian_cap_nhat, ls.ma_lich_su
                """,
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    String status = publicStatus(rs.getString("trang_thai_moi"));
                    String note = rs.getString("ghi_chu_tien_do");
                    row.put("maLichSu", rs.getInt("ma_lich_su"));
                    row.put("trangThai", status);
                    row.put("ghiChuTienDo", note);
                    row.put("thoiGianCapNhat", localDateTime(rs.getTimestamp("thoi_gian_cap_nhat")));
                    row.put("maNhanVien", rs.getString("ma_nhan_vien_cap_nhat"));
                    row.put("tenNhanVien", rs.getString("ho_ten"));
                    return row;
                },
                ticketId
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> capNhatTienDo(String ticketId, WarrantyProgressRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin cập nhật tiến độ.");
        }
        String status = normalizeStatus(request.trangThaiMoi());
        String employeeId = required(request.maNhanVien(), "Mã nhân viên kỹ thuật");
        if (!List.of("Dang_kiem_tra", "Dang_sua_chua", "Cho_tra_khach", "Tu_choi").contains(status)) {
            throw new IllegalArgumentException("Trạng thái tiến độ không hợp lệ.");
        }
        requireEmployee(employeeId);
        Map<String, Object> current = timChiTiet(ticketId);
        String currentStatus = String.valueOf(current.get("trangThai"));
        if (isTerminal(currentStatus)) {
            throw new IllegalArgumentException("Phiếu bảo hành đã kết thúc.");
        }

        String diagnosis = packDiagnosis(request.ketQuaKiemTra(), request.nguyenNhanLoi());
        String oldNotes = jdbcTemplate.queryForObject(
                "SELECT ghi_chu FROM phieu_bao_hanh WHERE ma_phieu_bao_hanh = ?",
                String.class,
                ticketId
        );
        String notes = unpackNoteData(oldNotes,
                (String) current.get("yeuCauSuaChua"), request.ghiChu());
        String storedStatus = databaseStatus(status);
        int historyBefore = countHistory(ticketId);
        jdbcTemplate.update(
                """
                UPDATE phieu_bao_hanh
                SET chan_doan_ky_thuat = ?, noi_dung_sua_chua = ?, ghi_chu = ?,
                    trang_thai = ?, ma_ky_thuat_vien = ?
                WHERE ma_phieu_bao_hanh = ?
                """,
                diagnosis, emptyToNull(request.noiDungSuaChua()), notes, storedStatus,
                employeeId, ticketId
        );
        if (countHistory(ticketId) == historyBefore) {
            insertHistory(ticketId, employeeId, storedStatus,
                    progressNote(status, request));
        }
        if ("Hoan_tat".equals(storedStatus)) {
            ImeiInfo info = findImei((String) current.get("maImei"));
            jdbcTemplate.update("UPDATE imei_san_pham SET trang_thai = ? WHERE ma_imei = ?",
                    info != null && info.invoiceId() == null ? "Trong_kho" : "Da_ban",
                    current.get("maImei"));
        }
        return timChiTiet(ticketId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> hoanTat(String ticketId, WarrantyCloseRequest request) {
        String employeeId = required(request == null ? null : request.maNhanVien(),
                "Mã nhân viên bàn giao");
        requireEmployee(employeeId);
        Map<String, Object> current = timChiTiet(ticketId);
        if (!"Cho_tra_khach".equals(current.get("trangThai"))) {
            throw new IllegalArgumentException("Chỉ có thể hoàn tất phiếu đang chờ trả khách.");
        }

        int historyBefore = countHistory(ticketId);
        jdbcTemplate.update(
                """
                UPDATE phieu_bao_hanh
                SET trang_thai = 'Hoan_tat', ngay_tra_thuc_te = ?, ma_ky_thuat_vien = ?
                WHERE ma_phieu_bao_hanh = ?
                """,
                Timestamp.valueOf(LocalDateTime.now()), employeeId, ticketId
        );
        if (countHistory(ticketId) == historyBefore) {
            insertHistory(ticketId, employeeId, "Hoan_tat",
                    "Đã bàn giao lại thiết bị cho khách hàng, hoàn tất quy trình bảo hành");
        }
        ImeiInfo info = findImei((String) current.get("maImei"));
        jdbcTemplate.update("UPDATE imei_san_pham SET trang_thai = ? WHERE ma_imei = ?",
                info != null && info.invoiceId() == null ? "Trong_kho" : "Da_ban",
                current.get("maImei"));
        return timChiTiet(ticketId);
    }

    private Map<String, Object> mapListItem(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        String status = publicStatus(rs.getString("trang_thai"));
        row.put("maPhieuBh", rs.getString("ma_phieu_bao_hanh"));
        row.put("maKhachHang", rs.getString("ma_khach_hang"));
        row.put("tenKhachHang", rs.getString("ten_khach_hang"));
        row.put("soDienThoai", rs.getString("so_dien_thoai"));
        row.put("maImei", rs.getString("ma_imei"));
        row.put("tenThietBi", rs.getString("ten_san_pham"));
        row.put("loiPhanAnh", rs.getString("loi_khach_phan_anh"));
        row.put("ngayTiepNhan", localDateTime(rs.getTimestamp("ngay_tiep_nhan")));
        row.put("trangThai", status);
        return row;
    }

    private Map<String, Object> mapDetail(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        String status = publicStatus(rs.getString("trang_thai"));
        String storedNotes = rs.getString("ghi_chu");
        String storedDiagnosis = rs.getString("chan_doan_ky_thuat");
        String[] notes = unpackNotes(storedNotes);
        String[] diagnosis = unpackDiagnosis(storedDiagnosis);
        row.put("maPhieuBh", rs.getString("ma_phieu_bao_hanh"));
        row.put("maKhachHang", rs.getString("ma_khach_hang"));
        row.put("tenKhachHang", rs.getString("ten_khach_hang"));
        row.put("soDienThoai", rs.getString("so_dien_thoai"));
        row.put("maImei", rs.getString("ma_imei"));
        row.put("tenThietBi", rs.getString("ten_san_pham"));
        row.put("loiPhanAnh", rs.getString("loi_khach_phan_anh"));
        row.put("tinhTrangTiepNhan", rs.getString("tinh_trang_tiep_nhan"));
        row.put("yeuCauSuaChua", notes[0]);
        row.put("ketQuaKiemTra", diagnosis[0]);
        row.put("nguyenNhanLoi", diagnosis[1]);
        row.put("noiDungSuaChua", rs.getString("noi_dung_sua_chua"));
        row.put("ghiChu", notes[1]);
        row.put("ngayTiepNhan", localDateTime(rs.getTimestamp("ngay_tiep_nhan")));
        row.put("ngayCapNhat", localDateTime(rs.getTimestamp("ngay_cap_nhat")));
        row.put("ngayBanGiao", localDateTime(rs.getTimestamp("ngay_tra_thuc_te")));
        row.put("trangThai", status);
        row.put("maNhanVienTiepNhan", rs.getString("ma_nhan_vien_tiep_nhan"));
        row.put("tenNhanVienTiepNhan", rs.getString("ten_nhan_vien_tiep_nhan"));
        row.put("maNhanVienCapNhat", rs.getString("ma_nhan_vien_cap_nhat"));
        row.put("tenNhanVienCapNhat", rs.getString("ten_nhan_vien_cap_nhat"));
        return row;
    }

    private ImeiInfo findImei(String imei) {
        List<ImeiInfo> result = jdbcTemplate.query(
                """
                SELECT im.ma_imei, im.ma_san_pham, im.ma_hoa_don, im.trang_thai,
                       im.ngay_ban, im.han_bao_hanh, sp.ten_san_pham,
                       hd.ma_khach_hang, kh.ho_ten, kh.so_dien_thoai
                FROM imei_san_pham im
                JOIN san_pham sp ON sp.ma_san_pham = im.ma_san_pham
                LEFT JOIN hoa_don hd ON hd.ma_hoa_don = im.ma_hoa_don
                LEFT JOIN khach_hang kh ON kh.ma_khach_hang = hd.ma_khach_hang
                WHERE im.ma_imei = ?
                """,
                (rs, rowNum) -> new ImeiInfo(
                        rs.getString("ma_imei"),
                        rs.getString("ma_san_pham"),
                        rs.getString("ma_hoa_don"),
                        rs.getString("trang_thai"),
                        localDateTime(rs.getTimestamp("ngay_ban")),
                        localDate(rs.getDate("han_bao_hanh")),
                        rs.getString("ten_san_pham"),
                        rs.getString("ma_khach_hang"),
                        rs.getString("ho_ten"),
                        rs.getString("so_dien_thoai")
                ),
                imei
        );
        return result.isEmpty() ? null : result.get(0);
    }

    private Map<String, Object> checkResult(boolean found, String reason, ImeiInfo info) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", found);
        result.put("lyDo", reason);
        result.put("conBaoHanh", info != null && "Da_ban".equals(info.status())
                && info.warrantyEnd() != null && !info.warrantyEnd().isBefore(LocalDate.now()));
        if (info != null) {
            result.put("trangThaiImei", info.status());
            result.put("tenSanPham", info.productName());
            result.put("maImei", info.imei());
            result.put("maKhachHang", info.customerId());
            result.put("tenKhachHang", info.customerName());
            result.put("soDienThoai", info.customerPhone());
            result.put("ngayBan", info.saleDate());
            result.put("hanBaoHanh", info.warrantyEnd());
        }
        return result;
    }

    private boolean hasActiveTicket(String imei) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM phieu_bao_hanh
                WHERE ma_imei = ? AND trang_thai NOT IN ('Hoan_tat', 'Tu_choi', 'Tu_choi_bao_hanh')
                """,
                Integer.class,
                imei
        );
        return count != null && count > 0;
    }

    private void requireEmployee(String employeeId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM nhan_vien WHERE ma_nhan_vien = ?",
                Integer.class,
                employeeId
        );
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Không tìm thấy nhân viên.");
        }
    }

    private void requireTicket(String ticketId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM phieu_bao_hanh WHERE ma_phieu_bao_hanh = ?",
                Integer.class,
                ticketId
        );
        if (count == null || count == 0) {
            throw new NoSuchElementException("Không tìm thấy phiếu bảo hành.");
        }
    }

    private int countHistory(String ticketId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lich_su_bao_hanh WHERE ma_phieu_bao_hanh = ?",
                Integer.class,
                ticketId
        );
        return count == null ? 0 : count;
    }

    private void insertHistory(String ticketId, String employeeId, String status, String note) {
        jdbcTemplate.update(
                """
                INSERT INTO lich_su_bao_hanh (
                    ma_phieu_bao_hanh, ma_nhan_vien_cap_nhat,
                    thoi_gian_cap_nhat, trang_thai_moi, ghi_chu_tien_do
                ) VALUES (?, ?, ?, ?, ?)
                """,
                ticketId, employeeId, Timestamp.valueOf(LocalDateTime.now()),
                status, note == null || note.isBlank() ? "Cập nhật tiến độ bảo hành" : note
        );
    }

    private String createTicketId() {
        String datePrefix = "PBH" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "_";
        List<String> ids = jdbcTemplate.queryForList(
                "SELECT ma_phieu_bao_hanh FROM phieu_bao_hanh WHERE ma_phieu_bao_hanh LIKE ?",
                String.class,
                datePrefix + "%"
        );
        BigInteger max = BigInteger.ZERO;
        for (String id : ids) {
            String suffix = id.substring(datePrefix.length());
            if (suffix.matches("[0-9]+")) {
                max = max.max(new BigInteger(suffix));
            }
        }
        return datePrefix + String.format("%02d", max.add(BigInteger.ONE).intValue());
    }

    private String normalizeStatus(String value) {
        return value == null ? "" : value.trim();
    }

    private String databaseStatus(String status) {
        return status;
    }

    private String publicStatus(String storedStatus) {
        if ("Tu_choi_bao_hanh".equals(storedStatus)) {
            return "Tu_choi";
        }
        return storedStatus;
    }

    private String progressNote(String status, WarrantyProgressRequest request) {
        StringBuilder note = new StringBuilder();
        note.append("Trạng thái: ").append(status);
        if (hasText(request.ketQuaKiemTra())) {
            note.append(" | Kết quả kiểm tra: ").append(request.ketQuaKiemTra().trim());
        }
        if (hasText(request.nguyenNhanLoi())) {
            note.append(" | Nguyên nhân lỗi: ").append(request.nguyenNhanLoi().trim());
        }
        if (hasText(request.noiDungSuaChua())) {
            note.append(" | Nội dung sửa chữa: ").append(request.noiDungSuaChua().trim());
        }
        if (hasText(request.ghiChu())) {
            note.append(" | ").append(request.ghiChu().trim());
        }
        return note.toString();
    }

    private String packDiagnosis(String result, String cause) {
        StringBuilder value = new StringBuilder();
        if (hasText(result)) {
            value.append(CHECK_RESULT_MARKER).append(result.trim());
        }
        if (hasText(cause)) {
            value.append(CAUSE_MARKER).append(cause.trim());
        }
        return value.length() == 0 ? null : value.toString();
    }

    private String[] unpackDiagnosis(String value) {
        if (value == null || (!value.contains(CHECK_RESULT_MARKER) && !value.contains(CAUSE_MARKER))) {
            return new String[]{value, null};
        }
        return new String[]{
                markerValue(value, CHECK_RESULT_MARKER, CAUSE_MARKER),
                markerValue(value, CAUSE_MARKER)
        };
    }

    private String packNotes(String request, String note) {
        StringBuilder value = new StringBuilder();
        if (hasText(request)) {
            value.append(REQUEST_MARKER).append(request.trim());
        }
        if (hasText(note)) {
            value.append(NOTE_MARKER).append(note.trim());
        }
        return value.length() == 0 ? null : value.toString();
    }

    private String unpackNoteData(String currentStored, String currentRequest, String note) {
        String[] old = unpackNotes(currentStored);
        String request = currentRequest != null ? currentRequest : old[0];
        return packNotes(request, note);
    }

    private String[] unpackNotes(String value) {
        if (value == null) {
            return new String[]{null, null};
        }
        if (!value.contains(REQUEST_MARKER) && !value.contains(NOTE_MARKER)) {
            return new String[]{null, value};
        }
        return new String[]{
                markerValue(value, REQUEST_MARKER, NOTE_MARKER),
                markerValue(value, NOTE_MARKER)
        };
    }

    private String markerValue(String source, String marker) {
        return markerValue(source, marker, null);
    }

    private String markerValue(String source, String marker, String nextMarker) {
        int start = source.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = nextMarker == null ? source.length() : source.indexOf(nextMarker, start);
        if (end < 0) {
            end = source.length();
        }
        String value = source.substring(start, end).trim();
        return value.isEmpty() ? null : value;
    }

    private boolean containsAny(Map<String, Object> ticket, String keyword) {
        for (String key : List.of("maPhieuBh", "tenKhachHang", "soDienThoai", "maImei", "tenThietBi")) {
            Object value = ticket.get(key);
            if (value != null && value.toString().toLowerCase(Locale.ROOT).contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isTerminal(String status) {
        return "Hoan_tat".equals(status) || "Tu_choi".equals(status)
                || "Tu_choi_bao_hanh".equals(status);
    }

    private String required(String value, String label) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(label + " không được để trống.");
        }
        return value.trim();
    }

    private String emptyToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private LocalDateTime localDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private LocalDate localDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private record ImeiInfo(
            String imei,
            String productId,
            String invoiceId,
            String status,
            LocalDateTime saleDate,
            LocalDate warrantyEnd,
            String productName,
            String customerId,
            String customerName,
            String customerPhone
    ) {
    }
}
