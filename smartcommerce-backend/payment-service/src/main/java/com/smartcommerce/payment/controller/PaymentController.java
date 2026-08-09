package com.smartcommerce.payment.controller;

import com.smartcommerce.payment.dto.PaymentDtos.CreatePaymentRequest;
import com.smartcommerce.payment.dto.PaymentDtos.PaymentResponse;
import com.smartcommerce.payment.entity.Payment;
import com.smartcommerce.payment.security.CurrentUser;
import com.smartcommerce.payment.security.CurrentUserResolver;
import com.smartcommerce.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping
    public ResponseEntity<PaymentResponse> charge(@Valid @RequestBody CreatePaymentRequest request,
                                                    HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        Payment payment = paymentService.charge(user, request.getOrderId(), request.getPaymentMethodId());
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(payment));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getByOrderId(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        Payment payment = paymentService.getByOrderId(orderId);
        if (!payment.getUserId().equals(user.getUserId()) && !user.getRoles().contains("ROLE_ADMIN")) {
            throw new SecurityException("You do not have access to this payment");
        }
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }
}
