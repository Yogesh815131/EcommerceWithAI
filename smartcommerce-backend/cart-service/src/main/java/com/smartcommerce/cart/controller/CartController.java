package com.smartcommerce.cart.controller;

import com.smartcommerce.cart.dto.CartDtos.AddToCartRequest;
import com.smartcommerce.cart.dto.CartDtos.CartResponse;
import com.smartcommerce.cart.dto.CartDtos.UpdateQuantityRequest;
import com.smartcommerce.cart.security.CurrentUser;
import com.smartcommerce.cart.security.CurrentUserResolver;
import com.smartcommerce.cart.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        return ResponseEntity.ok(cartService.getCart(user.getUserId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddToCartRequest request,
                                                 HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        CartResponse cart = cartService.addItem(user.getUserId(), request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(cart);
    }

    @PatchMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateQuantity(@PathVariable Long productId,
                                                         @Valid @RequestBody UpdateQuantityRequest request,
                                                         HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        CartResponse cart = cartService.updateQuantity(user.getUserId(), productId, request.getQuantity());
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long productId, HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        CartResponse cart = cartService.removeItem(user.getUserId(), productId);
        return ResponseEntity.ok(cart);
    }

    /** Not in the App Flow doc as a user-facing button, but needed internally (e.g. by Order Service after checkout). */
    @DeleteMapping
    public ResponseEntity<Void> clearCart(HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        cartService.clearCart(user.getUserId());
        return ResponseEntity.noContent().build();
    }
}
