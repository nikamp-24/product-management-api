package com.pooja.productmanagement.service;

import com.pooja.productmanagement.dto.ItemRequest;
import com.pooja.productmanagement.dto.ItemResponse;

import java.util.List;

/**
 * Service interface for Item inventory operations.
 */
public interface ItemService {

    ItemResponse addItem(ItemRequest request);

    ItemResponse updateItem(Long id, ItemRequest request);

    void deleteItem(Long id);

    ItemResponse getItemById(Long id);

    List<ItemResponse> getAllItems();

}
