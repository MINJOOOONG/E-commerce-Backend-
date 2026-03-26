package com.loopers.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.coupon.CouponService;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponKafkaConsumerTest {

    @Mock
    private EventHandledRepository eventHandledRepository;

    @Mock
    private CouponService couponService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CouponKafkaConsumer consumer;

    @DisplayName("쿠폰 발급 이벤트 수신 시,")
    @Nested
    class Consume {

        @DisplayName("처음 받은 이벤트면 CouponService.issueWithLimit을 호출한다.")
        @Test
        void processesNewEvent() {
            // arrange
            String payload = "{\"requestId\":\"req-1\",\"userId\":1,\"couponTemplateId\":10}";
            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "coupon-issue-requests", 0, 0, "req-1", payload);
            when(eventHandledRepository.existsByEventId("req-1")).thenReturn(false);
            when(eventHandledRepository.save(any(EventHandled.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // act
            consumer.consume(record);

            // assert
            verify(couponService).issueWithLimit("req-1", 1L, 10L);
            ArgumentCaptor<EventHandled> captor = ArgumentCaptor.forClass(EventHandled.class);
            verify(eventHandledRepository).save(captor.capture());
            assertThat(captor.getValue().getEventId()).isEqualTo("req-1");
        }

        @DisplayName("이미 처리된 이벤트면 스킵한다.")
        @Test
        void skipsDuplicateEvent() {
            // arrange
            String payload = "{\"requestId\":\"req-1\",\"userId\":1,\"couponTemplateId\":10}";
            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "coupon-issue-requests", 0, 0, "req-1", payload);
            when(eventHandledRepository.existsByEventId("req-1")).thenReturn(true);

            // act
            consumer.consume(record);

            // assert
            verify(couponService, never()).issueWithLimit(any(), any(), any());
        }
    }
}
