package com.smartcommerce.cart.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class CurrentUserResolver {

    /** Every cart endpoint requires a real, logged-in user — no anonymous carts in v1. */
    public CurrentUser resolve(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null) {
            throw new IllegalStateException(
                    "Missing X-User-Id header — this request did not come through the API Gateway");
        }

        String rolesHeader = request.getHeader("X-User-Roles");
        Long userId = Long.valueOf(userIdHeader);
        List<String> roles = (rolesHeader == null || rolesHeader.isBlank())
                ? Collections.emptyList()
                : Arrays.asList(rolesHeader.split(","));

        return new CurrentUser(userId, roles);
    }
}
