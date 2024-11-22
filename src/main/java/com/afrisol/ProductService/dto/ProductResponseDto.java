package com.afrisol.ProductService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponseDto {

    private String id;

    private String name;

    private String description;

    private String type;

    private Double price;

    private int quantity;
}


