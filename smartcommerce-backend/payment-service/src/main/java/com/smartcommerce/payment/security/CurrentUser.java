package com.smartcommerce.payment.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CurrentUser {
    private final Long userId;
    private final List<String> roles;
}
