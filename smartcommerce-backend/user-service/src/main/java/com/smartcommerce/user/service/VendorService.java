package com.smartcommerce.user.service;

import com.smartcommerce.user.dto.VendorApplicationRequest;
import com.smartcommerce.user.entity.Vendor;
import com.smartcommerce.user.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final WebClient.Builder loadBalancedWebClientBuilder;

    @org.springframework.beans.factory.annotation.Value("${internal.api-key}")
    private String internalApiKey;

    /** A user applies to become a vendor. Status starts as PENDING — no role is granted yet. */
    public Vendor apply(Long userId, VendorApplicationRequest request) {
        if (vendorRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("You have already applied to become a vendor");
        }

        Vendor vendor = Vendor.builder()
                .userId(userId)
                .businessName(request.getBusinessName())
                .businessDescription(request.getBusinessDescription())
                .status(Vendor.VendorStatus.PENDING)
                .build();

        return vendorRepository.save(vendor);
    }

    public List<Vendor> listPending() {
        return vendorRepository.findByStatus(Vendor.VendorStatus.PENDING);
    }

    /**
     * Admin approves a pending vendor application. Two things happen:
     *   1. This service's own record flips to ACTIVE.
     *   2. auth-service is called to actually grant ROLE_VENDOR — this
     *      service does NOT own the users/roles table, so it can't just
     *      update it directly; it has to ask the service that does.
     *
     * If step 2 fails, we deliberately do NOT save step 1 — better to have
     * the admin retry the whole approval than end up with a vendor record
     * marked ACTIVE while the underlying user still lacks ROLE_VENDOR.
     */
    public Vendor approve(Long vendorId, Long adminUserId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor application not found"));

        if (vendor.getStatus() != Vendor.VendorStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING applications can be approved");
        }

        grantVendorRole(vendor.getUserId()); // throws if this fails — see method below

        vendor.setStatus(Vendor.VendorStatus.ACTIVE);
        vendor.setReviewedByAdminId(adminUserId);
        vendor.setReviewedAt(Instant.now());
        return vendorRepository.save(vendor);
    }

    public Vendor reject(Long vendorId, Long adminUserId, String reason) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor application not found"));

        if (vendor.getStatus() != Vendor.VendorStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING applications can be rejected");
        }

        vendor.setStatus(Vendor.VendorStatus.REJECTED);
        vendor.setRejectionReason(reason);
        vendor.setReviewedByAdminId(adminUserId);
        vendor.setReviewedAt(Instant.now());
        return vendorRepository.save(vendor);
    }

    /**
     * Calls auth-service's internal role-grant endpoint directly (service-
     * to-service, bypassing the Gateway — see application.yml's
     * internal.api-key and auth-service's InternalController).
     */
    private void grantVendorRole(Long userId) {
        loadBalancedWebClientBuilder.build()
                .patch()
                .uri("http://auth-service/api/internal/users/{userId}/roles/grant?role=ROLE_VENDOR", userId)
                .header("X-Internal-Api-Key", internalApiKey)
                .retrieve()
                .toBodilessEntity()
                .block(); // synchronous on purpose — the approval must not proceed unless this succeeds
    }
}
