package com.smartcommerce.order.controller;

import com.smartcommerce.order.dto.OrderDtos.*;
import com.smartcommerce.order.entity.Order;
import com.smartcommerce.order.entity.OrderItem;
import com.smartcommerce.order.security.CurrentUser;
import com.smartcommerce.order.security.CurrentUserResolver;
import com.smartcommerce.order.service.OrderService;
import com.smartcommerce.order.service.VendorLookupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserResolver currentUserResolver;
    private final VendorLookupService vendorLookupService;

    @PostMapping
    public ResponseEntity<OrderResponse> checkout(@Valid @RequestBody CreateOrderRequest request,
                                                    HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        Order order = orderService.checkout(user, request.getShippingAddressId());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> myOrders(
            @RequestParam(required = false) Order.OrderStatus status,
            @PageableDefault(size = 20, sort = "placedAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        Page<OrderResponse> orders = orderService.listMyOrders(user.getUserId(), status, pageable)
                .map(OrderResponse::from);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id, HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        Order order = orderService.getOrder(user, id);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id, HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        Order cancelled = orderService.cancel(user, id);
        return ResponseEntity.ok(OrderResponse.from(cancelled));
    }

    // ---- Vendor Order Management ----

    @GetMapping("/vendor/me")
    public ResponseEntity<Page<OrderItemResponse>> myVendorOrderItems(
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest httpRequest) {
        CurrentUser user = requireVendor(httpRequest);
        Long vendorId = vendorLookupService.resolveActiveVendorId(user.getUserId(), user.getRoles());
        Page<OrderItemResponse> items = orderService.listItemsForVendor(vendorId, pageable)
                .map(OrderItemResponse::from);
        return ResponseEntity.ok(items);
    }

    @PatchMapping("/items/{orderItemId}/status")
    public ResponseEntity<OrderItemResponse> updateItemStatus(@PathVariable Long orderItemId,
                                                                @Valid @RequestBody UpdateItemStatusRequest request,
                                                                HttpServletRequest httpRequest) {
        CurrentUser user = requireVendor(httpRequest);
        Long vendorId = vendorLookupService.resolveActiveVendorId(user.getUserId(), user.getRoles());
        OrderItem updated = orderService.updateItemStatus(vendorId, orderItemId, request.getStatus());
        return ResponseEntity.ok(OrderItemResponse.from(updated));
    }

    private CurrentUser requireVendor(HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        if (!user.isVendor() && !user.isAdmin()) {
            throw new SecurityException("Vendor role required for this action");
        }
        return user;
    }
}
