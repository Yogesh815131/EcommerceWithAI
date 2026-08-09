package com.smartcommerce.payment.dto;

import com.smartcommerce.payment.entity.Payment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentDtos {

    @Data
    public static class CreatePaymentRequest {
        @NotNull
        private Long orderId;

        /** A Stripe PaymentMethod id (e.g. "pm_card_visa") — created client-side via Stripe.js/Elements, never a raw card number. */
        @NotBlank
        private String paymentMethodId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResponse {
        private Long id;
        private Long orderId;
        private BigDecimal amount;
        private Payment.PaymentStatus status;
        private String failureReason;
        private Instant createdAt;

        public static PaymentResponse from(Payment p) {
            return PaymentResponse.builder()
                    .id(p.getId())
                    .orderId(p.getOrderId())
                    .amount(p.getAmount())
                    .status(p.getStatus())
                    .failureReason(p.getFailureReason())
                    .createdAt(p.getCreatedAt())
                    .build();
        }
    }
}
