package com.smartcommerce.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcommerce.product.entity.StockAlert;

public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    List<StockAlert> findByProductIdAndNotifiedAtIsNull(Long productId);
}
