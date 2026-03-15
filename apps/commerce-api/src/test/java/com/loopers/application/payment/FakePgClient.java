package com.loopers.application.payment;

import com.loopers.domain.payment.PgClient;
import com.loopers.domain.payment.PgPaymentRequest;
import com.loopers.domain.payment.PgPaymentResponse;

class FakePgClient implements PgClient {

    private boolean shouldFail = false;
    private boolean shouldThrow = false;

    void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    void setShouldThrow(boolean shouldThrow) {
        this.shouldThrow = shouldThrow;
    }

    @Override
    public PgPaymentResponse requestPayment(PgPaymentRequest request) {
        if (shouldThrow) {
            throw new RuntimeException("PG 연결 오류");
        }
        if (shouldFail) {
            return new PgPaymentResponse(false, null, "CARD_LIMIT_EXCEEDED");
        }
        return new PgPaymentResponse(true, "pg-test-001", "APPROVED");
    }

    @Override
    public PgPaymentResponse queryPaymentStatus(String pgTransactionId) {
        return new PgPaymentResponse(true, pgTransactionId, "APPROVED");
    }

    @Override
    public PgPaymentResponse cancelPayment(String pgTransactionId) {
        return new PgPaymentResponse(true, pgTransactionId, "CANCELLED");
    }
}
