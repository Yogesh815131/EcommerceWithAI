package com.smartcommerce.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Logical reference to Product Service's products.id. */
    @Column(nullable = false)
    private Long productId;

    /** Logical reference to User/Vendor Service's vendors.id — denormalized here for Vendor Order Management queries. */
    @Column(nullable = false)
    private Long vendorId;

    /** Snapshot at time of purchase — never updated if the product changes later. */
    @Column(nullable = false)
    private String productNameSnapshot;

    @Column(nullable = false)
    private BigDecimal unitPriceSnapshot;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Order.OrderStatus itemStatus = Order.OrderStatus.PROCESSING;
}
