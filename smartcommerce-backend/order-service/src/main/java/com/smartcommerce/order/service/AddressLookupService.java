package com.smartcommerce.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AddressLookupService {

    private final WebClient.Builder loadBalancedWebClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Fetches the address and returns it as a JSON string, ready to store
     * as the order's immutable snapshot. Forwards the caller's own
     * X-User-Id so user-service's ownership check (this address must
     * belong to this user) still applies here too.
     */
    public String fetchAddressSnapshotJson(Long userId, Long addressId) {
        Map<String, Object> address = loadBalancedWebClientBuilder.build()
                .get()
                .uri("http://user-service/api/addresses/{id}", addressId)
                .header("X-User-Id", String.valueOf(userId))
                .retrieve()
                .onStatus(status -> status.isError(),
                        response -> response.bodyToMono(Map.class)
                                .map(body -> new IllegalArgumentException(
                                        String.valueOf(body.getOrDefault("message", "Address not found")))))
                .bodyToMono(Map.class)
                .block();

        try {
            return objectMapper.writeValueAsString(address);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to snapshot shipping address", e);
        }
    }
}
