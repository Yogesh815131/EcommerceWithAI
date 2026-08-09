package com.smartcommerce.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VendorLookupService {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    /** Same pattern as product-service's VendorLookupService — see that class for the full rationale. */
    public Long resolveActiveVendorId(Long userId, List<String> roles) {
        Map<String, Object> vendor = loadBalancedWebClientBuilder.build()
                .get()
                .uri("http://user-service/api/vendors/me")
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Roles", String.join(",", roles))
                .retrieve()
                .onStatus(status -> status.isError(),
                        response -> response.bodyToMono(Map.class)
                                .map(body -> new IllegalArgumentException(
                                        String.valueOf(body.getOrDefault("message", "Could not verify vendor status")))))
                .bodyToMono(Map.class)
                .block();

        if (vendor == null) {
            throw new IllegalArgumentException("Could not verify vendor status");
        }

        String status = String.valueOf(vendor.get("status"));
        if (!"ACTIVE".equals(status)) {
            throw new IllegalArgumentException("Your vendor account is " + status);
        }

        return Long.valueOf(String.valueOf(vendor.get("id")));
    }
}
