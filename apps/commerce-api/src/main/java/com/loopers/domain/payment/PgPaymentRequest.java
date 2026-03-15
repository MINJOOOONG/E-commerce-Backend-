package com.loopers.domain.payment;

public record PgPaymentRequest(Long orderId, Long amount, PaymentMethod method) {
}
