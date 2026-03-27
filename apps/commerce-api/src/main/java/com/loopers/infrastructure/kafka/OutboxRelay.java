package com.loopers.infrastructure.kafka;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import com.loopers.domain.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void relay() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatus(OutboxStatus.INIT);

        for (OutboxEvent event : pendingEvents) {
            try {
                String topic = event.getEventType().getTopic();
                String key = event.getPartitionKey() != null
                    ? event.getPartitionKey()
                    : event.getId().toString();

                kafkaTemplate.send(topic, key, event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[OutboxRelay] Kafka 발행 실패 - outboxId={}", event.getId(), ex);
                        }
                    });

                event.markSent();
                outboxEventRepository.save(event);

                log.info("[OutboxRelay] Kafka 발행 완료 - outboxId={}, topic={}",
                        event.getId(), topic);
            } catch (Exception e) {
                log.error("[OutboxRelay] Kafka 발행 중 예외 - outboxId={}", event.getId(), e);
            }
        }
    }
}
