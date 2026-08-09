package com.smartcommerce.payment.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sets the Stripe SDK's global API key on startup. The key itself lives
 * ONLY in an environment variable (STRIPE_SECRET_KEY) — never in this
 * file, never in application.yml with a real value, never committed to
 * source control. Use a Stripe TEST mode key (starts with sk_test_)
 * while developing.
 */
@Component
public class StripeConfig {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }
}
