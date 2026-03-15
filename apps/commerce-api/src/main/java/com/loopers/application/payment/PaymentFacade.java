package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PgClient;
import com.loopers.domain.payment.PgPaymentRequest;
import com.loopers.domain.payment.PgPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentFacade {

    private final PaymentTransactionService transactionService;
    private final PgClient pgClient;

    public PaymentInfo requestPayment(Long orderId, PaymentMethod method) {
        Payment payment = transactionService.createPendingPayment(orderId, method);

        PgPaymentResponse pgResponse;
        try {
            pgResponse = pgClient.requestPayment(
                new PgPaymentRequest(orderId, payment.getAmount(), method)
            );
        } catch (Exception e) {
            log.warn("PG 결제 요청 중 오류 발생. paymentId={}, orderId={}, error={}",
                payment.getId(), orderId, e.getMessage());
            return PaymentInfo.from(payment);
        }

        if (!pgResponse.success()) {
            Payment failed = transactionService.failPaymentWithCompensation(
                payment.getId(), pgResponse.responseCode()
            );
            return PaymentInfo.from(failed);
        }

        return PaymentInfo.from(payment);
    }
}
