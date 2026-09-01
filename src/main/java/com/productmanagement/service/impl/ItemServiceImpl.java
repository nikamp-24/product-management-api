package com.productmanagement.service.impl;

import com.productmanagement.dto.ItemRequest;
import com.productmanagement.dto.ItemResponse;
import com.productmanagement.entity.Item;
import com.productmanagement.entity.Product;
import com.productmanagement.exception.ResourceNotFoundException;
import com.productmanagement.repository.ItemRepository;
import com.productmanagement.repository.ProductRepository;
import com.productmanagement.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ItemResponse addItem(ItemRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        Item item = Item.builder()
                .quantity(request.getQuantity())
                .product(product)
                .build();

        Item savedItem = itemRepository.save(item);
        return mapToResponse(savedItem);
    }

    @Override
    @Transactional
    public ItemResponse updateItem(Long id, ItemRequest request) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        item.setQuantity(request.getQuantity());
        item.setProduct(product);

        Item updatedItem = itemRepository.save(item);
        return mapToResponse(updatedItem);
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
        itemRepository.delete(item);
    }

    @Override
    public ItemResponse getItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
        return mapToResponse(item);
    }

    @Override
    public List<ItemResponse> getAllItems() {
        return itemRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ItemResponse mapToResponse(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProduct() != null ? item.getProduct().getProductName() : null)
                .quantity(item.getQuantity())
                .build();
    }

}
