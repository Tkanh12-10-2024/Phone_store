package com.phonestore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PosCatalogIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void posProductUsesAvailableImeiCountAndReturnsDisplayFields() throws Exception {
        JsonNode products = objectMapper.readTree(mockMvc.perform(get("/api/pos/products"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        JsonNode product = null;
        for (JsonNode candidate : products) {
            if ("SP_SS_S24U_256AM".equals(candidate.get("maSanPham").asText())) {
                product = candidate;
                break;
            }
        }

        assertTrue(product != null);
        assertEquals(2, product.get("soLuongTon").asInt());
        assertEquals("12GB", product.get("ram").asText());
        assertEquals("256GB", product.get("rom").asText());
        assertFalse(product.has("imeis"));
    }
}
