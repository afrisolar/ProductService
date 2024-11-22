package com.afrisol.ProductService.service;

import com.afrisol.ProductService.dto.ProductRequestDto;
import com.afrisol.ProductService.dto.ProductResponseDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductService {
    Mono<ProductResponseDto> addProduct(ProductRequestDto productRequestDto, String requestID);
    Mono<ProductResponseDto> updateProduct(ProductRequestDto productRequestDto, String productId, String requestID);
    Mono<Void> deleteProduct(String productId, String requestID);
    Flux<ProductResponseDto> getAllProducts(String requestID);
    Mono<ProductResponseDto> getProduct(String productId, String requestID);
}
