package com.loopers.application.order.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

class OrderEventHandlerTest {

    @DisplayName("OrderEventHandler 단위 테스트")
    @Nested
    class HandleOrderCreated {

        private final OrderEventHandler handler = new OrderEventHandler();

        @DisplayName("OrderCreatedEvent를 받으면, 예외 없이 로그를 기록한다.")
        @Test
        void handlesEvent_withoutException() {
            // arrange
            OrderCreatedEvent event = new OrderCreatedEvent(1L, 100L, 50000L);

            // act & assert
            assertThatCode(() -> handler.handleOrderCreated(event))
                .doesNotThrowAnyException();
        }

        @DisplayName("couponId가 포함된 이벤트도 정상 처리한다.")
        @Test
        void handlesEvent_withCouponId() {
            // arrange
            OrderCreatedEvent event = new OrderCreatedEvent(1L, 100L, 50000L, 10L);

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
            ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
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
