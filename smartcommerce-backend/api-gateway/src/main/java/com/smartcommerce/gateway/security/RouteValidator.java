package com.smartcommerce.gateway.security;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class RouteValidator {

    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/eureka"
    );
    
    private static final List<String> PUBLIC_GET_PREFIXES = List.of(
            "/api/products",
            "/api/categories"
    );

    public final Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        boolean isAlwaysOpen = OPEN_ENDPOINTS.stream().anyMatch(path::startsWith);

        // /api/products/vendor/me must stay protected even though it starts
        // with /api/products — it's the vendor's OWN product list, not public browsing.
        boolean isPublicBrowsing = HttpMethod.GET.equals(method)
                && !path.startsWith("/api/products/vendor")
                && PUBLIC_GET_PREFIXES.stream().anyMatch(path::startsWith);

        return !(isAlwaysOpen || isPublicBrowsing);
    };

}