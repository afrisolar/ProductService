package com.afrisol.ProductService.service;

import com.afrisol.ProductService.dto.ProductRequestDto;
import com.afrisol.ProductService.dto.ProductResponseDto;
import com.afrisol.ProductService.exception.CustomException;
import com.afrisol.ProductService.exception.ProductAlreadyExistsException;
import com.afrisol.ProductService.exception.ProductNotFoundException;
import com.afrisol.ProductService.model.Product;
import com.afrisol.ProductService.repository.ProductRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Mono<ProductResponseDto> addProduct(ProductRequestDto productRequestDto, String requestID) {
        if (productRequestDto == null) {
            return Mono.error(new IllegalArgumentException("ProductRequestDto cannot be null"));
        }
        return productRepository.existsByName(productRequestDto.getName())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ProductAlreadyExistsException("Product already exists"));
                    }
                    return productRepository.save(Product.builder()
                                    .name(productRequestDto.getName())
                                    .description(productRequestDto.getDescription())
                                    .type(productRequestDto.getType())
                                    .price(productRequestDto.getPrice())
                                    .quantity(productRequestDto.getQuantity())
                                    .build())
                            .doOnNext(savedProduct ->
                                    log.info("Successfully added product with ID: {}", savedProduct.getProductId())
                            ).map(this::mapToProductResponseDto);
                });
    }

    @Override
    public Mono<ProductResponseDto> updateProduct(@Valid ProductRequestDto productRequestDto, String productId, String requestID) {
        log.info("Updating product with ID: {} Request ID: {}", productId, requestID);

        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new ProductNotFoundException("Product not found with ID: " + productId)))
                .flatMap(existingProduct -> {
                    // Update product fields
                    existingProduct.setName(productRequestDto.getName());
                    existingProduct.setDescription(productRequestDto.getDescription());
                    existingProduct.setType(productRequestDto.getType());
                    existingProduct.setPrice(productRequestDto.getPrice());
                    existingProduct.setQuantity(productRequestDto.getQuantity());
                    return productRepository.save(existingProduct);
                })
                .doOnNext(updatedProduct ->
                        log.info("Successfully updated product with ID: {} Request ID: {}", updatedProduct.getProductId(), requestID)
                )
                .map(this::mapToProductResponseDto);
    }

    @Override
    public Mono<Void> deleteProduct(String productId, String requestID) {
        log.info("Deleting product with ID: {} Request ID {}", productId, requestID);
        if (productId == null || productId.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Invalid product ID"));
        }
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new ProductNotFoundException("Product not found with ID: " + productId)))
                .flatMap(productRepository::delete)
                .doOnSuccess(unused -> log.info("Successfully deleted product with ID: {} request ID {}", productId, requestID));
    }

    @Override
    public Flux<ProductResponseDto> getAllProducts(String requestID) {
        log.info("Retrieving all products with request ID: {}", requestID);
        return productRepository.findAll().map(this::mapToProductResponseDto);
    }

    @Override
    public Mono<ProductResponseDto> getProduct(String productId, String requestID) {
        if (productId == null || productId.isEmpty()) {
            return Mono.error(new CustomException(HttpStatus.BAD_REQUEST, "Product ID cannot be null or empty"));
        }
        log.info("Searching for product with ID: {}", productId);
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new ProductNotFoundException("Product not found")))
                .map(this::mapToProductResponseDto)
                .doOnNext(product -> log.info("Successfully retrieved product with ID: {} for requestID: {}", productId, requestID))
                .onErrorResume(e -> {
                    if (e instanceof ProductNotFoundException) {
                        log.error("Product not found: {}", e.getMessage());
                        return Mono.error(e); // Rethrow as is
                    } else if (e instanceof CustomException) {
                        log.error("Custom exception occurred: {}", e.getMessage());
                        return Mono.error(e); // Rethrow as is
                    }
                    log.error("Unexpected error occurred: {}", e.getMessage(), e);
                    return Mono.error(new RuntimeException("Unexpected error occurred", e));
                });
    }

    private ProductResponseDto mapToProductResponseDto(Product product) {
        return ProductResponseDto.builder()
                .id(product.getProductId())
                .name(product.getName())
                .description(product.getDescription())
                .type(product.getType())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .build();
    }
}

