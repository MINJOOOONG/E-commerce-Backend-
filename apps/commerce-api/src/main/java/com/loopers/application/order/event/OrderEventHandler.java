package com.loopers.application.order.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.outbox.EventType;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderEventHandler {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Async("orderEventExecutor")
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[OrderCreatedEvent] 주문 생성 완료 - orderId={}, userId={}, totalAmount={}, couponId={}",
                event.getOrderId(),
                event.getUserId(),
                event.getTotalAmount(),
                event.getCouponId()
        );

        String payload = serializeEvent(event);
        OutboxEvent outboxEvent = new OutboxEvent(EventType.ORDER_CREATED, payload);
        outboxEventRepository.save(outboxEvent);

        log.info("[Outbox] 이벤트 저장 완료 - outboxId={}, eventType={}",
                outboxEvent.getId(), outboxEvent.getEventType());
    }

    private String serializeEvent(OrderCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }
}
