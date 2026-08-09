package com.smartcommerce.payment.service;

import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class StripeChargeService {

    /**
     * Charges a payment method for the given amount, synchronously.
     * confirm(true) + automatic_payment_methods with redirects disabled
     * keeps this a single-step charge suitable for a server-side flow —
     * full 3D Secure / redirect-based payment methods are NOT handled
     * here (that would need a client-side confirmation step); this
     * covers standard card payments, which is enough for MVP.
     */
    public ChargeResult charge(BigDecimal amount, String paymentMethodId, Long orderId) {
        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .setPaymentMethod(paymentMethodId)
                    .setConfirm(true)
                    .setDescription("SmartCommerce AI order #" + orderId)
                    .putMetadata("orderId", String.valueOf(orderId))
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            if ("succeeded".equals(intent.getStatus())) {
                return ChargeResult.success(intent.getId());
            }

            // requires_action, requires_payment_method, etc. — treat anything
            // short of "succeeded" as a failure for this simplified MVP flow.
            return ChargeResult.failure(intent.getId(), "Payment could not be completed (status: " + intent.getStatus() + ")");

        } catch (CardException e) {
            // Card was declined — Stripe gives a human-readable reason
            return ChargeResult.failure(null, e.getMessage());
        } catch (StripeException e) {
            return ChargeResult.failure(null, "Payment processing failed: " + e.getMessage());
        }
    }

    public record ChargeResult(boolean success, String providerChargeId, String failureReason) {
        static ChargeResult success(String chargeId) {
            return new ChargeResult(true, chargeId, null);
        }
        static ChargeResult failure(String chargeId, String reason) {
            return new ChargeResult(false, chargeId, reason);
        }
    }
}
