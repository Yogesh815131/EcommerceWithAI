package com.smartcommerce.user.dto;

import com.smartcommerce.user.entity.Vendor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorResponse {
    private Long id;
    private Long userId;
    private String businessName;
    private String businessDescription;
    private Vendor.VendorStatus status;
    private String rejectionReason;
    private Instant createdAt;

    public static VendorResponse from(Vendor v) {
        return VendorResponse.builder()
                .id(v.getId())
                .userId(v.getUserId())
                .businessName(v.getBusinessName())
                .businessDescription(v.getBusinessDescription())
                .status(v.getStatus())
                .rejectionReason(v.getRejectionReason())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
