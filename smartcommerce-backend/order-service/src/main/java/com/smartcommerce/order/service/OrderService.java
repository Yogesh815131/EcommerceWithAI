package com.smartcommerce.order.service;

import com.smartcommerce.order.entity.Order;
import com.smartcommerce.order.entity.OrderItem;
import com.smartcommerce.order.repository.OrderItemRepository;
import com.smartcommerce.order.repository.OrderRepository;
import com.smartcommerce.order.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartLookupService cartLookupService;
    private final ProductStockService productStockService;
    private final AddressLookupService addressLookupService;

    /**
     * The core checkout flow:
     *   1. Read the real cart from cart-service (never trust a client-submitted item list)
     *   2. Snapshot the shipping address
     *   3. For each item, atomically decrement stock in product-service
     *      - if any item fails (out of stock), roll back every item
     *        already decremented, then fail the whole order
     *   4. Save the order with all items
     *   5. Clear the cart
     *
     * NOTE: this is manual compensation across services, not a real
     * distributed transaction / saga framework — acceptable for this
     * scale, but worth knowing as a simplification if this ever needs to
     * handle much higher concurrency or add more steps.
     */
    public Order checkout(CurrentUser user, Long shippingAddressId) {
        List<CartLookupService.CartItem> cartItems = cartLookupService.getCart(user.getUserId());
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Your cart is empty");
        }

        String addressSnapshotJson = addressLookupService.fetchAddressSnapshotJson(user.getUserId(), shippingAddressId);

        List<OrderItem> successfullyDecremented = new ArrayList<>();
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartLookupService.CartItem cartItem : cartItems) {
            ProductStockService.ProductSnapshot product = productStockService.getProduct(cartItem.productId());

            boolean decremented = productStockService.decrementStock(cartItem.productId(), cartItem.quantity());
            if (!decremented) {
                // Roll back everything already decremented in this same checkout attempt
                for (OrderItem alreadyDecremented : successfullyDecremented) {
                    productStockService.restoreStock(alreadyDecremented.getProductId(), alreadyDecremented.getQuantity());
                }
                throw new IllegalArgumentException(
                        "\"" + product.name() + "\" no longer has enough stock. Please update your cart and try again.");
            }

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.id())
                    .vendorId(product.vendorId())
                    .productNameSnapshot(product.name())
                    .unitPriceSnapshot(product.price())
                    .quantity(cartItem.quantity())
                    .itemStatus(Order.OrderStatus.PROCESSING)
                    .build();

            orderItems.add(orderItem);
            successfullyDecremented.add(orderItem);
            subtotal = subtotal.add(product.price().multiply(BigDecimal.valueOf(cartItem.quantity())));
        }

        // Shipping/tax kept simple (flat/zero) for MVP — real calculation is out of scope here.
        BigDecimal shippingCost = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal total = subtotal.add(shippingCost).add(tax);

        Order order = Order.builder()
                .userId(user.getUserId())
                .status(Order.OrderStatus.PROCESSING)
                .subtotal(subtotal)
                .shippingCost(shippingCost)
                .tax(tax)
                .total(total)
                .shippingAddressId(shippingAddressId)
                .shippingAddressSnapshot(addressSnapshotJson)
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.setItems(orderItems);

        Order saved = orderRepository.save(order);

        cartLookupService.clearCart(user.getUserId());

        return saved;
    }

    public Page<Order> listMyOrders(Long userId, Order.OrderStatus statusFilter, Pageable pageable) {
        return statusFilter == null
                ? orderRepository.findByUserId(userId, pageable)
                : orderRepository.findByUserIdAndStatus(userId, statusFilter, pageable);
    }

    public Order getOrder(CurrentUser user, Long orderId) {
        if (user.isAdmin()) {
            return orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        }
        return orderRepository.findByIdAndUserId(orderId, user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    /** Customer (or Admin) cancels an order — only allowed before it ships, and restores stock for every item. */
    public Order cancel(CurrentUser user, Long orderId) {
        Order order = getOrder(user, orderId);

        if (order.getStatus() != Order.OrderStatus.PROCESSING) {
            throw new IllegalArgumentException(
                    "This order has already " + order.getStatus().name().toLowerCase()
                            + " and can no longer be cancelled");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.getItems().forEach(item -> {
            item.setItemStatus(Order.OrderStatus.CANCELLED);
            productStockService.restoreStock(item.getProductId(), item.getQuantity());
        });

        return orderRepository.save(order);
    }

    // ---- Vendor Order Management ----

    public Page<OrderItem> listItemsForVendor(Long vendorId, Pageable pageable) {
        return orderItemRepository.findByVendorId(vendorId, pageable);
    }

    /** Vendor updates the status of just their own line item within a (possibly multi-vendor) order. */
    public OrderItem updateItemStatus(Long vendorId, Long orderItemId, Order.OrderStatus newStatus) {
        OrderItem item = orderItemRepository.findByIdAndVendorId(orderItemId, vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Order item not found"));

        if (item.getItemStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("This item was cancelled and its status can no longer be changed");
        }
        if (item.getItemStatus() == Order.OrderStatus.SHIPPED && newStatus == Order.OrderStatus.PROCESSING) {
            throw new IllegalArgumentException("An item can't be moved back to PROCESSING once shipped");
        }

        item.setItemStatus(newStatus);
        return orderItemRepository.save(item);
    }
}
