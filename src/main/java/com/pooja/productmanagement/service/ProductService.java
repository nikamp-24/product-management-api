package com.pooja.productmanagement.service;

import com.pooja.productmanagement.dto.ProductRequest;
import com.pooja.productmanagement.dto.ProductResponse;

import java.util.List;

/**
 * Service interface for Product management operations.
 */
public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    ProductResponse getProductById(Long id);

    List<ProductResponse> getAllProducts();

}
