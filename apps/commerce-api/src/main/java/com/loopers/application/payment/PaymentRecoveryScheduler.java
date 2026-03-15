package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.payment.PgClient;
import com.loopers.domain.payment.PgPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentRecoveryScheduler {

    private static final long PENDING_THRESHOLD_MINUTES = 1L;

    private final PaymentService paymentService;
    private final PaymentTransactionService transactionService;
    private final PgClient pgClient;

    @Scheduled(fixedDelay = 60_000)
    public void recover() {
        ZonedDateTime threshold = ZonedDateTime.now().minusMinutes(PENDING_THRESHOLD_MINUTES);
        List<Payment> pendingPayments = paymentService.getPendingPaymentsBefore(threshold);

        for (Payment payment : pendingPayments) {
            recoverPayment(payment);
        }
    }

    private void recoverPayment(Payment payment) {
        if (payment.getPgTransactionId() == null) {
            return;
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }

        try {
            PgPaymentResponse response = pgClient.queryPaymentStatus(payment.getPgTransactionId());

            if (response.success()) {
                transactionService.approvePayment(
                    payment.getId(), payment.getPgTransactionId(), response.responseCode()
                );
                log.info("결제 복구 완료 (승인). paymentId={}, pgTxId={}",
                    payment.getId(), payment.getPgTransactionId());
            } else {
                transactionService.failPaymentWithCompensation(
                    payment.getId(), response.responseCode()
                );
                log.info("결제 복구 완료 (실패). paymentId={}, pgTxId={}",
                    payment.getId(), payment.getPgTransactionId());
            }
        } catch (Exception e) {
            log.warn("결제 복구 중 오류 발생. paymentId={}, pgTxId={}, error={}",
                payment.getId(), payment.getPgTransactionId(), e.getMessage());
        }
    }
}
