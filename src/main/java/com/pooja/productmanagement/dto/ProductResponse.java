package com.pooja.productmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * DTO representing product details returned to clients.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String productName;
    private String createdBy;
    private Instant createdOn;
    private String modifiedBy;
    private Instant modifiedOn;

}
