package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Payment createPayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    public Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제를 찾을 수 없습니다"));
    }

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "해당 주문의 결제를 찾을 수 없습니다"));
    }

    public Payment getPaymentByPgTransactionId(String pgTransactionId) {
        return paymentRepository.findByPgTransactionId(pgTransactionId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "해당 PG 트랜잭션의 결제를 찾을 수 없습니다"));
    }

    public List<Payment> getPendingPaymentsBefore(ZonedDateTime threshold) {
        return paymentRepository.findAllByStatusAndCreatedAtBefore(PaymentStatus.PENDING, threshold);
    }
}
