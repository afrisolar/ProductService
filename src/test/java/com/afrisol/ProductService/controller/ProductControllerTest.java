package com.afrisol.ProductService.controller;

import com.afrisol.ProductService.dto.ProductRequestDto;
import com.afrisol.ProductService.dto.ProductResponseDto;
import com.afrisol.ProductService.exception.ProductAlreadyExistsException;
import com.afrisol.ProductService.exception.ProductNotFoundException;
import com.afrisol.ProductService.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

@WebFluxTest(ProductController.class)
public class ProductControllerTest {
    @MockBean
    private ProductService productService;

    @Autowired
    private WebTestClient webTestClient;

    private ProductRequestDto productDto;
    private ProductResponseDto productResponseDto;
    private String requestID;

    @BeforeEach
    void setUp() {
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
    void addProduct_whenValid_shouldReturn200() {
        when(productService.addProduct(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(productResponseDto));

        webTestClient.post()
                .uri("/api/v1/products")
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductResponseDto.class)
                .isEqualTo(productResponseDto);

        Mockito.verify(productService).addProduct(Mockito.any(), Mockito.any());
    }


    @Test
    void addProduct_whenInvalid_shouldReturn404() {
        when(productService.addProduct(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.error(new ProductAlreadyExistsException("Product already exists!")));

        webTestClient.post()
                .uri("/api/v1/products")
                .bodyValue(productDto)
                .exchange()
                .expectStatus().is4xxClientError();

        Mockito.verify(productService).addProduct(Mockito.any(), Mockito.any());
    }

    @Test
    void updateProduct_whenProductExists_shouldReturnUpdatedProduct() {
        when(productService.updateProduct(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(productResponseDto));

        webTestClient.put()
                .uri("/api/v1/products/{productId}", "testId")
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductResponseDto.class)
                .isEqualTo(productResponseDto);

        Mockito.verify(productService).updateProduct(Mockito.any(), Mockito.any(), Mockito.any());
    }


    @Test
    void updateProduct_whenProductNotFound_shouldReturn404() {
        when(productService.updateProduct(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.error(new ProductNotFoundException("Product not found with ID: testId")));

        webTestClient.put()
                .uri("/api/v1/products/{productId}", "testId")
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isNotFound();

        Mockito.verify(productService).updateProduct(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void updateProduct_whenDatabaseErrorOccurs_shouldReturn500() {
        when(productService.updateProduct(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        webTestClient.put()
                .uri("/api/v1/products/{productId}", "testId")
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        Mockito.verify(productService).updateProduct(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void deleteProduct_whenProductExists_shouldReturnNoContent() {
        when(productService.deleteProduct(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/v1/products/{productId}", "testId")
                .exchange()
                .expectStatus().isNoContent();

        Mockito.verify(productService).deleteProduct(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void deleteProduct_whenProductNotFound_shouldReturn404() {
        when(productService.deleteProduct(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.error(new ProductNotFoundException("Product not found with ID: testId")));

        webTestClient.delete()
                .uri("/api/v1/products/{productId}", "testId")
                .exchange()
                .expectStatus().isNotFound();

        Mockito.verify(productService).deleteProduct(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void deleteProduct_whenDatabaseErrorOccurs_shouldReturn500() {
        when(productService.deleteProduct(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        webTestClient.delete()
                .uri("/api/v1/products/{productId}", "testId")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        Mockito.verify(productService).deleteProduct(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void getProduct_whenValidId_shouldReturnProduct() {
        when(productService.getProduct(Mockito.eq("testId"), Mockito.anyString()))
                .thenReturn(Mono.just(productResponseDto));

        webTestClient.get()
                .uri("/api/v1/products/{productId}", "testId")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductResponseDto.class)
                .consumeWith(response -> {
                    ProductResponseDto body = response.getResponseBody();
                    assert body != null;
                    assert body.getId().equals("testId");
                    assert body.getName().equals("Test Product");
                    // Add more assertions as needed
                });

        Mockito.verify(productService).getProduct(Mockito.eq("testId"), Mockito.anyString());
    }

    @Test
    void getProduct_whenNonexistentId_shouldReturn404() {
        when(productService.getProduct(Mockito.eq("nonExistentId"), Mockito.anyString()))
                .thenReturn(Mono.error(new ProductNotFoundException("Product not found")));

        webTestClient.get()
                .uri("/api/v1/products/{productId}", "nonExistentId")
                .exchange()
                .expectStatus().isNotFound();

        Mockito.verify(productService).getProduct(Mockito.eq("nonExistentId"), Mockito.anyString());
    }


    @Test
    void getAllProducts_whenProductsExist_shouldReturnProductList() {
        when(productService.getAllProducts(Mockito.anyString()))
                .thenReturn(Flux.just(productResponseDto));

        webTestClient.get()
                .uri("/api/v1/products")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponseDto.class)
                .consumeWith(response -> {
                    List<ProductResponseDto> products = response.getResponseBody();
                    assert products != null;
                    assert products.size() == 1;
                    assert products.get(0).getId().equals("testId");
                });

        Mockito.verify(productService).getAllProducts(Mockito.anyString());
    }

    @Test
    void getAllProducts_whenNoProductsExist_shouldReturnEmptyList() {
        when(productService.getAllProducts(Mockito.anyString()))
                .thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/api/v1/products")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponseDto.class)
                .consumeWith(response -> {
                    List<ProductResponseDto> products = response.getResponseBody();
                    assert products != null;
                    assert products.isEmpty();
                });

        Mockito.verify(productService).getAllProducts(Mockito.anyString());
    }

    @Test
    void getAllProducts_whenUnexpectedErrorOccurs_shouldReturn500() {
        when(productService.getAllProducts(Mockito.anyString()))
                .thenReturn(Flux.error(new RuntimeException("Database error")));

        webTestClient.get()
                .uri("/api/v1/products")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        Mockito.verify(productService).getAllProducts(Mockito.anyString());
    }


}
