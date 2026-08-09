package com.smartcommerce.product.controller;

import com.smartcommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * NOT for browser/frontend use — only order-service calls this, directly
 * (bypassing the Gateway), protected by a shared secret instead of a user
 * JWT, same pattern as auth-service's InternalController.
 */
@RestController
@RequestMapping("/api/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductRepository productRepository;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @PatchMapping("/{productId}/decrement-stock")
    @Transactional
    public ResponseEntity<Void> decrementStock(@PathVariable Long productId,
                                                @RequestParam Integer quantity,
                                                @RequestHeader("X-Internal-Api-Key") String providedKey) {
        if (!internalApiKey.equals(providedKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        int rowsUpdated = productRepository.decrementStock(productId, quantity);
        if (rowsUpdated == 0) {
            // Either the product doesn't exist, or there isn't enough stock —
            // either way, order-service should treat this as "can't fulfill."
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{productId}/restore-stock")
    @Transactional
    public ResponseEntity<Void> restoreStock(@PathVariable Long productId,
                                              @RequestParam Integer quantity,
                                              @RequestHeader("X-Internal-Api-Key") String providedKey) {
        if (!internalApiKey.equals(providedKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        productRepository.restoreStock(productId, quantity);
        return ResponseEntity.noContent().build();
    }
}