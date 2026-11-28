package com.phonestore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

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
class EmployeeAccountIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void employeeAccountCrudNeverReturnsPasswordAndDeleteIsSoft() throws Exception {
        Map<String, Object> createPayload = Map.of(
                "id", "NV_POS_TEST",
                "fullName", "Nhân viên POS kiểm thử",
                "phone", "0987654321",
                "username", "pos_test_user",
                "password", "test-password",
                "role", "NHAN_VIEN",
                "active", 1
        );

        JsonNode created = objectMapper.readTree(mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        assertEquals("NV_POS_TEST", created.get("id").asText());
        assertEquals("NHAN_VIEN", created.get("role").asText());
        assertFalse(created.has("password"));

        JsonNode edited = objectMapper.readTree(mockMvc.perform(put("/api/employees/NV_POS_TEST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Nhân viên POS kiểm thử đã sửa",
                                  "phone":"0987654321",
                                  "username":"pos_test_user",
                                  "role":"QUAN_LY",
                                  "active":0
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertEquals("QUAN_LY", edited.get("role").asText());
        assertEquals(0, edited.get("active").asInt());

        JsonNode activePosEmployees = objectMapper.readTree(
                mockMvc.perform(get("/api/pos/employees"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString()
        );
        for (JsonNode employee : activePosEmployees) {
            assertFalse("NV_POS_TEST".equals(employee.get("maNhanVien").asText()));
        }

        JsonNode detail = objectMapper.readTree(mockMvc.perform(get("/api/employees/NV_POS_TEST"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertFalse(detail.has("password"));
        assertEquals(0, detail.get("active").asInt());

        JsonNode deleted = objectMapper.readTree(mockMvc.perform(delete("/api/employees/NV_POS_TEST"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertTrue(deleted.get("success").asBoolean());

        JsonNode afterDelete = objectMapper.readTree(mockMvc.perform(get("/api/employees/NV_POS_TEST"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertEquals(0, afterDelete.get("active").asInt());
    }

    @Test
    void employeeCreateRequiresPhoneAndRejectsDuplicateUsername() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id":"NV_NO_PHONE",
                                  "fullName":"Thiếu số điện thoại",
                                  "username":"phone_required",
                                  "password":"test",
                                  "role":"NHAN_VIEN"
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id":"NV_DUPLICATE_USER",
                                  "fullName":"Tên đăng nhập trùng",
                                  "phone":"0912345670",
                                  "username":"banhang_nam",
                                  "password":"test",
                                  "role":"NHAN_VIEN"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
