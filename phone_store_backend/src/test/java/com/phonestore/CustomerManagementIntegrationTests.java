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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerManagementIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void restoreSeedCustomer() {
        jdbcTemplate.update(
                "UPDATE khach_hang SET trang_thai = 'Hoat_dong' WHERE ma_khach_hang = 'KH001'"
        );
    }

    @Test
    void customerSearchIncludesSalesAndWarrantyHistory() throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/customers")
                        .param("keyword", "0981112233"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode customers = objectMapper.readTree(
                listResult.getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
        assertEquals(1, customers.size());
        assertEquals("KH001", customers.get(0).get("maKhachHang").asText());
        assertTrue(customers.get(0).get("soHoaDon").asInt() > 0);

        MvcResult historyResult = mockMvc.perform(get("/api/customers/KH001/history"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode history = objectMapper.readTree(
                historyResult.getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
        assertTrue(history.size() > 0);
        assertTrue(history.get(0).hasNonNull("maHoaDon"));
        assertTrue(history.get(0).hasNonNull("tenSanPham"));
        assertTrue(history.get(0).hasNonNull("maImei"));
    }

    @Test
    void createUpdateDuplicateAndHardDeleteCustomerWithoutHistory() throws Exception {
        Map<String, Object> createPayload = Map.of(
                "hoTen", "Khách hàng kiểm thử",
                "soDienThoai", "09120000000",
                "email", "customer.test@example.com",
                "diaChi", "Quận 1, TP.HCM"
        );
        MvcResult createResult = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = objectMapper.readTree(
                createResult.getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
        String customerId = created.get("maKhachHang").asText();
        assertEquals("Hoat_dong", created.get("trangThai").asText());
        assertEquals(0, created.get("diemTichLuy").asInt());

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "hoTen", "Trùng số điện thoại",
                                "soDienThoai", "0912 000 0000"
                        ))))
                .andExpect(status().isBadRequest());

        Map<String, Object> updatePayload = Map.of(
                "hoTen", "Khách hàng đã cập nhật",
                "soDienThoai", "09120000001",
                "email", "updated.customer@example.com",
                "diaChi", "Quận 3, TP.HCM"
        );
        MvcResult updateResult = mockMvc.perform(put("/api/customers/" + customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode updated = objectMapper.readTree(
                updateResult.getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
        assertEquals("Khách hàng đã cập nhật", updated.get("hoTen").asText());
        assertEquals("09120000001", updated.get("soDienThoai").asText());

        MvcResult deleteResult = mockMvc.perform(delete("/api/customers/" + customerId))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("DELETED",
                objectMapper.readTree(deleteResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                        .get("action").asText());
        mockMvc.perform(get("/api/customers/" + customerId))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerWithTransactionsIsDeactivatedInsteadOfDeleted() throws Exception {
        MvcResult deleteResult = mockMvc.perform(delete("/api/customers/KH001"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("DEACTIVATED",
                objectMapper.readTree(deleteResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                        .get("action").asText());

        MvcResult customerResult = mockMvc.perform(get("/api/customers/KH001"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("Ngung_hoat_dong",
                objectMapper.readTree(customerResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                        .get("trangThai").asText());

        MvcResult posCustomersResult = mockMvc.perform(get("/api/pos/customers"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode posCustomers = objectMapper.readTree(
                posCustomersResult.getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
        for (JsonNode customer : posCustomers) {
            assertNotEquals("KH001", customer.get("maKhachHang").asText());
        }
    }
}
