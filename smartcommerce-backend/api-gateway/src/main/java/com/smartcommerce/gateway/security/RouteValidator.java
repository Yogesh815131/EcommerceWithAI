package com.smartcommerce.gateway.security;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/eureka"
    );

    public final Predicate<ServerHttpRequest> isSecured = request ->
            OPEN_ENDPOINTS.stream()
                    .noneMatch(openPath -> request.getURI().getPath().startsWith(openPath));
}