package com.pooja.productmanagement.controller;

import com.pooja.productmanagement.dto.ItemRequest;
import com.pooja.productmanagement.dto.ItemResponse;
import com.pooja.productmanagement.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping("/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> addItem(@Valid @RequestBody ItemRequest request) {
        ItemResponse response = itemService.addItem(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        List<ItemResponse> response = itemService.getAllItems();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<ItemResponse> getItemById(@PathVariable Long id) {
        ItemResponse response = itemService.getItemById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> updateItem(@PathVariable Long id,
                                                   @Valid @RequestBody ItemRequest request) {
        ItemResponse response = itemService.updateItem(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

}
