package com.smartcommerce.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectVendorRequest {

    @NotBlank
    private String reason;
}
