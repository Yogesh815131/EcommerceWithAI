package com.smartcommerce.product.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartcommerce.product.dto.ProductRequest;
import com.smartcommerce.product.dto.ProductResponse;
import com.smartcommerce.product.entity.Product;
import com.smartcommerce.product.security.CurrentUser;
import com.smartcommerce.product.security.CurrentUserResolver;
import com.smartcommerce.product.service.ProductService;
import com.smartcommerce.product.service.StockAlertService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final CurrentUserResolver currentUserResolver;
    private final StockAlertService stockAlertService;

    // ---- Public browsing (no auth required — see Gateway RouteValidator) ----

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> browse(@RequestParam(name = "categoryId", required = false) Long categoryId,
                                                          @RequestParam(name = "search", required = false) String search,
                                                          Pageable pageable) {
        Page<ProductResponse> results = productService.browse(categoryId, search, pageable)
                .map(ProductResponse::from);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable("id") Long id) {
    	log.info("product Id is : {}", id);
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
    public ResponseEntity<ProductResponse> update(@PathVariable("id") Long id,
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
    
    @PostMapping("/{id}/stock-alerts")
    public ResponseEntity<Void> subscribeToStockAlert(@PathVariable Long id, HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest); // reuses your existing pattern — 401 if no X-User-Id
        stockAlertService.subscribe(user.getUserId(), id);
        return ResponseEntity.accepted().build();
    }
}
