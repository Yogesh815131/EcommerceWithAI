package com.smartcommerce.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VendorApplicationRequest {

    @NotBlank
    private String businessName;

    private String businessDescription;
}
