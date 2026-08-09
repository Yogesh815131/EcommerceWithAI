package com.smartcommerce.order.repository;

import com.smartcommerce.order.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    Page<OrderItem> findByVendorId(Long vendorId, Pageable pageable);
    Optional<OrderItem> findByIdAndVendorId(Long id, Long vendorId);
}
