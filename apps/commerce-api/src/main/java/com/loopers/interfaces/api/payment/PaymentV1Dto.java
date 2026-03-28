package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentInfo;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public class PaymentV1Dto {

    public record PaymentRequest(
        @NotNull Long orderId,
        @NotNull PaymentMethod method
    ) {}

    public record CallbackRequest(
        @NotNull String pgTransactionId,
        @NotNull Boolean success,
        @NotNull String responseCode
    ) {}

    public record PaymentResponse(
        Long paymentId,
        Long orderId,
        Long amount,
        PaymentMethod method,
        PaymentStatus status,
        String pgTransactionId
    ) {
        public static PaymentResponse from(PaymentInfo info) {
            return new PaymentResponse(
                info.paymentId(),
                info.orderId(),
                info.amount(),
                info.method(),
                info.status(),
                info.pgTransactionId()
            );
        }
    }
}
