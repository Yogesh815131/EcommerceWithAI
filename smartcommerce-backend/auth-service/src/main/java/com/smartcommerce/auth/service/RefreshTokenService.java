package com.smartcommerce.auth.service;

import com.smartcommerce.auth.entity.RefreshToken;
import com.smartcommerce.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    /**
     * Creates a new opaque refresh token. We store only a SHA-256 hash of it
     * in the database — never the raw value. SHA-256 (not bcrypt) is
     * deliberate here: the raw token is a 64-byte cryptographically random
     * value, not a human password, so it already has far more entropy than
     * bcrypt's slow hashing is meant to defend against. A fast hash also
     * lets us look the token up directly by its hash at refresh time.
     */
    public String createRefreshToken(Long userId, String deviceInfo) {
        String rawToken = generateRawToken();

        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .tokenHash(sha256Hex(rawToken))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS))
                .revoked(false)
                .deviceInfo(deviceInfo)
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Validates a raw refresh token presented by the client. If valid,
     * ROTATES it: revokes the old row and issues a brand new token, so a
     * refresh token can only ever be used once (protects against replay if
     * a token is ever stolen).
     *
     * @throws IllegalArgumentException if the token is missing, unknown, expired, or already revoked
     */
    public RotatedToken validateAndRotate(String rawToken) {
        String hash = sha256Hex(rawToken);

        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (existing.isRevoked()) {
            throw new IllegalArgumentException("This refresh token has already been used or revoked");
        }
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token has expired, please log in again");
        }

        // Rotate: revoke the old one, issue a fresh one
        existing.setRevoked(true);
        existing.setRevokedAt(Instant.now());
        refreshTokenRepository.save(existing);

        String newRawToken = createRefreshToken(existing.getUserId(), existing.getDeviceInfo());
        return new RotatedToken(existing.getUserId(), newRawToken);
    }

    /** Revokes every active refresh token for a user — used by "logout all devices." */
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    /** Revokes a single refresh token — used by a normal "logout." */
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(sha256Hex(rawToken)).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record RotatedToken(Long userId, String newRawToken) {}
}
