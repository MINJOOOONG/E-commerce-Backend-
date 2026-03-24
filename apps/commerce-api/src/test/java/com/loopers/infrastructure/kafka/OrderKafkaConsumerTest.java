package com.loopers.infrastructure.kafka;

import com.loopers.domain.idempotency.EventHandled;
import com.loopers.domain.idempotency.EventHandledRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderKafkaConsumerTest {

    @Mock
    private EventHandledRepository eventHandledRepository;

    @InjectMocks
    private OrderKafkaConsumer consumer;

    @DisplayName("이벤트 수신 시,")
    @Nested
    class Consume {

        @DisplayName("처음 받은 이벤트면 처리하고 event_handled에 기록한다.")
        @Test
        void processesNewEvent() {
            // arrange
            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "order-events", 0, 0, "1", "{\"orderId\":1}");
            when(eventHandledRepository.existsByEventId("1")).thenReturn(false);
            when(eventHandledRepository.save(any(EventHandled.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // act
            consumer.consume(record);

            // assert
            ArgumentCaptor<EventHandled> captor = ArgumentCaptor.forClass(EventHandled.class);
            verify(eventHandledRepository).save(captor.capture());
            assertThat(captor.getValue().getEventId()).isEqualTo("1");
        }

        @DisplayName("이미 처리된 이벤트면 스킵한다.")
        @Test
        void skipsDuplicateEvent() {
            // arrange
            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "order-events", 0, 0, "1", "{\"orderId\":1}");
            when(eventHandledRepository.existsByEventId("1")).thenReturn(true);

            // act
            consumer.consume(record);

            // assert
            verify(eventHandledRepository, never()).save(any());
        }
    }
}
