package com.smartcommerce.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderLookupService {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    /**
     * Fetches the order directly from order-service, forwarding the
     * caller's own identity headers — order-service's normal ownership
     * check (this order must belong to this user) applies here exactly
     * as it would for a request coming through the Gateway.
     */
    public OrderSnapshot getOrder(Long userId, List<String> roles, Long orderId) {
        Map<String, Object> response = loadBalancedWebClientBuilder.build()
                .get()
                .uri("http://order-service/api/orders/{id}", orderId)
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Roles", String.join(",", roles))
                .retrieve()
                .onStatus(status -> status.isError(),
                        r -> r.bodyToMono(Map.class)
                                .map(body -> new IllegalArgumentException(
                                        String.valueOf(body.getOrDefault("message", "Order not found")))))
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new IllegalArgumentException("Order not found");
        }

        return new OrderSnapshot(
                Long.valueOf(String.valueOf(response.get("id"))),
                String.valueOf(response.get("status")),
                new BigDecimal(String.valueOf(response.get("total")))
        );
    }

    /**
     * Compensating action — called if the Stripe charge fails, so a
     * created-but-unpaid order doesn't linger. Reuses order-service's
     * normal cancel endpoint (which already restores stock), forwarding
     * the same caller identity.
     */
    public void cancelOrder(Long userId, List<String> roles, Long orderId) {
        loadBalancedWebClientBuilder.build()
                .patch()
                .uri("http://order-service/api/orders/{id}/cancel", orderId)
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Roles", String.join(",", roles))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public record OrderSnapshot(Long id, String status, BigDecimal total) {}
}
