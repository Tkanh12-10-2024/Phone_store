package com.phonestore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PromotionManagementIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void promotionListOptionsAndAlertsSupportManagementPage() throws Exception {
        JsonNode list = objectMapper.readTree(mockMvc.perform(get("/api/promotions"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertTrue(list.size() > 0);
        assertTrue(list.get(0).has("soLuongConLai"));
        assertTrue(list.get(0).has("tenThuongHieu"));

        JsonNode filtered = objectMapper.readTree(mockMvc.perform(get("/api/promotions")
                        .param("keyword", "APPLEVip5")
                        .param("trangThai", "Dang_dien_ra")
                        .param("loaiGiamGia", "Phan_tram"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals(1, filtered.size());
        assertEquals("APPLEVip5", filtered.get(0).get("codeVoucher").asText());

        JsonNode options = objectMapper.readTree(mockMvc.perform(get("/api/promotions/options"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertTrue(options.get("brands").size() > 0);
        assertTrue(options.get("categories").size() > 0);
        assertTrue(options.get("productLines").size() > 0);
        assertTrue(options.get("brands").get(0).has("value"));
        assertTrue(options.get("brands").get(0).has("label"));

        JsonNode alerts = objectMapper.readTree(mockMvc.perform(get("/api/promotions/alerts"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertTrue(alerts.size() >= 2);
        assertTrue(alerts.get(0).get("coCanhBao").asBoolean());
        assertFalse(alerts.get(0).get("lyDoCanhBao").asText().isBlank());
    }

    @Test
    void createUpdateStatusDeleteAndPosVoucherListRemainCompatible() throws Exception {
        JsonNode posVouchers = objectMapper.readTree(mockMvc.perform(get("/api/pos/vouchers"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertTrue(posVouchers.isArray());
        assertTrue(containsVoucher(posVouchers, "APPLEVip5"));

        String code = "IT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> payload = Map.ofEntries(
                Map.entry("codeVoucher", code),
                Map.entry("tenKhuyenMai", "Promotion API integration"),
                Map.entry("loaiGiamGia", "Phan_tram"),
                Map.entry("giaTriGiam", new BigDecimal("10")),
                Map.entry("giaTriDonHangToiThieu", new BigDecimal("100000")),
                Map.entry("soLuongPhatHanh", 25),
                Map.entry("ngayBatDau", formatter.format(now.minusHours(1))),
                Map.entry("ngayKetThuc", formatter.format(now.plusDays(2))),
                Map.entry("maThuongHieuApDung", 1),
                Map.entry("maDanhMucApDung", 1),
                Map.entry("doiTuongKhachHang", "Tat_ca"),
                Map.entry("trangThai", "Dang_dien_ra"));

        JsonNode created = objectMapper.readTree(mockMvc.perform(post("/api/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        String id = created.get("maKhuyenMai").asText();
        assertEquals(code, created.get("codeVoucher").asText());
        assertEquals(1, created.get("maThuongHieuApDung").asInt());

        Map<String, Object> update = Map.ofEntries(
                Map.entry("codeVoucher", code),
                Map.entry("tenKhuyenMai", "Promotion API integration updated"),
                Map.entry("loaiGiamGia", "Phan_tram"),
                Map.entry("giaTriGiam", new BigDecimal("12")),
                Map.entry("giaTriDonHangToiThieu", new BigDecimal("150000")),
                Map.entry("soLuongPhatHanh", 30),
                Map.entry("ngayBatDau", formatter.format(now.minusHours(1))),
                Map.entry("ngayKetThuc", formatter.format(now.plusDays(3))),
                Map.entry("maThuongHieuApDung", 1),
                Map.entry("maDanhMucApDung", 1),
                Map.entry("doiTuongKhachHang", "Thanh_vien"),
                Map.entry("trangThai", "Dang_dien_ra"));
        JsonNode updated = objectMapper.readTree(mockMvc.perform(put("/api/promotions/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals("Promotion API integration updated", updated.get("tenKhuyenMai").asText());
        assertEquals(0, new BigDecimal("12").compareTo(updated.get("giaTriGiam").decimalValue()));

        mockMvc.perform(put("/api/promotions/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThai\":\"Tam_dung\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/promotions/" + id))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/promotions/" + id))
                .andExpect(status().isNotFound());

        JsonNode usedDelete = objectMapper.readTree(mockMvc.perform(delete("/api/promotions/KM_CHAOTHU2026"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertTrue(usedDelete.get("message").asText().contains("Hết hạn"));
        mockMvc.perform(put("/api/promotions/KM_CHAOTHU2026/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThai\":\"Dang_dien_ra\"}"))
                .andExpect(status().isOk());
    }

    private boolean containsVoucher(JsonNode vouchers, String code) {
        for (JsonNode voucher : vouchers) {
            if (code.equals(voucher.path("codeVoucher").asText())) {
                return true;
            }
        }
        return false;
    }
}
