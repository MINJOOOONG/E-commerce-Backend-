package com.loopers.application.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.outbox.EventType;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventHandlerTest {

    @DisplayName("OrderEventHandler 단위 테스트")
    @Nested
    class HandleOrderCreated {

        @Mock
        private OutboxEventRepository outboxEventRepository;

        @Spy
        private ObjectMapper objectMapper = new ObjectMapper();

        @InjectMocks
        private OrderEventHandler handler;

        @DisplayName("OrderCreatedEvent를 받으면, Outbox에 이벤트를 저장한다.")
        @Test
        void savesOutboxEvent_whenEventReceived() {
            // arrange
            OrderCreatedEvent event = new OrderCreatedEvent(1L, 100L, 50000L);
            when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // act
            handler.handleOrderCreated(event);

            // assert
            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventRepository).save(captor.capture());

            OutboxEvent saved = captor.getValue();
            assertThat(saved.getEventType()).isEqualTo(EventType.ORDER_CREATED);
            assertThat(saved.getPayload()).contains("\"orderId\":1");
            assertThat(saved.getPayload()).contains("\"totalAmount\":50000");
        }

        @DisplayName("couponId가 포함된 이벤트도 Outbox에 저장한다.")
        @Test
        void savesOutboxEvent_withCouponId() {
            // arrange
            OrderCreatedEvent event = new OrderCreatedEvent(1L, 100L, 50000L, 10L);
            when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // act
            handler.handleOrderCreated(event);

            // assert
            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventRepository).save(captor.capture());

            assertThat(captor.getValue().getPayload()).contains("\"couponId\":10");
        }

        @DisplayName("이벤트 처리 시 예외가 발생하지 않는다.")
        @Test
        void handlesEvent_withoutException() {
            // arrange
            OrderCreatedEvent event = new OrderCreatedEvent(1L, 100L, 50000L);
            when(outboxEventRepository.save(any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // act & assert
            assertThatCode(() -> handler.handleOrderCreated(event))
                .doesNotThrowAnyException();
        }
    }

    @DisplayName("이벤트 발행 검증")
    @Nested
    class EventPublishing {

        @DisplayName("ApplicationEventPublisher로 OrderCreatedEvent를 발행할 수 있다.")
        @Test
        void publishesOrderCreatedEvent() {
            // arrange
            ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
            OrderCreatedEvent event = new OrderCreatedEvent(1L, 100L, 50000L);

            // act
            publisher.publishEvent(event);

            // assert
            verify(publisher).publishEvent(event);
        }

        @DisplayName("발행된 이벤트의 필드 값이 보존된다.")
        @Test
        void eventFieldsArePreserved() {
            // arrange
            Long orderId = 42L;
            Long userId = 100L;
            Long totalAmount = 99000L;
            Long couponId = 5L;

            // act
            OrderCreatedEvent event = new OrderCreatedEvent(orderId, userId, totalAmount, couponId);

            // assert
            assertThat(event.getOrderId()).isEqualTo(orderId);
            assertThat(event.getUserId()).isEqualTo(userId);
            assertThat(event.getTotalAmount()).isEqualTo(totalAmount);
            assertThat(event.getCouponId()).isEqualTo(couponId);
        }
    }
}
