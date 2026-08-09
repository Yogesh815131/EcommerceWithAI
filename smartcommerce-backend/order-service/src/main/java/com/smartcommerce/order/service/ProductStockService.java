package com.smartcommerce.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductStockService {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public ProductSnapshot getProduct(Long productId) {
        Map<String, Object> response = loadBalancedWebClientBuilder.build()
                .get()
                .uri("http://product-service/api/products/{id}", productId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new IllegalArgumentException("Product " + productId + " not found");
        }

        return new ProductSnapshot(
                Long.valueOf(String.valueOf(response.get("id"))),
                Long.valueOf(String.valueOf(response.get("vendorId"))),
                String.valueOf(response.get("name")),
                new BigDecimal(String.valueOf(response.get("price")))
        );
    }

    /** @return true if stock was successfully decremented, false if there wasn't enough */
    public boolean decrementStock(Long productId, Integer quantity) {
        try {
            loadBalancedWebClientBuilder.build()
                    .patch()
                    .uri("http://product-service/api/internal/products/{id}/decrement-stock?quantity={qty}",
                            productId, quantity)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            return false; // 409 Conflict (insufficient stock) or any other failure — treat as "can't fulfill"
        }
    }

    /** Compensating action — call this for every item already decremented if a later item in the same order fails. */
    public void restoreStock(Long productId, Integer quantity) {
        loadBalancedWebClientBuilder.build()
                .patch()
                .uri("http://product-service/api/internal/products/{id}/restore-stock?quantity={qty}",
                        productId, quantity)
                .header("X-Internal-Api-Key", internalApiKey)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public record ProductSnapshot(Long id, Long vendorId, String name, BigDecimal price) {}
}
