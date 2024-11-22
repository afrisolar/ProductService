package com.afrisol.ProductService.controller;

import com.afrisol.ProductService.dto.ProductRequestDto;
import com.afrisol.ProductService.dto.ProductResponseDto;
import com.afrisol.ProductService.exception.ProductNotFoundException;
import com.afrisol.ProductService.service.ProductService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/products")
@Slf4j
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Flux<ProductResponseDto> getAllProducts() {
        String requestID = UUID.randomUUID().toString();
        log.info("Getting all products : {}", requestID);
        return productService.getAllProducts(requestID);
    }

    @GetMapping("/{productId}")
    public Mono<ResponseEntity<ProductResponseDto>> getProduct(@PathVariable @Valid String productId) {
        String requestID = UUID.randomUUID().toString();
        log.info("Getting product with ID : {} and requestID: {}", productId, requestID);
        return productService.getProduct(productId, requestID)
                .map(ResponseEntity::ok)
                .onErrorResume(ProductNotFoundException.class, ex -> {
                    log.error("Product not found: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.status(404).body(null));
                })
                .onErrorResume(RuntimeException.class, ex -> {
                    log.error("Unhandled RuntimeException: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.status(400).body(null));
                });
    }

    @PostMapping
    public Mono<ResponseEntity<ProductResponseDto>> addProduct(@RequestBody @Valid ProductRequestDto productDto) {
        String requestID = UUID.randomUUID().toString();
        log.info("Adding product with name: {} and requestID {}", productDto.getName(), requestID);
        return productService.addProduct(productDto, requestID)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{productId}")
    public Mono<ResponseEntity<ProductResponseDto>> updateProduct(
            @PathVariable String productId,
            @RequestBody @Valid ProductRequestDto productDto) {
        String requestID = UUID.randomUUID().toString();
        log.info("Updating product with ID: {} and requestID {}", productId, requestID);
        return productService.updateProduct(productDto, productId, requestID)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{productId}")
    public Mono<ResponseEntity<Object>> deleteProduct(@PathVariable String productId) {
        String requestID = UUID.randomUUID().toString();
        log.info("Deleting product with ID: {} and requestID {}", productId, requestID);
        return productService.deleteProduct(productId, requestID)
                .then(Mono.just(ResponseEntity.noContent().<Object>build())) // Success: 204 No Content
                .onErrorResume(e -> {
                    log.error("Error deleting product with ID: {} - {}", productId, e.getMessage(), e);
                    if (e instanceof ProductNotFoundException) {
                        return Mono.just(ResponseEntity.status(404).body(Map.of("error", e.getMessage())));
                    }
                    return Mono.just(ResponseEntity.status(500).body(Map.of("error", "Internal Server Error")));
                });
    }

}

