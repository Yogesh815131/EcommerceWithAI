package com.smartcommerce.payment.service;

import com.smartcommerce.payment.entity.Payment;
import com.smartcommerce.payment.repository.PaymentRepository;
import com.smartcommerce.payment.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderLookupService orderLookupService;

    @Mock
    private StripeChargeService stripeChargeService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, orderLookupService, stripeChargeService);
    }

    private CurrentUser customer() {
        return new CurrentUser(42L, List.of("ROLE_CUSTOMER"));
    }

    @Test
    void charge_rejectsDuplicatePaymentAttempt() {
        Payment existing = Payment.builder().orderId(1L).status(Payment.PaymentStatus.SUCCEEDED).build();
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> paymentService.charge(customer(), 1L, "pm_card_visa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already succeeded");

        verifyNoInteractions(orderLookupService, stripeChargeService);
    }

    @Test
    void charge_rejectsNonProcessingOrder() {
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(orderLookupService.getOrder(42L, customer().getRoles(), 1L))
                .thenReturn(new OrderLookupService.OrderSnapshot(1L, "CANCELLED", new BigDecimal("50.00")));

        assertThatThrownBy(() -> paymentService.charge(customer(), 1L, "pm_card_visa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CANCELLED");

        verifyNoInteractions(stripeChargeService);
    }

    @Test
    void charge_succeedsAndSavesPayment() {
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(orderLookupService.getOrder(42L, customer().getRoles(), 1L))
                .thenReturn(new OrderLookupService.OrderSnapshot(1L, "PROCESSING", new BigDecimal("50.00")));
        when(stripeChargeService.charge(new BigDecimal("50.00"), "pm_card_visa", 1L))
                .thenReturn(new StripeChargeService.ChargeResult(true, "pi_123", null));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.charge(customer(), 1L, "pm_card_visa");

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.SUCCEEDED);
        assertThat(result.getProviderChargeId()).isEqualTo("pi_123");
        verify(orderLookupService, never()).cancelOrder(any(), any(), any());
    }

    @Test
    void charge_declineCancelsOrderAndThrows() {
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(orderLookupService.getOrder(42L, customer().getRoles(), 1L))
                .thenReturn(new OrderLookupService.OrderSnapshot(1L, "PROCESSING", new BigDecimal("50.00")));
        when(stripeChargeService.charge(new BigDecimal("50.00"), "pm_card_declined", 1L))
                .thenReturn(new StripeChargeService.ChargeResult(false, null, "Your card was declined."));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> paymentService.charge(customer(), 1L, "pm_card_declined"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("declined");

        // Verify the compensating cancel actually happened
        verify(orderLookupService).cancelOrder(42L, customer().getRoles(), 1L);

        // And that a FAILED payment record was still saved for audit purposes
        verify(paymentRepository).save(argThat(p -> p.getStatus() == Payment.PaymentStatus.FAILED));
    }
}
