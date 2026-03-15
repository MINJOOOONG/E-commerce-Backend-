package com.loopers.domain.payment;

public record PgPaymentResponse(boolean success, String pgTransactionId, String responseCode) {
}
