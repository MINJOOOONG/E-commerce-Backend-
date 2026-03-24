package com.loopers.infrastructure.kafka;

import com.loopers.domain.idempotency.EventHandled;
import com.loopers.domain.idempotency.EventHandledRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderKafkaConsumer {

    private final EventHandledRepository eventHandledRepository;

    @Transactional
    @KafkaListener(
        topics = OutboxRelay.ORDER_EVENTS_TOPIC,
        groupId = "order-event-consumer"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String eventId = record.key();
        String payload = record.value();

        // 멱등 처리: 이미 처리된 이벤트는 스킵
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.info("[OrderKafkaConsumer] 중복 이벤트 스킵 - eventId={}", eventId);
            return;
        }

        log.info("[OrderKafkaConsumer] 이벤트 수신 - eventId={}, payload={}", eventId, payload);

        // TODO: 실제 후속 처리 로직 (알림 발송, 메트릭 집계, 외부 시스템 연동 등)

        // 처리 완료 기록
        eventHandledRepository.save(new EventHandled(eventId));
        log.info("[OrderKafkaConsumer] 이벤트 처리 완료 - eventId={}", eventId);
    }
}
