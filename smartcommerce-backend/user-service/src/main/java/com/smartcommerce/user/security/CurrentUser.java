package com.smartcommerce.user.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * This service never sees a raw JWT — the Gateway already validated it and
 * forwards the result as plain headers (X-User-Id, X-User-Roles). This
 * class is just a convenient, typed wrapper around those two headers for
 * a single request. See CurrentUserFilter for how it gets populated.
 */
@Getter
@AllArgsConstructor
public class CurrentUser {
    private final Long userId;
    private final List<String> roles;

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }
}
