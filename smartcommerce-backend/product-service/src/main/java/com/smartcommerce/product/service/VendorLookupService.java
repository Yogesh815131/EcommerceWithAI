package com.smartcommerce.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Product Service doesn't own vendor data — User/Vendor Service does. So
 * before a vendor can create a product, we ask User Service directly
 * "what's this user's vendor record, and is it ACTIVE?" This call
 * bypasses the Gateway (service-to-service, same pattern as
 * user-service -> auth-service's role grant), but here we forward the
 * ORIGINAL caller's identity headers rather than using a shared secret,
 * since user-service's /api/vendors/me endpoint expects to know who's
 * asking (not "trust this other service blindly").
 */
@Service
@RequiredArgsConstructor
public class VendorLookupService {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    @Value("${services.user-service.url:http://user-service}")
    private String userServiceUrl;

    /** @throws IllegalArgumentException if the caller has no ACTIVE vendor record */
    public Long resolveActiveVendorId(Long userId, List<String> roles) {
        Map<String, Object> vendor = loadBalancedWebClientBuilder.build()
                .get()
                .uri(userServiceUrl + "/api/vendors/me")
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
            throw new IllegalArgumentException(
                    "Your vendor application is " + status + " — only ACTIVE vendors can manage products");
        }

        return Long.valueOf(String.valueOf(vendor.get("id")));
    }
}
