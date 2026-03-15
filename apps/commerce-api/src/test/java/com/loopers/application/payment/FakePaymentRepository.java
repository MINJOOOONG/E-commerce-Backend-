package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class FakePaymentRepository implements PaymentRepository {

    private final Map<Long, Payment> store = new HashMap<>();
    private long sequence = 1L;

    @Override
    public Payment save(Payment payment) {
        store.put(payment.getId(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Payment> findByOrderId(Long orderId) {
        return store.values().stream()
            .filter(p -> p.getOrderId().equals(orderId))
            .findFirst();
    }

    @Override
    public Optional<Payment> findByPgTransactionId(String pgTransactionId) {
        return store.values().stream()
            .filter(p -> pgTransactionId.equals(p.getPgTransactionId()))
            .findFirst();
    }

    @Override
    public List<Payment> findAllByStatusAndCreatedAtBefore(PaymentStatus status, ZonedDateTime threshold) {
        return store.values().stream()
            .filter(p -> p.getStatus() == status)
            .toList();
    }
}
