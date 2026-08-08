package com.smartcommerce.cart.service;

import com.smartcommerce.cart.dto.CartDtos.CartResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<Object, Object, Object> hashOperations;

    @Mock
    private ProductLookupService productLookupService;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        // opsForHash() is a generic method — this cast is the standard way
        // to stub it with Mockito; RedisTemplate erases the hash key/value
        // types at runtime anyway, so this matches what CartService actually gets.
        when(redisTemplate.opsForHash()).thenReturn((HashOperations) hashOperations);
        cartService = new CartService(redisTemplate, productLookupService);
    }

    private ProductLookupService.ProductInfo sampleProduct(int stock) {
        return new ProductLookupService.ProductInfo(
                1L, "Running Shoes", "http://example.com/shoe.jpg", new BigDecimal("59.99"), stock);
    }

    @Test
    void addItem_succeedsWhenWithinStock() {
        when(productLookupService.getProduct(1L)).thenReturn(sampleProduct(10));
        when(hashOperations.get("cart:42", "1")).thenReturn(null); // nothing in cart yet
        when(hashOperations.entries("cart:42")).thenReturn(Map.of("1", "2"));

        CartResponse result = cartService.addItem(42L, 1L, 2);

        verify(hashOperations).put("cart:42", "1", "2");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getSubtotal()).isEqualByComparingTo("119.98"); // 59.99 * 2
    }

    @Test
    void addItem_incrementsExistingQuantity() {
        when(productLookupService.getProduct(1L)).thenReturn(sampleProduct(10));
        when(hashOperations.get("cart:42", "1")).thenReturn("3"); // already 3 in cart
        when(hashOperations.entries("cart:42")).thenReturn(Map.of("1", "5"));

        cartService.addItem(42L, 1L, 2); // adding 2 more -> should become 5

        verify(hashOperations).put("cart:42", "1", "5");
    }

    @Test
    void addItem_rejectsWhenExceedingStock() {
        when(productLookupService.getProduct(1L)).thenReturn(sampleProduct(5));
        when(hashOperations.get("cart:42", "1")).thenReturn(null);

        assertThatThrownBy(() -> cartService.addItem(42L, 1L, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only 5 left in stock");

        verify(hashOperations, never()).put(any(), any(), any());
    }

    @Test
    void updateQuantity_rejectsWhenItemNotInCart() {
        when(hashOperations.hasKey("cart:42", "1")).thenReturn(false);

        assertThatThrownBy(() -> cartService.updateQuantity(42L, 1L, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not in your cart");

        verifyNoInteractions(productLookupService);
    }

    @Test
    void updateQuantity_rejectsWhenExceedingStock() {
        when(hashOperations.hasKey("cart:42", "1")).thenReturn(true);
        when(productLookupService.getProduct(1L)).thenReturn(sampleProduct(3));

        assertThatThrownBy(() -> cartService.updateQuantity(42L, 1L, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only 3 left in stock");
    }

    @Test
    void updateQuantity_succeedsWithinStock() {
        when(hashOperations.hasKey("cart:42", "1")).thenReturn(true);
        when(productLookupService.getProduct(1L)).thenReturn(sampleProduct(10));
        when(hashOperations.entries("cart:42")).thenReturn(Map.of("1", "4"));

        cartService.updateQuantity(42L, 1L, 4);

        verify(hashOperations).put("cart:42", "1", "4");
    }

    @Test
    void removeItem_deletesTheHashField() {
        when(hashOperations.entries("cart:42")).thenReturn(Map.of());

        cartService.removeItem(42L, 1L);

        verify(hashOperations).delete("cart:42", "1");
    }

    @Test
    void clearCart_deletesTheWholeKey() {
        cartService.clearCart(42L);

        verify(redisTemplate).delete("cart:42");
    }

    @Test
    void getCart_calculatesSubtotalAcrossMultipleItems() {
        Map<Object, Object> hashContents = new LinkedHashMap<>();
        hashContents.put("1", "2"); // productId 1, qty 2
        hashContents.put("2", "1"); // productId 2, qty 1

        when(hashOperations.entries("cart:42")).thenReturn(hashContents);
        when(productLookupService.getProduct(1L)).thenReturn(sampleProduct(10));
        when(productLookupService.getProduct(2L)).thenReturn(
                new ProductLookupService.ProductInfo(2L, "Socks", "http://example.com/socks.jpg",
                        new BigDecimal("9.99"), 50));

        CartResponse result = cartService.getCart(42L);

        assertThat(result.getItems()).hasSize(2);
        // (59.99 * 2) + (9.99 * 1) = 129.97
        assertThat(result.getSubtotal()).isEqualByComparingTo("129.97");
    }

    @Test
    void getCart_returnsEmptyCartWhenNothingStored() {
        when(hashOperations.entries("cart:42")).thenReturn(Map.of());

        CartResponse result = cartService.getCart(42L);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getSubtotal()).isEqualByComparingTo("0");
    }
}
