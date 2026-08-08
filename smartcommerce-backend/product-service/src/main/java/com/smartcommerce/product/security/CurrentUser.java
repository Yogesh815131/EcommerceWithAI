package com.smartcommerce.product.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CurrentUser {
    private final Long userId;
    private final List<String> roles;

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isVendor() {
        return hasRole("ROLE_VENDOR");
    }

    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }
}
