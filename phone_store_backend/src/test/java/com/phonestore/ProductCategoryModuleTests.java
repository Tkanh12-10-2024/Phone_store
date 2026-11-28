package com.phonestore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductCategoryModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void filterOptionsAndAutomaticSkuCreation() throws Exception {
        MvcResult filtersRes = mockMvc.perform(get("/api/products/filters"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode filters = objectMapper.readTree(
                filtersRes.getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
        assertTrue(filters.get("brands").size() > 0);
        assertTrue(filters.get("categories").size() > 0);
        assertTrue(filters.get("rams").size() > 0);
        assertTrue(filters.get("productLines").size() > 0);

        Map<String, Object> payload = Map.of(
                "tenSanPham", "Điện thoại tự sinh SKU Test",
                "maThuongHieu", 1,
                "maDanhMuc", 1,
                "dongMay", "Auto SKU Test Series",
                "variants", List.of(Map.of(
                        "mauSac", "Xanh Test",
                        "ram", "8GB",
                        "rom", "128GB",
                        "giaNhapThamKhao", 10000000,
                        "giaBan", 12000000,
                        "dinhMucTonToiThieu", 1
                ))
        );
        MvcResult createRes = mockMvc.perform(post("/api/products/with-variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = objectMapper.readTree(
                createRes.getResponse().getContentAsString(StandardCharsets.UTF_8)
        ).get(0);
        String generatedSku = created.get("maSanPham").asText();
        assertTrue(generatedSku.startsWith("SP_"));
        assertTrue(generatedSku.length() <= 20);

        mockMvc.perform(delete("/api/products/" + generatedSku))
                .andExpect(status().isOk());
    }

    @Test
    void categoryCrudAndDeleteProtection() throws Exception {
        // 1. Create valid category
        Map<String, Object> payload = Map.of(
                "tenDanhMuc", "Gaming Cao Cấp Test",
                "moTa", "Điện thoại chuyên chơi game cấu hình khủng"
        );
        MvcResult createRes = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = objectMapper.readTree(createRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        int id = created.get("maDanhMuc").asInt();
        assertEquals("Gaming Cao Cấp Test", created.get("tenDanhMuc").asText());

        // 2. Duplicate rejected (400)
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        // 3. Update category
        Map<String, Object> updatePayload = Map.of(
                "tenDanhMuc", "Gaming Esport Test",
                "moTa", "Cập nhật mô tả danh mục"
        );
        MvcResult updateRes = mockMvc.perform(put("/api/categories/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode updated = objectMapper.readTree(updateRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals("Gaming Esport Test", updated.get("tenDanhMuc").asText());

        // 4. Delete unused succeeds
        MvcResult delRes = mockMvc.perform(delete("/api/categories/" + id))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(objectMapper.readTree(delRes.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("success").asBoolean());

        // 5. Delete in-use category (id=1) rejected (409)
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void brandCrudAndDeleteProtection() throws Exception {
        // 1. Create valid brand
        Map<String, Object> payload = Map.of(
                "tenThuongHieu", "OnePlus Test",
                "quocGia", "Trung Quốc",
                "moTa", "Flagship Killer"
        );
        MvcResult createRes = mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = objectMapper.readTree(createRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        int id = created.get("maThuongHieu").asInt();
        assertEquals("OnePlus Test", created.get("tenThuongHieu").asText());

        // 2. Duplicate rejected (400)
        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        // 3. Update brand
        Map<String, Object> updatePayload = Map.of(
                "tenThuongHieu", "OnePlus Global Test",
                "quocGia", "Quốc tế",
                "moTa", "Cập nhật thương hiệu"
        );
        MvcResult updateRes = mockMvc.perform(put("/api/brands/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode updated = objectMapper.readTree(updateRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals("OnePlus Global Test", updated.get("tenThuongHieu").asText());

        // 4. Delete unused succeeds
        MvcResult delRes = mockMvc.perform(delete("/api/brands/" + id))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(objectMapper.readTree(delRes.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("success").asBoolean());

        // 5. Delete in-use brand (id=1 Apple) rejected (409)
        mockMvc.perform(delete("/api/brands/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void productWithVariantsUploadFilterDetailAndSoftDelete() throws Exception {
        // 1. Upload valid PNG & verify static resource access
        MockMultipartFile pngFile = new MockMultipartFile(
                "files",
                "iphone16-demo.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );
        MvcResult uploadRes = mockMvc.perform(multipart("/api/products/upload").file(pngFile))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode uploadJson = objectMapper.readTree(uploadRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertTrue(uploadJson.get("success").asBoolean());
        String uploadedUrl = uploadJson.get("urls").get(0).asText();
        assertTrue(uploadedUrl.startsWith("/uploads/products/"));

        mockMvc.perform(get(uploadedUrl))
                .andExpect(status().isOk());

        // Reject .exe upload
        MockMultipartFile badFile = new MockMultipartFile(
                "files",
                "malware.exe",
                "application/octet-stream",
                "MZ".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/products/upload").file(badFile))
                .andExpect(status().isBadRequest());

        // 2. Create product with 2 variants
        Map<String, Object> multiPayload = Map.ofEntries(
                Map.entry("tenSanPham", "iPhone 16 Pro Test"),
                Map.entry("maThuongHieu", 1),
                Map.entry("maDanhMuc", 1),
                Map.entry("dongMay", "iPhone 16 Series Test"),
                Map.entry("manHinh", "OLED 6.3 inch 120Hz"),
                Map.entry("chipXuLy", "Apple A18 Pro"),
                Map.entry("pin", "3582 mAh"),
                Map.entry("camera", "48MP + 48MP + 12MP"),
                Map.entry("heDieuHanh", "iOS 18"),
                Map.entry("thoiGianBaoHanhThang", 12),
                Map.entry("trangThai", "Dang_kinh_doanh"),
                Map.entry("hinhAnh", uploadedUrl),
                Map.entry("galleryUrls", List.of(uploadedUrl)),
                Map.entry("variants", List.of(
                        Map.of(
                                "maSanPham", "SP_T_IP16P_256",
                                "mauSac", "Titan Sa Mạc",
                                "ram", "8GB",
                                "rom", "256GB",
                                "giaNhapThamKhao", new BigDecimal("25000000"),
                                "giaBan", new BigDecimal("28990000"),
                                "dinhMucTonToiThieu", 2
                        ),
                        Map.of(
                                "maSanPham", "SP_T_IP16P_128",
                                "mauSac", "Titan Đen",
                                "ram", "8GB",
                                "rom", "128GB",
                                "giaNhapThamKhao", new BigDecimal("22000000"),
                                "giaBan", new BigDecimal("25990000"),
                                "dinhMucTonToiThieu", 3
                        )
                ))
        );

        MvcResult createVarRes = mockMvc.perform(post("/api/products/with-variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(multiPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode createdList = objectMapper.readTree(createVarRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals(2, createdList.size());

        // 3. Filter products by brand, price range, ram, rom, keyword, sort=price_asc
        MvcResult filterRes = mockMvc.perform(get("/api/products")
                        .param("maThuongHieu", "1")
                        .param("minPrice", "25000000")
                        .param("maxPrice", "29500000")
                        .param("ram", "8GB")
                        .param("keyword", "iPhone 16")
                        .param("sort", "price_asc"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode filtered = objectMapper.readTree(filterRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals(2, filtered.size());
        assertEquals("SP_T_IP16P_128", filtered.get(0).get("maSanPham").asText());
        assertEquals("SP_T_IP16P_256", filtered.get(1).get("maSanPham").asText());

        // Filter by rom=256GB
        MvcResult romFilterRes = mockMvc.perform(get("/api/products")
                        .param("keyword", "iPhone 16")
                        .param("rom", "256GB"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode romFiltered = objectMapper.readTree(romFilterRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals(1, romFiltered.size());
        assertEquals("SP_T_IP16P_256", romFiltered.get(0).get("maSanPham").asText());

        // 4. Detail endpoint: galleries & siblingVariants
        MvcResult detailRes = mockMvc.perform(get("/api/products/SP_T_IP16P_256"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode detail = objectMapper.readTree(detailRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertTrue(detail.get("galleries").size() >= 1);
        assertEquals(2, detail.get("siblingVariants").size());

        // 5. Update product & toggle status
        Map<String, Object> updatePayload = Map.ofEntries(
                Map.entry("tenSanPham", "iPhone 16 Pro Test Updated"),
                Map.entry("maThuongHieu", 1),
                Map.entry("maDanhMuc", 1),
                Map.entry("dongMay", "iPhone 16 Series Test"),
                Map.entry("mauSac", "Titan Sa Mạc"),
                Map.entry("ram", "8GB"),
                Map.entry("rom", "256GB"),
                Map.entry("giaNhapThamKhao", new BigDecimal("25000000")),
                Map.entry("giaBan", new BigDecimal("27990000")),
                Map.entry("trangThai", "Dang_kinh_doanh")
        );
        MvcResult updRes = mockMvc.perform(put("/api/products/SP_T_IP16P_256")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("iPhone 16 Pro Test Updated",
                objectMapper.readTree(updRes.getResponse().getContentAsString(StandardCharsets.UTF_8))
                        .get("tenSanPham").asText());

        MvcResult statusRes = mockMvc.perform(put("/api/products/SP_T_IP16P_256/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThai\":\"Ngung_kinh_doanh\"}"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("Ngung_kinh_doanh",
                objectMapper.readTree(statusRes.getResponse().getContentAsString(StandardCharsets.UTF_8))
                        .get("trangThai").asText());

        // 6. Hard delete unused newly created variants
        MvcResult del1 = mockMvc.perform(delete("/api/products/SP_T_IP16P_256"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("DELETED",
                objectMapper.readTree(del1.getResponse().getContentAsString(StandardCharsets.UTF_8))
                        .get("action").asText());

        MvcResult del2 = mockMvc.perform(delete("/api/products/SP_T_IP16P_128"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("DELETED",
                objectMapper.readTree(del2.getResponse().getContentAsString(StandardCharsets.UTF_8))
                        .get("action").asText());

        // 7. Soft delete historical product (SP_IP15PM_256TN has IMEI/order/invoice history) -> DEACTIVATED
        MvcResult softDelRes = mockMvc.perform(delete("/api/products/SP_IP15PM_256TN"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("DEACTIVATED",
                objectMapper.readTree(softDelRes.getResponse().getContentAsString(StandardCharsets.UTF_8))
                        .get("action").asText());

        MvcResult afterSoftDel = mockMvc.perform(get("/api/products/SP_IP15PM_256TN"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("Ngung_kinh_doanh",
                objectMapper.readTree(afterSoftDel.getResponse().getContentAsString(StandardCharsets.UTF_8))
                        .get("trangThai").asText());

        // Restore SP_IP15PM_256TN status so seed state stays clean
        mockMvc.perform(put("/api/products/SP_IP15PM_256TN/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trangThai\":\"Dang_kinh_doanh\"}"))
                .andExpect(status().isOk());
    }
}
