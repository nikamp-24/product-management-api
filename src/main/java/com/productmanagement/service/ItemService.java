package com.productmanagement.service;

import com.productmanagement.dto.ItemRequest;
import com.productmanagement.dto.ItemResponse;

import java.util.List;

public interface ItemService {

    ItemResponse addItem(ItemRequest request);

    ItemResponse updateItem(Long id, ItemRequest request);

    void deleteItem(Long id);

    ItemResponse getItemById(Long id);

    List<ItemResponse> getAllItems();

}
