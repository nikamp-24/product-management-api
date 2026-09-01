package com.productmanagement.service.impl;

import com.productmanagement.dto.ItemRequest;
import com.productmanagement.dto.ItemResponse;
import com.productmanagement.entity.Item;
import com.productmanagement.entity.Product;
import com.productmanagement.exception.ResourceNotFoundException;
import com.productmanagement.repository.ItemRepository;
import com.productmanagement.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    private Product product;
    private Item item;
    private ItemRequest itemRequest;

    @BeforeEach
    void setUp() {
        product = Product.builder().id(10L).productName("Wireless Mouse").build();
        item = Item.builder().id(1L).quantity(50).product(product).build();
        itemRequest = ItemRequest.builder().productId(10L).quantity(50).build();
    }

    @Test
    void addItem_Success() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        ItemResponse response = itemService.addItem(itemRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getProductId()).isEqualTo(10L);
        assertThat(response.getProductName()).isEqualTo("Wireless Mouse");
        assertThat(response.getQuantity()).isEqualTo(50);
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void addItem_ThrowsResourceNotFoundException_WhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        ItemRequest invalidRequest = ItemRequest.builder().productId(99L).quantity(10).build();

        assertThatThrownBy(() -> itemService.addItem(invalidRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 99");
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItem_Success() {
        Product newProduct = Product.builder().id(20L).productName("Gaming Mouse").build();
        ItemRequest updateRequest = ItemRequest.builder().productId(20L).quantity(75).build();
        Item updatedItem = Item.builder().id(1L).quantity(75).product(newProduct).build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(productRepository.findById(20L)).thenReturn(Optional.of(newProduct));
        when(itemRepository.save(any(Item.class))).thenReturn(updatedItem);

        ItemResponse response = itemService.updateItem(1L, updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.getQuantity()).isEqualTo(75);
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void updateItem_ThrowsResourceNotFoundException_WhenItemNotFound() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.updateItem(99L, itemRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Item not found with id: 99");
    }

    @Test
    void updateItem_ThrowsResourceNotFoundException_WhenProductNotFound() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.updateItem(1L, itemRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 10");
    }

    @Test
    void deleteItem_Success() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        itemService.deleteItem(1L);

        verify(itemRepository).delete(item);
    }

    @Test
    void deleteItem_ThrowsResourceNotFoundException() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.deleteItem(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(itemRepository, never()).delete(any());
    }

    @Test
    void getItemById_Success() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        ItemResponse response = itemService.getItemById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getQuantity()).isEqualTo(50);
    }

    @Test
    void getItemById_ThrowsResourceNotFoundException() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getItemById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllItems_Success() {
        when(itemRepository.findAll()).thenReturn(List.of(item));

        List<ItemResponse> responses = itemService.getAllItems();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getQuantity()).isEqualTo(50);
    }

    @Test
    void getAllItems_Pageable_Success() {
        org.springframework.data.domain.Page<Item> page = new org.springframework.data.domain.PageImpl<>(List.of(item));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(itemRepository.findAll(pageable)).thenReturn(page);

        org.springframework.data.domain.Page<ItemResponse> responses = itemService.getAllItems(pageable);

        assertThat(responses).hasSize(1);
        assertThat(responses.getContent().get(0).getQuantity()).isEqualTo(50);
    }

    @Test
    void getItemsByProductId_Success() {
        when(productRepository.existsById(10L)).thenReturn(true);
        when(itemRepository.findByProductId(10L)).thenReturn(List.of(item));

        List<ItemResponse> responses = itemService.getItemsByProductId(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getProductId()).isEqualTo(10L);
    }

}
