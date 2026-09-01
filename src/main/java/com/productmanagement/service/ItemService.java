package com.productmanagement.service;

import com.productmanagement.dto.ItemRequest;
import com.productmanagement.dto.ItemResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ItemService {

    ItemResponse addItem(ItemRequest request);

    ItemResponse updateItem(Long id, ItemRequest request);

    void deleteItem(Long id);

    ItemResponse getItemById(Long id);

    List<ItemResponse> getAllItems();

    Page<ItemResponse> getAllItems(Pageable pageable);

    List<ItemResponse> getItemsByProductId(Long productId);

    Page<ItemResponse> getItemsByProductId(Long productId, Pageable pageable);

}
