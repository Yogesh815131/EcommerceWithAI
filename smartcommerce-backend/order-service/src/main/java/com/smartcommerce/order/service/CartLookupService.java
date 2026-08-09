package com.smartcommerce.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartLookupService {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    /**
     * Reads the caller's cart directly from cart-service (bypassing the
     * Gateway). We forward the ORIGINAL caller's X-User-Id — cart-service
     * doesn't know or care that the request is coming from another
     * service rather than through the Gateway; it just needs that header,
     * same as always.
     */
    @SuppressWarnings("unchecked")
    public List<CartItem> getCart(Long userId) {
        Map<String, Object> response = loadBalancedWebClientBuilder.build()
                .get()
                .uri("http://cart-service/api/cart")
                .header("X-User-Id", String.valueOf(userId))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || response.get("items") == null) {
            return List.of();
        }

        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) response.get("items");
        return rawItems.stream()
                .map(item -> new CartItem(
                        Long.valueOf(String.valueOf(item.get("productId"))),
                        Integer.valueOf(String.valueOf(item.get("quantity"))),
                        new BigDecimal(String.valueOf(item.get("price")))))
                .toList();
    }

    public void clearCart(Long userId) {
        loadBalancedWebClientBuilder.build()
                .delete()
                .uri("http://cart-service/api/cart")
                .header("X-User-Id", String.valueOf(userId))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public record CartItem(Long productId, Integer quantity, BigDecimal priceAtReadTime) {}
}
