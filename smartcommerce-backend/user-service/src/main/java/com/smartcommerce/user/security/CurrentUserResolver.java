package com.smartcommerce.user.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Reads the X-User-Id / X-User-Roles headers that AuthenticationFilter
 * (in api-gateway) already stamped onto the request after verifying the
 * JWT. This service trusts those headers because, in this architecture,
 * every request is required to pass through the Gateway first.
 *
 * NOTE: for production hardening, downstream services are also expected
 * to double check these headers only come from the Gateway (e.g. network
 * policy restricting direct access to service ports) — this is the
 * "defense in depth" point flagged in the TRD's security requirements.
 */
@Component
public class CurrentUserResolver {

    public CurrentUser resolve(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        String rolesHeader = request.getHeader("X-User-Roles");

        if (userIdHeader == null) {
            throw new IllegalStateException(
                    "Missing X-User-Id header — this request did not come through the API Gateway");
        }

        Long userId = Long.valueOf(userIdHeader);
        List<String> roles = (rolesHeader == null || rolesHeader.isBlank())
                ? Collections.emptyList()
                : Arrays.asList(rolesHeader.split(","));

        return new CurrentUser(userId, roles);
    }
}
