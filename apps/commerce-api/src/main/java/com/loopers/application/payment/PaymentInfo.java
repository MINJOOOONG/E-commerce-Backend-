package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentStatus;

public record PaymentInfo(
    Long paymentId,
    Long orderId,
    Long userId,
    Long amount,
    PaymentMethod method,
    PaymentStatus status,
    String pgTransactionId
) {

    public static PaymentInfo from(Payment payment) {
        return new PaymentInfo(
            payment.getId(),
            payment.getOrderId(),
            payment.getUserId(),
            payment.getAmount(),
            payment.getMethod(),
            payment.getStatus(),
            payment.getPgTransactionId()
        );
    }
}
