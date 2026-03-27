package com.loopers.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.idempotency.EventHandled;
import com.loopers.domain.idempotency.EventHandledRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class CouponKafkaConsumer {

    private final EventHandledRepository eventHandledRepository;
    private final CouponService couponService;
    private final ObjectMapper objectMapper;

    @Transactional
    @KafkaListener(
        topics = "coupon-issue-requests",
        groupId = "coupon-issue-consumer"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String eventId = record.key();
        String payload = record.value();

        if (eventHandledRepository.existsByEventId(eventId)) {
            log.info("[CouponKafkaConsumer] 중복 이벤트 스킵 - eventId={}", eventId);
            acknowledgment.acknowledge();
            return;
        }

        log.info("[CouponKafkaConsumer] 이벤트 수신 - eventId={}, payload={}", eventId, payload);

        try {
            JsonNode node = objectMapper.readTree(payload);
            String requestId = node.get("requestId").asText();
            Long userId = node.get("userId").asLong();
            Long couponTemplateId = node.get("couponTemplateId").asLong();

            couponService.issueWithLimit(requestId, userId, couponTemplateId);
        } catch (JsonProcessingException e) {
            log.error("[CouponKafkaConsumer] payload 파싱 실패 - eventId={}", eventId, e);
            throw new RuntimeException("payload 파싱 실패", e);
        }

        eventHandledRepository.save(new EventHandled(eventId));
        acknowledgment.acknowledge();
        log.info("[CouponKafkaConsumer] 이벤트 처리 완료 - eventId={}", eventId);
    }
}
