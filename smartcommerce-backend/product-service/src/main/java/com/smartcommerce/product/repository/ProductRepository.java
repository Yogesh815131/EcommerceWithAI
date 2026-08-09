package com.smartcommerce.product.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartcommerce.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByStatus(Product.ProductStatus status, Pageable pageable);

    Page<Product> findByCategoryIdAndStatus(Long categoryId, Product.ProductStatus status, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndStatus(String name, Product.ProductStatus status, Pageable pageable);

    Page<Product> findByVendorId(Long vendorId, Pageable pageable);
    
    @Modifying
    @Query(
            "UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity, p.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE p.id = :productId AND p.stockQuantity >= :quantity")
    int decrementStock(@Param("productId") Long productId,
                        @Param("quantity") Integer quantity);

    @Modifying
    @Query(
            "UPDATE Product p SET p.stockQuantity = p.stockQuantity + :quantity, p.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE p.id = :productId")
    void restoreStock(@Param("productId") Long productId,
                       @Param("quantity") Integer quantity);
}
