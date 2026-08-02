package com.smartcommerce.user.controller;

import com.smartcommerce.user.dto.RejectVendorRequest;
import com.smartcommerce.user.dto.VendorApplicationRequest;
import com.smartcommerce.user.dto.VendorResponse;
import com.smartcommerce.user.entity.Vendor;
import com.smartcommerce.user.security.CurrentUser;
import com.smartcommerce.user.security.CurrentUserResolver;
import com.smartcommerce.user.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;
    private final CurrentUserResolver currentUserResolver;

    /** Any logged-in user can apply to become a vendor. Starts as PENDING. */
    @PostMapping
    public ResponseEntity<VendorResponse> apply(@Valid @RequestBody VendorApplicationRequest request,
                                                 HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        Vendor vendor = vendorService.apply(user.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(VendorResponse.from(vendor));
    }

    /** Admin-only: view all pending applications waiting for review. */
    @GetMapping("/pending")
    public ResponseEntity<List<VendorResponse>> listPending(HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        requireAdmin(user);

        List<VendorResponse> pending = vendorService.listPending().stream()
                .map(VendorResponse::from)
                .toList();
        return ResponseEntity.ok(pending);
    }

    /** Admin-only: approve a pending application (grants ROLE_VENDOR via auth-service). */
    @PatchMapping("/{vendorId}/approve")
    public ResponseEntity<VendorResponse> approve(@PathVariable Long vendorId, HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        requireAdmin(user);

        Vendor approved = vendorService.approve(vendorId, user.getUserId());
        return ResponseEntity.ok(VendorResponse.from(approved));
    }

    /** Admin-only: reject a pending application with a reason. */
    @PatchMapping("/{vendorId}/reject")
    public ResponseEntity<VendorResponse> reject(@PathVariable Long vendorId,
                                                  @Valid @RequestBody RejectVendorRequest request,
                                                  HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        requireAdmin(user);

        Vendor rejected = vendorService.reject(vendorId, user.getUserId(), request.getReason());
        return ResponseEntity.ok(VendorResponse.from(rejected));
    }

    private void requireAdmin(CurrentUser user) {
        if (!user.isAdmin()) {
            throw new SecurityException("Admin role required for this action");
        }
    }
}
