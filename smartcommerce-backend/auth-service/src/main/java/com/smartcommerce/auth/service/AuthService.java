package com.smartcommerce.auth.service;

import com.smartcommerce.auth.dto.AuthResponse;
import com.smartcommerce.auth.dto.LoginRequest;
import com.smartcommerce.auth.dto.RegisterRequest;
import com.smartcommerce.auth.entity.User;
import com.smartcommerce.auth.repository.UserRepository;
import com.smartcommerce.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        User saved = userRepository.save(user);
        return buildAuthResponse(saved, null);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (user.getPasswordHash() == null) {
            throw new IllegalArgumentException(
                    "This email is registered via Google. Please use 'Sign in with Google'.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return buildAuthResponse(user, null);
    }

    /**
     * Exchanges a valid refresh token for a brand new access token + a
     * brand new refresh token (rotation — see RefreshTokenService).
     */
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedToken rotated = refreshTokenService.validateAndRotate(rawRefreshToken);

        User user = userRepository.findById(rotated.userId())
                .orElseThrow(() -> new IllegalArgumentException("User no longer exists"));

        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRoles());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(rotated.newRawToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles())
                .build();
    }

    /** Logs out of just the current device/session. */
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    /** Logs out of every device — revokes all active refresh tokens for the user. */
    public void logoutAllDevices(Long userId) {
        refreshTokenService.revokeAllForUser(userId);
    }

    private AuthResponse buildAuthResponse(User user, String deviceInfo) {
        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRoles());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId(), deviceInfo);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles())
                .build();
    }

    // TODO: implement OAuth2 (Google) success handler that finds-or-creates
    // a User with authProvider = "GOOGLE" and issues tokens the same way via buildAuthResponse().
}
