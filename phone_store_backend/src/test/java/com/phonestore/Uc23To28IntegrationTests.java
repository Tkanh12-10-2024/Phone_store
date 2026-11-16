package com.phonestore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Uc23To28IntegrationTests {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void uc23PromotionCrudAlertsAndValidations() throws Exception {
                // 1. Auto alerts (< 24h remaining and < 5% quota left)
                MvcResult alertsRes = mockMvc.perform(get("/api/promotions/alerts"))
                                .andExpect(status().isOk())
                                .andReturn();
                JsonNode alerts = objectMapper
                                .readTree(alertsRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
                assertTrue(alerts.size() >= 2, "Phải phát hiện ít nhất 2 khuyến mãi sắp hết hạn / sắp hết số lượng");

                // 2. Create valid promotion
                DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                LocalDateTime now = LocalDateTime.now();
                Map<String, Object> createPayload = Map.ofEntries(
                                Map.entry("codeVoucher", "TEST_NOEL_26"),
                                Map.entry("tenKhuyenMai", "Ưu đãi Noel Test"),
                                Map.entry("loaiGiamGia", "Phan_tram"),
                                Map.entry("giaTriGiam", new BigDecimal("15")),
                                Map.entry("giaTriDonHangToiThieu", new BigDecimal("5000000")),
                                Map.entry("giamToiDa", new BigDecimal("1000000")),
                                Map.entry("soLuongPhatHanh", 50),
                                Map.entry("ngayBatDau", fmt.format(now.minusHours(1))),
                                Map.entry("ngayKetThuc", fmt.format(now.plusDays(10))),
                                Map.entry("doiTuongKhachHang", "Tat_ca"),
                                Map.entry("trangThai", "Dang_dien_ra"));

                MvcResult createdRes = mockMvc.perform(post("/api/promotions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createPayload)))
                                .andExpect(status().isCreated())
                                .andReturn();
                JsonNode created = objectMapper
                                .readTree(createdRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
                String maKm = created.get("maKhuyenMai").asText();
                assertEquals("TEST_NOEL_26", created.get("codeVoucher").asText());

                // 3. Duplicate code rejected
                mockMvc.perform(post("/api/promotions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createPayload)))
                                .andExpect(status().isBadRequest());

                // 4. Invalid end < start rejected
                Map<String, Object> invalidDates = Map.ofEntries(
                                Map.entry("codeVoucher", "BAD_DATE_26"),
                                Map.entry("tenKhuyenMai", "Sai ngày"),
                                Map.entry("loaiGiamGia", "Tien_mat"),
                                Map.entry("giaTriGiam", new BigDecimal("100000")),
                                Map.entry("soLuongPhatHanh", 10),
                                Map.entry("ngayBatDau", fmt.format(now.plusDays(5))),
                                Map.entry("ngayKetThuc", fmt.format(now)));
                mockMvc.perform(post("/api/promotions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidDates)))
                                .andExpect(status().isBadRequest());

                // 5. Pause & edit promotion, then delete unused promotion
                mockMvc.perform(put("/api/promotions/" + maKm + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"trangThai\":\"Tam_dung\"}"))
                                .andExpect(status().isOk());

                MvcResult delRes = mockMvc.perform(delete("/api/promotions/" + maKm))
                                .andExpect(status().isOk())
                                .andReturn();
                JsonNode delJson = objectMapper
                                .readTree(delRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
                assertTrue(delJson.get("success").asBoolean());

                // 6. Deleting used promotion safely expires it instead of breaking FK
                MvcResult usedDelRes = mockMvc.perform(delete("/api/promotions/KM_CHAOTHU2026"))
                                .andExpect(status().isOk())
                                .andReturn();
                assertTrue(objectMapper.readTree(usedDelRes.getResponse().getContentAsString(StandardCharsets.UTF_8))
                                .get("message").asText().contains("Hết hạn"));

                // Restore KM_CHAOTHU2026 status for subsequent tests
                mockMvc.perform(put("/api/promotions/KM_CHAOTHU2026/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"trangThai\":\"Dang_dien_ra\"}"))
                                .andExpect(status().isOk());
        }

        @Test
        void uc24PosVoucherValidationAndCheckout() throws Exception {
                // 1. Valid Apple 5% voucher on iPhone 15 Pro Max (29,990,000 * 5% = 1,499,500)
                Map<String, Object> validReq = Map.of(
                                "codeVoucher", "APPLEVip5",
                                "maKhachHang", "KH001",
                                "vatPhanTram", BigDecimal.ZERO,
                                "items", List.of(Map.of(
                                                "maSanPham", "SP_IP15PM_256TN",
                                                "soLuong", 1,
                                                "donGia", new BigDecimal("29990000"))));
                MvcResult validRes = mockMvc.perform(post("/api/pos/apply-voucher")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validReq)))
                                .andExpect(status().isOk())
                                .andReturn();
                JsonNode validJson = objectMapper
                                .readTree(validRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
                assertTrue(validJson.get("valid").asBoolean());
                assertEquals(0, new BigDecimal("1499500").compareTo(validJson.get("soTienGiam").decimalValue()));
                assertEquals(0, new BigDecimal("28490500").compareTo(validJson.get("tongThanhToan").decimalValue()));

                // 2. Brand mismatch (APPLEVip5 applied to Samsung cart) keeps total untouched
                Map<String, Object> mismatchReq = Map.of(
                                "codeVoucher", "APPLEVip5",
                                "maKhachHang", "KH001",
                                "vatPhanTram", BigDecimal.ZERO,
                                "items", List.of(Map.of(
                                                "maSanPham", "SP_SS_S24U_256AM",
                                                "soLuong", 1,
                                                "donGia", new BigDecimal("27990000"))));
                MvcResult mismatchRes = mockMvc.perform(post("/api/pos/apply-voucher")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(mismatchReq)))
                                .andExpect(status().isOk())
                                .andReturn();
                JsonNode mismatchJson = objectMapper
                                .readTree(mismatchRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
                assertFalse(mismatchJson.get("valid").asBoolean());
                assertEquals("BRAND_MISMATCH", mismatchJson.get("errorCode").asText());
                assertEquals(0, BigDecimal.ZERO.compareTo(mismatchJson.get("soTienGiam").decimalValue()));
                assertEquals(0, new BigDecimal("27990000").compareTo(mismatchJson.get("tongThanhToan").decimalValue()));

                // 3. Expired voucher rejected cleanly
                Map<String, Object> expiredReq = Map.of(
                                "codeVoucher", "HET_HAN_2025",
                                "maKhachHang", "KH_LE",
                                "items", List.of(Map.of("maSanPham", "SP_OPPO_RENO11_256", "soLuong", 1)));
                JsonNode expJson = objectMapper.readTree(mockMvc.perform(post("/api/pos/apply-voucher")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(expiredReq)))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
                assertFalse(expJson.get("valid").asBoolean());
                assertEquals("EXPIRED", expJson.get("errorCode").asText());

                // 4. VIP-only voucher rejected for walk-in customer
                Map<String, Object> vipFailReq = Map.of(
                                "codeVoucher", "VIPSAP97",
                                "maKhachHang", "KH_LE",
                                "items", List.of(Map.of("maSanPham", "SP_SS_S24U_256AM", "soLuong", 1)));
                JsonNode vipFailJson = objectMapper.readTree(mockMvc.perform(post("/api/pos/apply-voucher")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(vipFailReq)))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
                assertFalse(vipFailJson.get("valid").asBoolean());
                assertEquals("CUSTOMER_TIER_MISMATCH", vipFailJson.get("errorCode").asText());

                // 5. Real POS Checkout updates stock & creates invoice
                MvcResult beforeProds = mockMvc.perform(get("/api/pos/products")).andExpect(status().isOk())
                                .andReturn();
                int initialStock = 0;
                for (JsonNode p : objectMapper
                                .readTree(beforeProds.getResponse().getContentAsString(StandardCharsets.UTF_8))) {
                        if ("SP_SS_S24U_256AM".equals(p.get("maSanPham").asText())) {
                                initialStock = p.get("soLuongTon").asInt();
                        }
                }
                assertTrue(initialStock >= 2);

                Map<String, Object> checkoutPayload = Map.of(
                                "maKhachHang", "KH001",
                                "maNhanVien", "NV003",
                                "codeVoucher", "FLASH12H",
                                "phuongThucThanhToan", "Tien_mat",
                                "vatPhanTram", BigDecimal.ZERO,
                                "tienKhachDua", new BigDecimal("28000000"),
                                "items", List.of(Map.of(
                                                "maSanPham", "SP_SS_S24U_256AM",
                                                "soLuong", 1,
                                                "donGia", new BigDecimal("27990000"))));
                MvcResult coRes = mockMvc.perform(post("/api/pos/checkout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(checkoutPayload)))
                                .andExpect(status().isOk())
                                .andReturn();
                JsonNode coJson = objectMapper.readTree(coRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
                assertTrue(coJson.get("maHoaDon").asText().startsWith("HD"));
                assertEquals(0, new BigDecimal("1200000").compareTo(coJson.get("tienGiamGia").decimalValue()));
                assertEquals(0, new BigDecimal("26790000").compareTo(coJson.get("tongThanhToan").decimalValue()));

                MvcResult afterProds = mockMvc.perform(get("/api/pos/products")).andExpect(status().isOk()).andReturn();
                int afterStock = 0;
                for (JsonNode p : objectMapper
                                .readTree(afterProds.getResponse().getContentAsString(StandardCharsets.UTF_8))) {
                        if ("SP_SS_S24U_256AM".equals(p.get("maSanPham").asText())) {
                                afterStock = p.get("soLuongTon").asInt();
                        }
                }
                assertEquals(initialStock - 1, afterStock);
        }

        @Test
        void uc25To27StatisticsEndpointsAndEdgeCases() throws Exception {
                // UC-25 Revenue by Month + Invalid date validation + Empty period message
                MvcResult revRes = mockMvc.perform(get("/api/statistics/revenue")
                                .param("tuNgay", "2026-09-01")
                                .param("denNgay", "2026-12-31")
                                .param("groupBy", "MONTH"))
                                .andExpect(status().isOk())
                                .andReturn();
                JsonNode revJson = objectMapper
                                .readTree(revRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
                assertTrue(revJson.get("tongHoaDon").asInt() >= 8);
                assertTrue(revJson.get("doanhThuThucThu").decimalValue().compareTo(BigDecimal.ZERO) > 0);
                assertTrue(revJson.get("loiNhuanGop").decimalValue().compareTo(BigDecimal.ZERO) > 0);
                assertTrue(revJson.get("series").size() >= 3);

                mockMvc.perform(get("/api/statistics/revenue")
                                .param("tuNgay", "2026-12-31")
                                .param("denNgay", "2026-01-01"))
                                .andExpect(status().isBadRequest());

                JsonNode emptyRev = objectMapper.readTree(mockMvc.perform(get("/api/statistics/revenue")
                                .param("tuNgay", "2099-01-01")
                                .param("denNgay", "2099-01-31"))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
                assertEquals(0, emptyRev.get("tongHoaDon").asInt());
                assertEquals("Không có doanh thu phát sinh trong khoảng thời gian này",
                                emptyRev.get("emptyMessage").asText());

                // UC-26 Products (Best Sellers & >60 days Stagnant Stock)
                JsonNode prodStats = objectMapper.readTree(mockMvc.perform(get("/api/statistics/products")
                                .param("tuNgay", "2026-09-01")
                                .param("denNgay", "2026-12-31"))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
                assertTrue(prodStats.get("bestSellers").size() > 0);
                boolean foundStagnantFindN3 = false;
                for (JsonNode st : prodStats.get("stagnantProducts")) {
                        if ("SP_OPPO_FINDN3".equals(st.get("maSanPham").asText())
                                        && st.get("soNgayTonKho").asLong() > 60) {
                                foundStagnantFindN3 = true;
                        }
                }
                assertTrue(foundStagnantFindN3,
                                "SP_OPPO_FINDN3 nhập > 60 ngày chưa xuất phải nằm trong danh sách tồn đọng");

                // UC-27 Inventory & Alerts
                JsonNode invStats = objectMapper.readTree(mockMvc.perform(get("/api/statistics/inventory"))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
                assertTrue(invStats.get("tongDangGiuCho").asInt() >= 2,
                                "Phải hiển thị số lượng đang giữ chỗ từ đơn Cho_thanh_toan");
                assertTrue(invStats.get("soSanPhamCanhBao").asInt() >= 3, "Phải có cảnh báo tồn kho thấp/hết hàng");
                assertTrue(invStats.get("movements").size() > 0);
        }

        @Test
        void uc28ReportPreviewExcelPdfExportAndAuditLog() throws Exception {
                Map<String, Object> req = Map.of(
                                "loaiBaoCao", "Tong_hop",
                                "tuNgay", "2026-09-01",
                                "denNgay", "2026-12-31",
                                "maNhanVien", "NV001",
                                "ghiChu", "Kiểm thử tích hợp UC-28");

                // 1. Preview
                JsonNode prev = objectMapper.readTree(mockMvc.perform(post("/api/reports/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
                assertTrue(prev.get("columns").size() >= 6);
                assertTrue(prev.get("rows").size() >= 5);

                // 2. Excel (.xlsx starts with PK zip signature)
                MvcResult excelRes = mockMvc.perform(post("/api/reports/export/excel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type",
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .andReturn();
                byte[] xlsxBytes = excelRes.getResponse().getContentAsByteArray();
                assertTrue(xlsxBytes.length > 1000);
                assertEquals('P', (char) xlsxBytes[0]);
                assertEquals('K', (char) xlsxBytes[1]);

                // 3. PDF (.pdf starts with %PDF-)
                MvcResult pdfRes = mockMvc.perform(post("/api/reports/export/pdf")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                                .andReturn();
                byte[] pdfBytes = pdfRes.getResponse().getContentAsByteArray();
                assertTrue(pdfBytes.length > 1000);
                String pdfHeader = new String(pdfBytes, 0, 5);
                assertEquals("%PDF-", pdfHeader);

                // 4. Verify nhat_ky_bao_cao logged both exports
                JsonNode history = objectMapper.readTree(mockMvc.perform(get("/api/reports/history"))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
                assertTrue(history.size() >= 4);
        }
}
