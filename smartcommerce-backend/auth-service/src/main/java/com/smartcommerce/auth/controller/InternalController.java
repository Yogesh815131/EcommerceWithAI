package com.smartcommerce.auth.controller;

import com.smartcommerce.auth.entity.User;
import com.smartcommerce.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalController {

    private final UserRepository userRepository;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @PatchMapping("/{userId}/roles/grant")
    public ResponseEntity<Void> grantRole(@PathVariable("userId") Long userId,
            							  @RequestParam("role") String role,
                                          @RequestHeader("X-Internal-Api-Key") String providedKey) {
        if (!internalApiKey.equals(providedKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<String> roles = new HashSet<>(user.getRoles());
        roles.add(role);
        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.noContent().build();
    }
}