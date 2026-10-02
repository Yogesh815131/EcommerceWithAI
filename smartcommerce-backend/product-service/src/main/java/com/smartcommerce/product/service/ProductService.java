package com.smartcommerce.product.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.smartcommerce.product.dto.ProductRequest;
import com.smartcommerce.product.entity.Product;
import com.smartcommerce.product.repository.CategoryRepository;
import com.smartcommerce.product.repository.ProductRepository;
import com.smartcommerce.product.security.CurrentUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final VendorLookupService vendorLookupService;
    private final StockAlertService stockAlertService;

    // ---- Public browsing ----

    public Page<Product> browse(Long categoryId, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return productRepository.findByNameContainingIgnoreCaseAndStatus(
                    search, Product.ProductStatus.ACTIVE, pageable);
        }
        if (categoryId != null) {
            return productRepository.findByCategoryIdAndStatus(categoryId, Product.ProductStatus.ACTIVE, pageable);
        }
        return productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
    }

    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    // ---- Vendor management ----

    public Page<Product> listMyProducts(CurrentUser user, Pageable pageable) {
        Long vendorId = vendorLookupService.resolveActiveVendorId(user.getUserId(), user.getRoles());
        return productRepository.findByVendorId(vendorId, pageable);
    }

    public Product create(CurrentUser user, ProductRequest request) {
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new IllegalArgumentException("Category not found");
        }

        Long vendorId = vendorLookupService.resolveActiveVendorId(user.getUserId(), user.getRoles());

        Product product = Product.builder()
                .vendorId(vendorId)
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .imageUrl(request.getImageUrl())
                .status(Product.ProductStatus.ACTIVE)
                .build();

        return productRepository.save(product);
    }

    public Product update(CurrentUser user, Long productId, ProductRequest request) {
        Product product = getOwnedProduct(user, productId);

        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new IllegalArgumentException("Category not found");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategoryId(request.getCategoryId());
        product.setImageUrl(request.getImageUrl());

        return productRepository.save(product);
    }
    

    public Product setStatus(CurrentUser user, Long productId, Product.ProductStatus status) {
        Product product = getOwnedProduct(user, productId);
        product.setStatus(status);
        return productRepository.save(product);
    }

    public void delete(CurrentUser user, Long productId) {
        Product product = getOwnedProduct(user, productId);
        productRepository.delete(product);
    }

    /** Loads a product and verifies the calling vendor actually owns it (or is an Admin). */
    private Product getOwnedProduct(CurrentUser user, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (user.isAdmin()) {
            return product; // admins can manage any product
        }

        Long callerVendorId = vendorLookupService.resolveActiveVendorId(user.getUserId(), user.getRoles());
        if (!product.getVendorId().equals(callerVendorId)) {
            throw new SecurityException("You do not own this product");
        }

        return product;
    }
    
   
}
