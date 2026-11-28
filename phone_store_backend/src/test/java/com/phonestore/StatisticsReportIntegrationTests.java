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

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StatisticsReportIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void statisticsExposeRelationalRevenueProductAndInventoryData() throws Exception {
        JsonNode revenue = json(mockMvc.perform(get("/api/statistics/revenue")
                .param("tuNgay", "2026-09-01")
                .param("denNgay", "2026-12-31")
                .param("groupBy", "MONTH"))
                .andExpect(status().isOk()).andReturn());
        assertTrue(revenue.get("tongHoaDon").asInt() >= 8);
        assertTrue(revenue.get("doanhThuThucThu").decimalValue().signum() > 0);
        assertTrue(revenue.get("tongTienVon").decimalValue().signum() > 0);
        assertTrue(revenue.get("series").size() >= 3);

        JsonNode productStats = json(mockMvc.perform(get("/api/statistics/products")
                .param("tuNgay", "2026-09-01")
                .param("denNgay", "2026-12-31")
                .param("maThuongHieu", "4"))
                .andExpect(status().isOk()).andReturn());
        assertTrue(productStats.get("bestSellers").size() > 0);
        assertEquals("SP_OPPO_RENO11_256", productStats.get("bestSellers").get(0).get("maSanPham").asText());
        assertTrue(productStats.get("stagnantProducts").size() > 0);

        JsonNode inventory = json(mockMvc.perform(get("/api/statistics/inventory"))
                .andExpect(status().isOk()).andReturn());
        assertTrue(inventory.get("tongDangGiuCho").asInt() >= 2);
        assertTrue(inventory.get("soSanPhamCanhBao").asInt() >= 3);
        assertTrue(inventory.get("items").get(0).has("soLuongKhaDung"));
        assertTrue(inventory.get("movements").size() > 0);

        JsonNode filters = json(mockMvc.perform(get("/api/statistics/inventory/filters"))
                .andExpect(status().isOk()).andReturn());
        assertTrue(filters.get("brands").size() >= 4);
        assertTrue(filters.get("dongMay").size() > 0);
    }

    @Test
    void invalidDateRangeAndUnknownGroupingAreRejected() throws Exception {
        mockMvc.perform(get("/api/statistics/revenue")
                        .param("tuNgay", "2026-12-31")
                        .param("denNgay", "2026-01-01"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/statistics/revenue")
                        .param("tuNgay", "not-a-date"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/statistics/revenue").param("groupBy", "WEEK"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/reports/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loaiBaoCao\":\"Tong_hop\",\"tuNgay\":\"2026-12-31\",\"denNgay\":\"2026-01-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void previewAndExportsPersistDownloadHistory() throws Exception {
        Map<String, Object> request = Map.of(
                "loaiBaoCao", "Tong_hop",
                "tuNgay", "2026-09-01",
                "denNgay", "2026-12-31",
                "maNhanVien", "NV001",
                "ghiChu", "Focused report integration test");
        String payload = objectMapper.writeValueAsString(request);
        JsonNode preview = json(mockMvc.perform(post("/api/reports/preview")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andReturn());
        assertTrue(preview.get("columns").size() >= 6);
        assertTrue(preview.get("rows").size() >= 5);
        assertTrue(preview.has("summary"));
        assertEquals("Hoàng Mạnh Hà", preview.get("nguoiLap").asText());

        MvcResult excel = mockMvc.perform(post("/api/reports/export/excel")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();
        byte[] xlsx = excel.getResponse().getContentAsByteArray();
        assertTrue(xlsx.length > 1000);
        assertEquals('P', (char) xlsx[0]);
        assertEquals('K', (char) xlsx[1]);

        MvcResult pdf = mockMvc.perform(post("/api/reports/export/pdf")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andReturn();
        byte[] pdfBytes = pdf.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 1000);
        assertEquals("%PDF-", new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII));

        JsonNode history = json(mockMvc.perform(get("/api/reports/history").param("limit", "15"))
                .andExpect(status().isOk()).andReturn());
        assertTrue(history.size() >= 4);
        JsonNode recent = history.get(0);
        assertTrue(recent.hasNonNull("maBaoCao"));
        assertTrue(recent.hasNonNull("tieuChiLoc"));
        assertTrue(recent.hasNonNull("duongDanFile"));
        assertEquals("Tong_hop", recent.get("loaiBaoCao").asText());
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
