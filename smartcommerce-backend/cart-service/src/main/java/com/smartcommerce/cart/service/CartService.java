package com.smartcommerce.cart.service;

import com.smartcommerce.cart.dto.CartDtos.CartItemResponse;
import com.smartcommerce.cart.dto.CartDtos.CartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final String CART_KEY_PREFIX = "cart:";
    // Per the Backend Schema doc: no TTL while items exist is fine, but we
    // refresh a 30-day inactivity expiry on every write so an abandoned
    // cart doesn't sit in Redis forever.
    private static final Duration CART_TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;
    private final ProductLookupService productLookupService;

    public CartResponse getCart(Long userId) {
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(cartKey(userId));

        List<CartItemResponse> items = raw.entrySet().stream()
                .map(entry -> buildItemResponse(
                        Long.valueOf((String) entry.getKey()),
                        Integer.valueOf((String) entry.getValue())))
                .collect(Collectors.toList());

        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder().items(items).subtotal(subtotal).build();
    }

    /** Adding an existing item INCREASES its quantity by the requested amount. */
    public CartResponse addItem(Long userId, Long productId, int quantityToAdd) {
        ProductLookupService.ProductInfo product = productLookupService.getProduct(productId);

        int currentQty = getCurrentQuantity(userId, productId);
        int newQty = currentQty + quantityToAdd;

        validateStock(product, newQty);

        redisTemplate.opsForHash().put(cartKey(userId), productId.toString(), String.valueOf(newQty));
        redisTemplate.expire(cartKey(userId), CART_TTL);

        return getCart(userId);
    }

    /** Update sets the ABSOLUTE quantity (used by the Cart page's quantity stepper). */
    public CartResponse updateQuantity(Long userId, Long productId, int newQuantity) {
        if (!redisTemplate.opsForHash().hasKey(cartKey(userId), productId.toString())) {
            throw new IllegalArgumentException("This item is not in your cart");
        }

        ProductLookupService.ProductInfo product = productLookupService.getProduct(productId);
        validateStock(product, newQuantity);

        redisTemplate.opsForHash().put(cartKey(userId), productId.toString(), String.valueOf(newQuantity));
        redisTemplate.expire(cartKey(userId), CART_TTL);

        return getCart(userId);
    }

    public CartResponse removeItem(Long userId, Long productId) {
        redisTemplate.opsForHash().delete(cartKey(userId), productId.toString());
        return getCart(userId);
    }

    public void clearCart(Long userId) {
        redisTemplate.delete(cartKey(userId));
    }

    private void validateStock(ProductLookupService.ProductInfo product, int requestedQuantity) {
        if (requestedQuantity > product.stockQuantity()) {
            throw new IllegalArgumentException(
                    "Only " + product.stockQuantity() + " left in stock for \"" + product.name() + "\"");
        }
    }

    private int getCurrentQuantity(Long userId, Long productId) {
        Object existing = redisTemplate.opsForHash().get(cartKey(userId), productId.toString());
        return existing == null ? 0 : Integer.parseInt((String) existing);
    }

    private CartItemResponse buildItemResponse(Long productId, Integer quantity) {
        // NOTE: fetches product details fresh on every read so price/stock
        // shown always reflects the current catalog state, not a stale
        // snapshot from when the item was added.
        ProductLookupService.ProductInfo product = productLookupService.getProduct(productId);

        return CartItemResponse.builder()
                .productId(product.id())
                .name(product.name())
                .imageUrl(product.imageUrl())
                .price(product.price())
                .quantity(quantity)
                .subtotal(product.price().multiply(BigDecimal.valueOf(quantity)))
                .availableStock(product.stockQuantity())
                .build();
    }

    private String cartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }
}
