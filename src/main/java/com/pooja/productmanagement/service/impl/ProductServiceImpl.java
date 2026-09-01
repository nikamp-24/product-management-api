package com.pooja.productmanagement.service.impl;

import com.pooja.productmanagement.dto.ProductRequest;
import com.pooja.productmanagement.dto.ProductResponse;
import com.pooja.productmanagement.entity.Product;
import com.pooja.productmanagement.exception.DuplicateResourceException;
import com.pooja.productmanagement.exception.ResourceNotFoundException;
import com.pooja.productmanagement.repository.ProductRepository;
import com.pooja.productmanagement.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Implementation of ProductService for managing product catalog data.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsByProductName(request.getProductName())) {
            throw new DuplicateResourceException("Product with name '" + request.getProductName() + "' already exists");
        }

        Product product = Product.builder()
                .productName(request.getProductName())
                .createdBy("SYSTEM")
                .createdOn(Instant.now())
                .build();

        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (!product.getProductName().equalsIgnoreCase(request.getProductName()) &&
                productRepository.existsByProductName(request.getProductName())) {
            throw new DuplicateResourceException("Product with name '" + request.getProductName() + "' already exists");
        }

        product.setProductName(request.getProductName());
        product.setModifiedBy("SYSTEM");
        product.setModifiedOn(Instant.now());

        Product updatedProduct = productRepository.save(product);
        return mapToResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .createdBy(product.getCreatedBy())
                .createdOn(product.getCreatedOn())
                .modifiedBy(product.getModifiedBy())
                .modifiedOn(product.getModifiedOn())
                .build();
    }

}
