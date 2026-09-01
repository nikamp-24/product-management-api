package com.productmanagement.service.impl;

import com.productmanagement.dto.ProductRequest;
import com.productmanagement.dto.ProductResponse;
import com.productmanagement.entity.Product;
import com.productmanagement.exception.DuplicateResourceException;
import com.productmanagement.exception.ResourceNotFoundException;
import com.productmanagement.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .productName("Mechanical Keyboard")
                .createdBy("admin")
                .createdOn(Instant.now())
                .build();

        productRequest = ProductRequest.builder()
                .productName("Mechanical Keyboard")
                .build();
    }

    @Test
    void createProduct_Success() {
        when(productRepository.existsByProductName("Mechanical Keyboard")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.createProduct(productRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getProductName()).isEqualTo("Mechanical Keyboard");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_ThrowsDuplicateResourceException() {
        when(productRepository.existsByProductName("Mechanical Keyboard")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(productRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_Success() {
        ProductRequest updateRequest = ProductRequest.builder().productName("Gaming Keyboard").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsByProductName("Gaming Keyboard")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.updateProduct(1L, updateRequest);

        assertThat(response).isNotNull();
        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_ThrowsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, productRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 99");
    }

    @Test
    void updateProduct_ThrowsDuplicateResourceException_WhenNameChangedAndExists() {
        ProductRequest updateRequest = ProductRequest.builder().productName("Existing Keyboard").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsByProductName("Existing Keyboard")).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProduct(1L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void deleteProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_ThrowsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).delete(any());
    }

    @Test
    void getProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getProductById_ThrowsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllProducts_Success() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.getAllProducts();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getProductName()).isEqualTo("Mechanical Keyboard");
    }

}
