package com.phonestore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class WarrantyManagementIntegrationTests {

    private static final String TEST_IMEI = "358941091234563";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String createdTicketId;

    @AfterEach
    void cleanUpTicket() {
        if (createdTicketId != null) {
            jdbcTemplate.update("DELETE FROM lich_su_bao_hanh WHERE ma_phieu_bao_hanh = ?", createdTicketId);
            jdbcTemplate.update("DELETE FROM phieu_bao_hanh WHERE ma_phieu_bao_hanh = ?", createdTicketId);
        }
        jdbcTemplate.update("UPDATE imei_san_pham SET trang_thai = 'Trong_kho' WHERE ma_imei = ?", TEST_IMEI);
    }

    @Test
    void receivesWarehouseImeiTracksProgressAndCompletesTicket() throws Exception {
        JsonNode check = json(mockMvc.perform(post("/api/warranty/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("imei", TEST_IMEI))))
                .andExpect(status().isOk())
                .andReturn());
        assertTrue(check.get("found").asBoolean());
        assertFalse(check.get("conBaoHanh").asBoolean());
        assertEquals("Trong_kho", check.get("trangThaiImei").asText());
        assertFalse(check.hasNonNull("maKhachHang"));

        Map<String, Object> receive = new HashMap<>();
        receive.put("maKhachHang", null);
        receive.put("maImei", TEST_IMEI);
        receive.put("tenThietBi", "ignored client-side product name");
        receive.put("tinhTrangTiepNhan", "Vỏ máy còn nguyên");
        receive.put("loiPhanAnh", "Camera bị rung");
        receive.put("yeuCauSuaChua", "Kiểm tra camera");
        receive.put("maNhanVien", "NV003");
        MvcResult receivedResult = mockMvc.perform(post("/api/warranty")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receive)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode received = json(receivedResult);
        createdTicketId = received.get("maPhieuBh").asText();
        assertEquals("Tiep_nhan", received.get("trangThai").asText());
        assertEquals("SP_IP15PM_256TN", jdbcTemplate.queryForObject(
                "SELECT ma_san_pham FROM phieu_bao_hanh WHERE ma_phieu_bao_hanh = ?",
                String.class, createdTicketId));
        assertEquals("Dang_bao_hanh", jdbcTemplate.queryForObject(
                "SELECT trang_thai FROM imei_san_pham WHERE ma_imei = ?", String.class, TEST_IMEI));

        JsonNode blockedCheck = json(mockMvc.perform(post("/api/warranty/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("imei", TEST_IMEI))))
                .andExpect(status().isOk())
                .andReturn());
        assertFalse(blockedCheck.get("found").asBoolean());

        JsonNode filtered = json(mockMvc.perform(get("/api/warranty")
                        .param("keyword", TEST_IMEI)
                        .param("trangThai", "Tiep_nhan"))
                .andExpect(status().isOk())
                .andReturn());
        assertTrue(containsTicket(filtered, createdTicketId));

        Map<String, Object> progress = new HashMap<>();
        progress.put("ketQuaKiemTra", "Mô-tơ camera lỗi");
        progress.put("nguyenNhanLoi", "Cáp kết nối lỏng");
        progress.put("noiDungSuaChua", "Thay cụm camera");
        progress.put("ghiChu", "Đã báo khách");
        progress.put("trangThaiMoi", "Dang_kiem_tra");
        progress.put("maNhanVien", "NV003");
        JsonNode inspected = json(mockMvc.perform(put("/api/warranty/" + createdTicketId + "/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(progress)))
                .andExpect(status().isOk())
                .andReturn());
        assertEquals("Dang_kiem_tra", inspected.get("trangThai").asText());
        assertEquals("Kiểm tra camera", inspected.get("yeuCauSuaChua").asText());
        assertEquals("Mô-tơ camera lỗi", inspected.get("ketQuaKiemTra").asText());
        assertEquals("Cáp kết nối lỏng", inspected.get("nguyenNhanLoi").asText());
        assertEquals("Đã báo khách", inspected.get("ghiChu").asText());

        progress.put("trangThaiMoi", "Dang_sua_chua");
        mockMvc.perform(put("/api/warranty/" + createdTicketId + "/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(progress)))
                .andExpect(status().isOk());
        progress.put("trangThaiMoi", "Cho_tra_khach");
        mockMvc.perform(put("/api/warranty/" + createdTicketId + "/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(progress)))
                .andExpect(status().isOk());

        JsonNode closed = json(mockMvc.perform(put("/api/warranty/" + createdTicketId + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("maNhanVien", "NV003"))))
                .andExpect(status().isOk())
                .andReturn());
        assertEquals("Hoan_tat", closed.get("trangThai").asText());
        assertTrue(closed.hasNonNull("ngayBanGiao"));
        assertEquals("Trong_kho", jdbcTemplate.queryForObject(
                "SELECT trang_thai FROM imei_san_pham WHERE ma_imei = ?", String.class, TEST_IMEI));

        JsonNode history = json(mockMvc.perform(get("/api/warranty/" + createdTicketId + "/history"))
                .andExpect(status().isOk())
                .andReturn());
        assertEquals(5, history.size());
        assertEquals("Hoan_tat", history.get(history.size() - 1).get("trangThai").asText());

        JsonNode detail = json(mockMvc.perform(get("/api/warranty/" + createdTicketId))
                .andExpect(status().isOk())
                .andReturn());
        assertEquals(createdTicketId, detail.get("maPhieuBh").asText());
        assertEquals(5, detail.get("lichSu").size());
    }

    @Test
    void unknownImeiReturnsNotFoundEligibilityAndMissingTicketReturns404() throws Exception {
        JsonNode check = json(mockMvc.perform(post("/api/warranty/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("imei", "000000000000000"))))
                .andExpect(status().isOk())
                .andReturn());
        assertFalse(check.get("found").asBoolean());
        assertFalse(check.get("conBaoHanh").asBoolean());

        mockMvc.perform(get("/api/warranty/UNKNOWN_TICKET"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/warranty/UNKNOWN_TICKET/history"))
                .andExpect(status().isNotFound());
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private boolean containsTicket(JsonNode tickets, String ticketId) {
        for (JsonNode ticket : tickets) {
            if (ticketId.equals(ticket.path("maPhieuBh").asText())) {
                return true;
            }
        }
        return false;
    }
}
