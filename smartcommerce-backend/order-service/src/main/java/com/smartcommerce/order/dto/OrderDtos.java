package com.smartcommerce.order.dto;

import com.smartcommerce.order.entity.Order;
import com.smartcommerce.order.entity.OrderItem;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderDtos {

    @Data
    public static class CreateOrderRequest {
        @NotNull
        private Long shippingAddressId;
    }

    @Data
    public static class UpdateItemStatusRequest {
        @NotNull
        private Order.OrderStatus status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private Long vendorId;
        private String productName;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal lineTotal;
        private Order.OrderStatus itemStatus;

        public static OrderItemResponse from(OrderItem item) {
            return OrderItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProductId())
                    .vendorId(item.getVendorId())
                    .productName(item.getProductNameSnapshot())
                    .unitPrice(item.getUnitPriceSnapshot())
                    .quantity(item.getQuantity())
                    .lineTotal(item.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .itemStatus(item.getItemStatus())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderResponse {
        private Long id;
        private Order.OrderStatus status;
        private BigDecimal subtotal;
        private BigDecimal shippingCost;
        private BigDecimal tax;
        private BigDecimal total;
        private Long shippingAddressId;
        private Instant placedAt;
        private List<OrderItemResponse> items;

        public static OrderResponse from(Order order) {
            return OrderResponse.builder()
                    .id(order.getId())
                    .status(order.getStatus())
                    .subtotal(order.getSubtotal())
                    .shippingCost(order.getShippingCost())
                    .tax(order.getTax())
                    .total(order.getTotal())
                    .shippingAddressId(order.getShippingAddressId())
                    .placedAt(order.getPlacedAt())
                    .items(order.getItems().stream().map(OrderItemResponse::from).toList())
                    .build();
        }
    }
}
