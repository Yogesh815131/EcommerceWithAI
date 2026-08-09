package com.smartcommerce.order.service;

import com.smartcommerce.order.entity.Order;
import com.smartcommerce.order.entity.OrderItem;
import com.smartcommerce.order.repository.OrderItemRepository;
import com.smartcommerce.order.repository.OrderRepository;
import com.smartcommerce.order.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartLookupService cartLookupService;

    @Mock
    private ProductStockService productStockService;

    @Mock
    private AddressLookupService addressLookupService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository, cartLookupService,
                productStockService, addressLookupService);
    }

    private CurrentUser customer() {
        return new CurrentUser(42L, List.of("ROLE_CUSTOMER"));
    }

    @Test
    void checkout_rejectsEmptyCart() {
        when(cartLookupService.getCart(42L)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.checkout(customer(), 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cart is empty");

        verifyNoInteractions(addressLookupService, productStockService);
    }

    @Test
    void checkout_succeedsAndClearsCart() {
        CartLookupService.CartItem cartItem = new CartLookupService.CartItem(100L, 2, new BigDecimal("10.00"));
        when(cartLookupService.getCart(42L)).thenReturn(List.of(cartItem));
        when(addressLookupService.fetchAddressSnapshotJson(42L, 1L)).thenReturn("{\"city\":\"Springfield\"}");
        when(productStockService.getProduct(100L))
                .thenReturn(new ProductStockService.ProductSnapshot(100L, 500L, "Widget", new BigDecimal("10.00")));
        when(productStockService.decrementStock(100L, 2)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.checkout(customer(), 1L);

        assertThat(result.getSubtotal()).isEqualByComparingTo("20.00");
        assertThat(result.getTotal()).isEqualByComparingTo("20.00");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.PROCESSING);

        verify(cartLookupService).clearCart(42L);
    }

    @Test
    void checkout_rollsBackAlreadyDecrementedItemsWhenLaterItemFails() {
        CartLookupService.CartItem item1 = new CartLookupService.CartItem(100L, 2, new BigDecimal("10.00"));
        CartLookupService.CartItem item2 = new CartLookupService.CartItem(200L, 5, new BigDecimal("5.00"));

        when(cartLookupService.getCart(42L)).thenReturn(List.of(item1, item2));
        when(addressLookupService.fetchAddressSnapshotJson(42L, 1L)).thenReturn("{}");

        when(productStockService.getProduct(100L))
                .thenReturn(new ProductStockService.ProductSnapshot(100L, 500L, "Widget", new BigDecimal("10.00")));
        when(productStockService.getProduct(200L))
                .thenReturn(new ProductStockService.ProductSnapshot(200L, 600L, "Gadget", new BigDecimal("5.00")));

        when(productStockService.decrementStock(100L, 2)).thenReturn(true);  // first item succeeds
        when(productStockService.decrementStock(200L, 5)).thenReturn(false); // second item fails (out of stock)

        assertThatThrownBy(() -> orderService.checkout(customer(), 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Gadget");

        // The first item's stock decrement must be undone since the whole order failed
        verify(productStockService).restoreStock(100L, 2);
        verify(orderRepository, never()).save(any());
        verify(cartLookupService, never()).clearCart(any());
    }

    @Test
    void cancel_rejectsWhenAlreadyShipped() {
        Order order = Order.builder().id(1L).userId(42L).status(Order.OrderStatus.SHIPPED)
                .items(List.of()).build();
        when(orderRepository.findByIdAndUserId(1L, 42L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(customer(), 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shipped");
    }

    @Test
    void cancel_restoresStockForEveryItem() {
        OrderItem item1 = OrderItem.builder().productId(100L).quantity(2).itemStatus(Order.OrderStatus.PROCESSING).build();
        OrderItem item2 = OrderItem.builder().productId(200L).quantity(3).itemStatus(Order.OrderStatus.PROCESSING).build();
        Order order = Order.builder().id(1L).userId(42L).status(Order.OrderStatus.PROCESSING)
                .items(List.of(item1, item2)).build();

        when(orderRepository.findByIdAndUserId(1L, 42L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.cancel(customer(), 1L);

        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        verify(productStockService).restoreStock(100L, 2);
        verify(productStockService).restoreStock(200L, 3);
    }

    @Test
    void updateItemStatus_rejectsMovingShippedBackToProcessing() {
        OrderItem item = OrderItem.builder().id(9L).itemStatus(Order.OrderStatus.SHIPPED).build();
        when(orderItemRepository.findByIdAndVendorId(9L, 500L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> orderService.updateItemStatus(500L, 9L, Order.OrderStatus.PROCESSING))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shipped");
    }

    @Test
    void updateItemStatus_rejectsChangingCancelledItem() {
        OrderItem item = OrderItem.builder().id(9L).itemStatus(Order.OrderStatus.CANCELLED).build();
        when(orderItemRepository.findByIdAndVendorId(9L, 500L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> orderService.updateItemStatus(500L, 9L, Order.OrderStatus.SHIPPED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void updateItemStatus_succeedsForValidTransition() {
        OrderItem item = OrderItem.builder().id(9L).itemStatus(Order.OrderStatus.PROCESSING).build();
        when(orderItemRepository.findByIdAndVendorId(9L, 500L)).thenReturn(Optional.of(item));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderItem result = orderService.updateItemStatus(500L, 9L, Order.OrderStatus.SHIPPED);

        assertThat(result.getItemStatus()).isEqualTo(Order.OrderStatus.SHIPPED);
    }
}
