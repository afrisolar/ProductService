package com.afrisol.ProductService.repository;

import com.afrisol.ProductService.model.Product;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ProductRepository extends ReactiveCrudRepository<Product, String> {
    Mono<Boolean> existsByName(String name);
}
