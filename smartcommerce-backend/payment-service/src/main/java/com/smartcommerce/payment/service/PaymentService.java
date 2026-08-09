package com.smartcommerce.payment.service;

import com.smartcommerce.payment.entity.Payment;
import com.smartcommerce.payment.repository.PaymentRepository;
import com.smartcommerce.payment.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderLookupService orderLookupService;
    private final StripeChargeService stripeChargeService;

    /**
     * Charges for an order. On failure, automatically cancels the order
     * (restoring stock via order-service's existing cancel logic) so a
     * created-but-never-paid order doesn't linger — this is the
     * compensating step that makes "payment failed" functionally
     * equivalent to "order was never created," even though Order Service
     * already created it at checkout time (see the design note from when
     * this service was built for the full rationale).
     */
    public Payment charge(CurrentUser user, Long orderId, String paymentMethodId) {
        // Idempotency guard: if a payment already exists for this order
        // (e.g. a retried request), don't charge twice — return what happened before.
        var existing = paymentRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                    "A payment for this order was already " + existing.get().getStatus().name().toLowerCase());
        }

        OrderLookupService.OrderSnapshot order =
                orderLookupService.getOrder(user.getUserId(), user.getRoles(), orderId);

        if (!"PROCESSING".equals(order.status())) {
            throw new IllegalArgumentException(
                    "This order is " + order.status() + " and cannot be paid for");
        }

        StripeChargeService.ChargeResult result =
                stripeChargeService.charge(order.total(), paymentMethodId, orderId);

        if (result.success()) {
            Payment payment = Payment.builder()
                    .orderId(orderId)
                    .userId(user.getUserId())
                    .provider("STRIPE")
                    .providerChargeId(result.providerChargeId())
                    .amount(order.total())
                    .status(Payment.PaymentStatus.SUCCEEDED)
                    .build();
            return paymentRepository.save(payment);
        }

        // Payment failed — record it, then compensate by cancelling the order.
        Payment failedPayment = Payment.builder()
                .orderId(orderId)
                .userId(user.getUserId())
                .provider("STRIPE")
                .providerChargeId(result.providerChargeId() != null ? result.providerChargeId() : "failed-" + orderId)
                .amount(order.total())
                .status(Payment.PaymentStatus.FAILED)
                .failureReason(result.failureReason())
                .build();
        paymentRepository.save(failedPayment);

        orderLookupService.cancelOrder(user.getUserId(), user.getRoles(), orderId);

        throw new IllegalArgumentException(
                "Your payment was declined: " + result.failureReason()
                        + ". Your order has been cancelled and no charge was made.");
    }

    public Payment getByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No payment found for this order"));
    }
}
