package com.loopers.application.order.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class OrderCreatedEventTest {

    @DisplayName("OrderCreatedEvent를 생성할 때,")
    @Nested
    class Create {

        @DisplayName("orderId, userId, totalAmount를 전달하면, 정상적으로 생성된다.")
        @Test
        void createsEvent_whenAllFieldsAreProvided() {
            // arrange
            Long orderId = 1L;
            Long userId = 100L;
            Long totalAmount = 50000L;

            // act
            OrderCreatedEvent event = new OrderCreatedEvent(orderId, userId, totalAmount);

            // assert
            assertAll(
                () -> assertThat(event.getOrderId()).isEqualTo(orderId),
                () -> assertThat(event.getUserId()).isEqualTo(userId),
                () -> assertThat(event.getTotalAmount()).isEqualTo(totalAmount)
            );
        }

        @DisplayName("couponId를 포함해서 생성할 수 있다.")
        @Test
        void createsEvent_withCouponId() {
            // arrange
            Long orderId = 1L;
            Long userId = 100L;
            Long totalAmount = 50000L;
            Long couponId = 10L;

            // act
            OrderCreatedEvent event = new OrderCreatedEvent(orderId, userId, totalAmount, couponId);

            // assert
            assertAll(
                () -> assertThat(event.getOrderId()).isEqualTo(orderId),
                () -> assertThat(event.getCouponId()).isEqualTo(couponId)
            );
        }

        @DisplayName("couponId 없이 생성하면, couponId는 null이다.")
        @Test
        void couponIdIsNull_whenNotProvided() {
            // act
            OrderCreatedEvent event = new OrderCreatedEvent(1L, 100L, 50000L);

            // assert
            assertThat(event.getCouponId()).isNull();
        }
    }
}
