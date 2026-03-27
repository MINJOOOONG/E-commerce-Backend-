package com.loopers.infrastructure.kafka;

import com.loopers.domain.outbox.EventType;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import com.loopers.domain.outbox.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @InjectMocks
    private OutboxRelay outboxRelay;

    @DisplayName("relay 실행 시,")
    @Nested
    class Relay {

        @DisplayName("ORDER_CREATED 이벤트는 order-events 토픽으로 발행한다.")
        @Test
        void sendsOrderEventToOrderTopic() {
            // arrange
            OutboxEvent event = new OutboxEvent(EventType.ORDER_CREATED, "{\"orderId\":1}");
            when(outboxEventRepository.findByStatus(OutboxStatus.INIT)).thenReturn(List.of(event));
            when(kafkaTemplate.send(any(), any(), any())).thenReturn(new CompletableFuture<>());
            when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // act
            outboxRelay.relay();

            // assert
            verify(kafkaTemplate).send(eq("order-events"), any(), eq("{\"orderId\":1}"));
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        }

        @DisplayName("COUPON_ISSUE_REQUESTED 이벤트는 coupon-issue-requests 토픽에 partitionKey를 key로 발행한다.")
        @Test
        void sendsCouponEventWithPartitionKey() {
            // arrange
            OutboxEvent event = new OutboxEvent(EventType.COUPON_ISSUE_REQUESTED, "{\"userId\":1}", "10");
            when(outboxEventRepository.findByStatus(OutboxStatus.INIT)).thenReturn(List.of(event));
            when(kafkaTemplate.send(any(), any(), any())).thenReturn(new CompletableFuture<>());
            when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // act
            outboxRelay.relay();

            // assert
            verify(kafkaTemplate).send(eq("coupon-issue-requests"), eq("10"), eq("{\"userId\":1}"));
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        }

        @DisplayName("INIT 상태 이벤트가 없으면 Kafka 발행을 하지 않는다.")
        @Test
        void doesNotSend_whenNoInitEvents() {
            // arrange
            when(outboxEventRepository.findByStatus(OutboxStatus.INIT)).thenReturn(List.of());

            // act
            outboxRelay.relay();

            // assert
            verify(kafkaTemplate, never()).send(any(), any(), any());
        }
    }
}
