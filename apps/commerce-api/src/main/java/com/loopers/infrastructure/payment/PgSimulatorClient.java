package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PgClient;
import com.loopers.domain.payment.PgPaymentRequest;
import com.loopers.domain.payment.PgPaymentResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PgSimulatorClient implements PgClient {

    private static final long AMOUNT_LIMIT = 100_000L;

    @Override
    public PgPaymentResponse requestPayment(PgPaymentRequest request) {
        if (request.amount() > AMOUNT_LIMIT) {
            return new PgPaymentResponse(false, null, "CARD_LIMIT_EXCEEDED");
        }
        String pgTransactionId = "pg-" + UUID.randomUUID().toString().substring(0, 8);
        return new PgPaymentResponse(true, pgTransactionId, "APPROVED");
    }

    @Override
    public PgPaymentResponse queryPaymentStatus(String pgTransactionId) {
        if (pgTransactionId != null && pgTransactionId.startsWith("pg-")) {
            return new PgPaymentResponse(true, pgTransactionId, "APPROVED");
        }
        return new PgPaymentResponse(false, pgTransactionId, "NOT_FOUND");
    }

    @Override
    public PgPaymentResponse cancelPayment(String pgTransactionId) {
        return new PgPaymentResponse(true, pgTransactionId, "CANCELLED");
    }
}
