package com.smartcommerce.cart.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductLookupService {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    /**
     * Calls product-service's PUBLIC product-detail endpoint directly
     * (service-to-service, bypassing the Gateway — no auth headers
     * needed since GET /api/products/{id} is public there too).
     *
     * @throws IllegalArgumentException if the product doesn't exist or isn't ACTIVE
     */
    public ProductInfo getProduct(Long productId) {
        Map<String, Object> response;
        try {
            response = loadBalancedWebClientBuilder.build()
                    .get()
                    .uri("http://product-service/api/products/{id}", productId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            throw new IllegalArgumentException("Product " + productId + " not found");
        }

        if (response == null) {
            throw new IllegalArgumentException("Product " + productId + " not found");
        }

        String status = String.valueOf(response.get("status"));
        if (!"ACTIVE".equals(status)) {
            throw new IllegalArgumentException("This product is no longer available");
        }

        return new ProductInfo(
                Long.valueOf(String.valueOf(response.get("id"))),
                String.valueOf(response.get("name")),
                String.valueOf(response.get("imageUrl")),
                new BigDecimal(String.valueOf(response.get("price"))),
                Integer.valueOf(String.valueOf(response.get("stockQuantity")))
        );
    }

    public record ProductInfo(Long id, String name, String imageUrl, BigDecimal price, Integer stockQuantity) {}
}
