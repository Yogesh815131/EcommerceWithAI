package com.smartcommerce.product.controller;

import com.smartcommerce.product.dto.ProductRequest;
import com.smartcommerce.product.dto.ProductResponse;
import com.smartcommerce.product.entity.Product;
import com.smartcommerce.product.security.CurrentUser;
import com.smartcommerce.product.security.CurrentUserResolver;
import com.smartcommerce.product.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CurrentUserResolver currentUserResolver;

    // ---- Public browsing (no auth required — see Gateway RouteValidator) ----

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> browse(@RequestParam(required = false) Long categoryId,
                                                          @RequestParam(required = false) String search,
                                                          Pageable pageable) {
        Page<ProductResponse> results = productService.browse(categoryId, search, pageable)
                .map(ProductResponse::from);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ProductResponse.from(productService.getById(id)));
    }

    // ---- Vendor management (requires ROLE_VENDOR + ownership) ----

    @GetMapping("/vendor/me")
    public ResponseEntity<Page<ProductResponse>> myProducts(Pageable pageable, HttpServletRequest httpRequest) {
        CurrentUser user = requireVendor(httpRequest);
        Page<ProductResponse> results = productService.listMyProducts(user, pageable).map(ProductResponse::from);
        return ResponseEntity.ok(results);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request,
                                                   HttpServletRequest httpRequest) {
        CurrentUser user = requireVendor(httpRequest);
        Product created = productService.create(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ProductRequest request,
                                                   HttpServletRequest httpRequest) {
        CurrentUser user = requireVendor(httpRequest);
        Product updated = productService.update(user, id, request);
        return ResponseEntity.ok(ProductResponse.from(updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductResponse> setStatus(@PathVariable Long id,
                                                       @RequestParam Product.ProductStatus status,
                                                       HttpServletRequest httpRequest) {
        CurrentUser user = requireVendor(httpRequest);
        Product updated = productService.setStatus(user, id, status);
        return ResponseEntity.ok(ProductResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        CurrentUser user = requireVendor(httpRequest);
        productService.delete(user, id);
        return ResponseEntity.noContent().build();
    }

    private CurrentUser requireVendor(HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        if (!user.isVendor() && !user.isAdmin()) {
            throw new SecurityException("Vendor role required for this action");
        }
        return user;
    }
}
