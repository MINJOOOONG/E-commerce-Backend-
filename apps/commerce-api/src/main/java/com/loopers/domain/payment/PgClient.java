package com.loopers.domain.payment;

public interface PgClient {

    PgPaymentResponse requestPayment(PgPaymentRequest request);

    PgPaymentResponse queryPaymentStatus(String pgTransactionId);

    PgPaymentResponse cancelPayment(String pgTransactionId);
}
