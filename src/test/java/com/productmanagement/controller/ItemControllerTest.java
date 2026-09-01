package com.productmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productmanagement.dto.ItemRequest;
import com.productmanagement.dto.LoginRequest;
import com.productmanagement.dto.ProductRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private Long testProductId;
    private Long createdItemId;

    @BeforeAll
    void setupProduct() throws Exception {
        LoginRequest adminLogin = LoginRequest.builder().username("admin").password("Admin@123").build();
        MvcResult adminResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(adminResult.getResponse().getContentAsString()).get("accessToken").asText();

        ProductRequest prodReq = ProductRequest.builder().productName("Item Test Base Product").build();
        MvcResult prodResult = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prodReq)))
                .andExpect(status().isCreated())
                .andReturn();
        testProductId = objectMapper.readTree(prodResult.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @Order(1)
    void addItem_AsAdmin_Success() throws Exception {
        ItemRequest request = ItemRequest.builder()
                .productId(testProductId)
                .quantity(15)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.quantity").value(15))
                .andReturn();

        createdItemId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @Order(2)
    void addItem_Validation_NullProductId_Returns400() throws Exception {
        ItemRequest request = ItemRequest.builder().quantity(10).build();

        mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @Order(3)
    void addItem_Validation_NegativeQuantity_Returns400() throws Exception {
        ItemRequest request = ItemRequest.builder()
                .productId(testProductId)
                .quantity(-5)
                .build();

        mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @Order(4)
    void getAllItems_Success() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(5)
    void getItemById_Success() throws Exception {
        mockMvc.perform(get("/api/v1/items/" + createdItemId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(15));
    }

    @Test
    @Order(6)
    void getItemById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/items/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @Order(7)
    void updateItem_AsAdmin_Success() throws Exception {
        ItemRequest update = ItemRequest.builder()
                .productId(testProductId)
                .quantity(30)
                .build();

        mockMvc.perform(put("/api/v1/items/" + createdItemId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(30));
    }

    @Test
    @Order(8)
    void deleteItem_AsAdmin_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/items/" + createdItemId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(9)
    void deleteItem_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/items/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

}
