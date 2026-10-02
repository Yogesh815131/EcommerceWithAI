package com.smartcommerce.product.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartcommerce.product.entity.Product;
import com.smartcommerce.product.entity.StockAlert;
import com.smartcommerce.product.repository.ProductRepository;
import com.smartcommerce.product.repository.StockAlertRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockAlertService {

    private final StockAlertRepository stockAlertRepository;
    private final ProductRepository productRepository;

    public void subscribe(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (product.getStockQuantity() > 0) {
            throw new IllegalArgumentException("Product is already in stock");
        }

        if (stockAlertRepository.existsByUserIdAndProductId(userId, productId)) {
            return; // already subscribed — treat as success, not an error
        }

        stockAlertRepository.save(StockAlert.builder()
                .userId(userId)
                .productId(productId)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /** Call this whenever a product's stock moves from 0 to positive. */
    public void notifySubscribersIfRestocked(Long productId, int previousQuantity, int newQuantity) {
        if (previousQuantity > 0 || newQuantity <= 0) return; // not a 0 -> positive transition

        List<StockAlert> pending = stockAlertRepository.findByProductIdAndNotifiedAtIsNull(productId);
        if (pending.isEmpty()) return;

        // TODO: replace with a real call once notification-service exists —
        // e.g. publish a "product.restocked" Kafka event carrying productId
        // and the list of userIds, and let notification-service resolve each
        // user's email (via auth-service) and send it. For now, this just
        // marks the alerts as handled so the same user isn't queued twice.
        log.info("STUB: would notify {} user(s) that product {} is back in stock",
                pending.size(), productId);

        pending.forEach(alert -> alert.setNotifiedAt(LocalDateTime.now()));
        stockAlertRepository.saveAll(pending);
    }
}
