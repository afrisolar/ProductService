package com.afrisol.ProductService.service;

import com.afrisol.ProductService.dto.ProductRequestDto;
import com.afrisol.ProductService.dto.ProductResponseDto;
import com.afrisol.ProductService.exception.CustomException;
import com.afrisol.ProductService.exception.ProductNotFoundException;
import com.afrisol.ProductService.model.Product;
import com.afrisol.ProductService.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;
    private Product product;
    private ProductRequestDto productDto;
    private ProductResponseDto productResponseDto;
    private String requestID;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setName("Test Product");
        product.setType("Electronics");
        product.setDescription("Test Description");
        product.setPrice(100.0);
        product.setQuantity(10);

        productDto = new ProductRequestDto();
        productDto.setName("Test Product");
        productDto.setType("Electronics");
        productDto.setDescription("Test Description");
        productDto.setPrice(100.0);
        productDto.setQuantity(10);

        productResponseDto = new ProductResponseDto();
        productResponseDto.setId("testId");
        productResponseDto.setName("Test Product");
        productResponseDto.setType("Electronics");
        productResponseDto.setDescription("Test Description");
        productResponseDto.setPrice(100.0);
        productResponseDto.setQuantity(10);

        requestID = UUID.randomUUID().toString();
    }

    @Test
    void addProduct() {
        // Mock existsByName to return false, indicating the product does not already exist
        Mockito.when(productRepository.existsByName(Mockito.anyString())).thenReturn(Mono.just(false));

        // Mock save behavior
        when(productRepository.save(Mockito.any(Product.class))).thenAnswer(invocation -> {
            Product savedProduct = invocation.getArgument(0);
            savedProduct.setProductId("testId"); // Directly set the id without reflection
            savedProduct.setName("Test Product");
            savedProduct.setDescription("Test Description");
            savedProduct.setType("Electronics");
            savedProduct.setPrice(100.0);
            savedProduct.setQuantity(10);
            return Mono.just(savedProduct);
        });

        // Verify the response
        StepVerifier.create(productService.addProduct(productDto, requestID))
                .expectNextMatches(productResponse -> productResponse.getId().equals("testId")
                        && productResponse.getName().equals("Test Product")
                        && productResponse.getDescription().equals("Test Description")
                        && productResponse.getType().equals("Electronics")
                        && productResponse.getPrice().equals(100.0)
                        && productResponse.getQuantity() == 10)
                .verifyComplete();

        // Verify repository interactions
        verify(productRepository, Mockito.times(1)).existsByName(productDto.getName());
        verify(productRepository, Mockito.times(1)).save(Mockito.any(Product.class));
    }


    @Test
    void addProduct_whenNameExists_shouldThrowError() {
        when(productRepository.existsByName(product.getName())).thenReturn(Mono.just(true));

        StepVerifier.create(productService.addProduct(productDto, requestID))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Product already exists"))
                .verify();
        verify(productRepository).existsByName(product.getName());
        verify(productRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void updateProduct_whenProductExists_shouldReturnUpdatedProduct() {
        when(productRepository.findById(Mockito.anyString())).thenReturn(Mono.just(product));
        when(productRepository.save(Mockito.any(Product.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        productDto.setName("Updated Product");
        productDto.setDescription("Updated Description");

        StepVerifier.create(productService.updateProduct(productDto, "testId", requestID))
                .expectNextMatches(updatedProduct -> updatedProduct.getName().equals("Updated Product"))
                .verifyComplete();

        verify(productRepository, Mockito.times(1)).findById("testId");
        verify(productRepository, Mockito.times(1)).save(Mockito.any(Product.class));
    }

    @Test
    void updateProduct_whenProductNotFound_shouldThrowProductNotFoundException() {
        when(productRepository.findById(Mockito.anyString())).thenReturn(Mono.empty());

        StepVerifier.create(productService.updateProduct(productDto, "nonExistentId", requestID))
                .expectErrorMatches(throwable -> throwable instanceof ProductNotFoundException &&
                        throwable.getMessage().equals("Product not found with ID: nonExistentId"))
                .verify();

        verify(productRepository, Mockito.times(1)).findById("nonExistentId");
    }

    @Test
    void updateProduct_whenDatabaseErrorOccurs_shouldThrowRuntimeException() {
        when(productRepository.findById(Mockito.anyString())).thenReturn(Mono.just(product));
        when(productRepository.save(Mockito.any(Product.class))).thenReturn(Mono.error(new RuntimeException("Database error")));

        StepVerifier.create(productService.updateProduct(productDto, "testId", requestID))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Database error"))
                .verify();

        verify(productRepository, Mockito.times(1)).findById("testId");
    }

    @Test
    void deleteProduct_whenProductExists_shouldReturnVoid() {
        when(productRepository.findById(Mockito.anyString())).thenReturn(Mono.just(product));
        when(productRepository.delete(Mockito.any(Product.class))).thenReturn(Mono.empty());

        StepVerifier.create(productService.deleteProduct("testId", requestID))
                .verifyComplete();

        verify(productRepository, Mockito.times(1)).findById("testId");
        verify(productRepository, Mockito.times(1)).delete(Mockito.any(Product.class));
    }

    @Test
    void deleteProduct_whenProductNotFound_shouldThrowProductNotFoundException() {
        when(productRepository.findById(Mockito.anyString())).thenReturn(Mono.empty());

        StepVerifier.create(productService.deleteProduct("nonExistentId", requestID))
                .expectErrorMatches(throwable -> throwable instanceof ProductNotFoundException &&
                        throwable.getMessage().equals("Product not found with ID: nonExistentId"))
                .verify();

        verify(productRepository, Mockito.times(1)).findById("nonExistentId");
    }

    @Test
    void deleteProduct_whenDatabaseErrorOccurs_shouldThrowRuntimeException() {
        when(productRepository.findById(Mockito.anyString())).thenReturn(Mono.just(product));
        when(productRepository.delete(Mockito.any(Product.class))).thenReturn(Mono.error(new RuntimeException("Database error")));

        StepVerifier.create(productService.deleteProduct("testId", requestID))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Database error"))
                .verify();

        verify(productRepository, Mockito.times(1)).findById("testId");
        verify(productRepository, Mockito.times(1)).delete(Mockito.any(Product.class));
    }

    @Test
    void getProduct_whenNonexistentId_shouldThrowProductNotFoundException() {
        when(productRepository.findById("nonExistentId")).thenReturn(Mono.empty());

        StepVerifier.create(productService.getProduct("nonExistentId", requestID))
                .expectErrorMatches(throwable -> throwable instanceof ProductNotFoundException &&
                        throwable.getMessage().equals("Product not found"))
                .verify();

        verify(productRepository).findById("nonExistentId");
    }

    @Test
    void getProduct_whenNullId_shouldThrowCustomException() {
        StepVerifier.create(productService.getProduct(null, requestID))
                .expectErrorMatches(throwable -> throwable instanceof CustomException &&
                        throwable.getMessage().equals("Product ID cannot be null or empty"))
                .verify();
    }

    @Test
    void getProduct_whenEmptyId_shouldThrowCustomException() {
        StepVerifier.create(productService.getProduct("", requestID))
                .expectErrorMatches(throwable -> throwable instanceof CustomException &&
                        throwable.getMessage().equals("Product ID cannot be null or empty"))
                .verify();
    }

    @Test
    void getProduct_whenUnexpectedErrorOccurs_shouldThrowRuntimeException() {
        when(productRepository.findById("testId")).thenReturn(Mono.error(new RuntimeException("Database error")));

        StepVerifier.create(productService.getProduct("testId", requestID))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Unexpected error occurred"))
                .verify();

        verify(productRepository).findById("testId");
    }
    @Test
    void getAllProducts_whenProductsExist_shouldReturnProductList() {
        when(productRepository.findAll()).thenReturn(Flux.just(product));

        StepVerifier.create(productService.getAllProducts(requestID))
                .expectNextMatches(response -> response.getName().equals("Test Product"))
                .verifyComplete();

        verify(productRepository).findAll();
    }

    @Test
    void getAllProducts_whenNoProductsExist_shouldReturnEmptyFlux() {
        when(productRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(productService.getAllProducts(requestID))
                .verifyComplete();

        verify(productRepository).findAll();
    }

    @Test
    void getAllProducts_whenUnexpectedErrorOccurs_shouldThrowRuntimeException() {
        when(productRepository.findAll()).thenReturn(Flux.error(new RuntimeException("Database error")));

        StepVerifier.create(productService.getAllProducts(requestID))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Database error"))
                .verify();

        verify(productRepository).findAll();
    }

}
