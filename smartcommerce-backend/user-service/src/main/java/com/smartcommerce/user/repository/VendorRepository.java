package com.smartcommerce.user.repository;

import com.smartcommerce.user.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByUserId(Long userId);
    List<Vendor> findByStatus(Vendor.VendorStatus status);
    boolean existsByUserId(Long userId);
}
