package com.productmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.productmanagement.config.DataInitializer;
import com.productmanagement.dto.ItemRequest;
import com.productmanagement.dto.LoginRequest;
import com.productmanagement.dto.ProductRequest;
import com.productmanagement.dto.RefreshTokenRequest;
import com.productmanagement.dto.RegisterRequest;
import com.productmanagement.entity.RoleName;
import com.productmanagement.repository.RoleRepository;
import com.productmanagement.repository.UserRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductManagementApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataInitializer dataInitializer;

    private static String adminToken;
    private static String userToken;
    private static String userRefreshToken;
    private static Long createdProductId;
    private static Long createdItemId;

    @Test
    @Order(1)
    void verifyStartupAndDataInitializerIdempotency() {
        assertThat(roleRepository.count()).isEqualTo(2);
        assertThat(userRepository.existsByUsername("admin")).isTrue();

        dataInitializer.run();

        assertThat(roleRepository.count()).isEqualTo(2);
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(2)
    void verifyAdminLogin() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("admin")
                .password("Admin@123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        adminToken = responseNode.get("accessToken").asText();
        assertThat(adminToken).isNotBlank();
        assertThat(responseNode.get("role").asText()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @Order(3)
    void verifyUserRegistrationAndLogin() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("testuser@example.com")
                .password("User@123")
                .role(RoleName.ROLE_USER.name())
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("User@123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        userToken = responseNode.get("accessToken").asText();
        userRefreshToken = responseNode.get("refreshToken").asText();
        assertThat(userToken).isNotBlank();
        assertThat(userRefreshToken).isNotBlank();
        assertThat(responseNode.get("role").asText()).isEqualTo("ROLE_USER");
    }

    @Test
    @Order(4)
    void verifyRefreshTokenFlow() throws Exception {
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(userRefreshToken)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String newAccessToken = responseNode.get("accessToken").asText();
        assertThat(newAccessToken).isNotBlank();
        userToken = newAccessToken;
    }

    @Test
    @Order(5)
    void verifyUnauthorizedAndRoleForbiddenAccess() throws Exception {
        ProductRequest productRequest = ProductRequest.builder()
                .productName("Unauthorized Test Product")
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    void verifyProductCrudOperations() throws Exception {
        ProductRequest createRequest = ProductRequest.builder()
                .productName("Premium Laptop")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode productNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        createdProductId = productNode.get("id").asLong();
        assertThat(createdProductId).isNotNull();
        assertThat(productNode.get("productName").asText()).isEqualTo("Premium Laptop");

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products/" + createdProductId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        ProductRequest updateRequest = ProductRequest.builder()
                .productName("Premium Laptop Pro")
                .build();

        mockMvc.perform(put("/api/v1/products/" + createdProductId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(7)
    void verifyItemCrudOperations() throws Exception {
        ItemRequest createItem = ItemRequest.builder()
                .productId(createdProductId)
                .quantity(25)
                .build();

        MvcResult itemResult = mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createItem)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode itemNode = objectMapper.readTree(itemResult.getResponse().getContentAsString());
        createdItemId = itemNode.get("id").asLong();
        assertThat(createdItemId).isNotNull();
        assertThat(itemNode.get("quantity").asInt()).isEqualTo(25);

        mockMvc.perform(get("/api/v1/items")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/items/" + createdItemId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        ItemRequest updateItem = ItemRequest.builder()
                .productId(createdProductId)
                .quantity(50)
                .build();

        mockMvc.perform(put("/api/v1/items/" + createdItemId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateItem)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/items/" + createdItemId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/products/" + createdProductId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(8)
    void verifyOpenApiDocumentationEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

}
